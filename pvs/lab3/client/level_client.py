#!/usr/bin/env python3
"""Клиент ЛР3, вариант 3 «Уровень» для стенда SDK-1.1M.

Запуск:   py -3 level_client.py COM3 [--log файл]
Нужен pyserial:  py -3 -m pip install pyserial

Окно показывает то же, что дисплей стенда: ускорения по трём осям и
указатель (линия горизонта или стрелка вниз) того типа, который сейчас
выбран на стенде. Кнопки окна отправляют на стенд команду сменить тип
указателя. Все строки, переданные по UART в обе стороны, пишутся в файл
журнала с меткой времени.

Протокол (подробно - fw/Core/Inc/proto.h), строки ASCII:
  ПК -> стенд:  PING | MODE H | MODE A
  стенд -> ПК:  PONG | OK MODE <m> | ERR ... | KEY <n>
                ACC <x> <y> <z> <m> <deg>   (mg; m = H|A; deg 0..359 или "-")
"""

import argparse
import datetime
import math
import queue
import sys
import threading
import time
import tkinter as tk
from tkinter import ttk

import serial

BAUD = 115200
PING_TIMEOUT_S = 1.0      # ответ на PING ждём столько, потом повторяем
ACK_TIMEOUT_S = 1.0       # подтверждение команды MODE
DATA_TIMEOUT_S = 0.5      # ACC приходят каждые 50 мс; дольше нет - связь потеряна
POLL_MS = 30              # период обработки принятых строк в окне

MODE_NAMES = {"H": "Горизонт", "A": "Стрелка"}

# Шкала указателя: те же пропорции, что на дисплее стенда (радиус 30 точек,
# стрелка до R-3, наконечник 8 точек под 25 градусов, горизонт до R-1)
DIAL_SIZE = 300
DIAL_R = 120
DIAL_SCALE = DIAL_R / 30.0
ARROW_HEAD_DEG = 25


def timestamp():
    return datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]


def format_g(mg):
    """mg -> '+0.98': так же, как на стенде (level_format_g)."""
    mag = abs(mg)
    centi = (mag + 5) // 10
    sign = "-" if (mg < 0 and centi != 0) else "+"
    return f"{sign}{centi // 100}.{centi % 100:02d}"


class Journal:
    """Файл журнала: каждая строка обмена с меткой времени и направлением."""

    def __init__(self, path):
        self._f = open(path, "a", encoding="utf-8")
        self._lock = threading.Lock()
        self.path = path

    def write(self, direction, text):
        with self._lock:
            self._f.write(f"{timestamp()} {direction} {text}\n")
            self._f.flush()

    def close(self):
        with self._lock:
            self._f.close()


class Link:
    """COM-порт: приём строк в отдельном потоке, передача из окна."""

    def __init__(self, port_name, journal):
        self._journal = journal
        self._port = serial.Serial(port_name, BAUD, timeout=0.05)
        self.lines = queue.Queue()
        self._stop = threading.Event()
        self._thread = threading.Thread(target=self._reader, daemon=True)
        self._thread.start()

    def _reader(self):
        buf = b""
        while not self._stop.is_set():
            try:
                chunk = self._port.read(256)
            except serial.SerialException as exc:
                self.lines.put(("!", f"ошибка порта: {exc}"))
                return
            buf += chunk
            while b"\n" in buf:
                raw, buf = buf.split(b"\n", 1)
                line = raw.rstrip(b"\r").decode("ascii", errors="backslashreplace")
                self._journal.write("RX", line)
                self.lines.put(("RX", line))
        if buf:
            self._journal.write("RX", buf.decode("ascii", errors="backslashreplace") + "  (без конца строки)")

    def send(self, line):
        self._journal.write("TX", line)
        self._port.write((line + "\n").encode("ascii"))

    def close(self):
        self._stop.set()
        self._thread.join(timeout=1.0)
        self._port.close()


class App:
    def __init__(self, root, link, port_name, journal_path):
        self.root = root
        self.link = link

        self.board_mode = None        # тип указателя, который сообщил стенд
        self.angle = None             # направление силы тяжести, None - не определено
        self.data_valid = False
        self.last_data = 0.0
        self.pong_received = False
        self.ping_sent_at = None
        self.pending_ack = None       # (ожидаемая строка, время отправки)

        root.title(f"Уровень - {port_name}")
        root.resizable(False, False)

        main = ttk.Frame(root, padding=10)
        main.grid()

        self.canvas = tk.Canvas(main, width=DIAL_SIZE, height=DIAL_SIZE,
                                background="#101820", highlightthickness=0)
        self.canvas.grid(row=0, column=0, rowspan=2, padx=(0, 12))

        info = ttk.Frame(main)
        info.grid(row=0, column=1, sticky="n")

        big = ("Consolas", 16)
        self.mode_var = tk.StringVar(value="Указатель на стенде: -")
        ttk.Label(info, textvariable=self.mode_var, font=("Segoe UI", 12, "bold")).grid(
            row=0, column=0, sticky="w", pady=(0, 8))

        self.axis_vars = []
        for i, name in enumerate("XYZ"):
            var = tk.StringVar(value=f"{name}:   ---")
            ttk.Label(info, textvariable=var, font=big).grid(row=1 + i, column=0, sticky="w")
            self.axis_vars.append(var)

        self.key_var = tk.StringVar(value="Клавиатура стенда: -")
        ttk.Label(info, textvariable=self.key_var).grid(row=4, column=0, sticky="w", pady=(8, 0))

        ctrl = ttk.LabelFrame(main, text="Задать тип указателя", padding=8)
        ctrl.grid(row=1, column=1, sticky="sew")
        ttk.Button(ctrl, text="Горизонт", command=lambda: self.send_mode("H")).grid(row=0, column=0, padx=4)
        ttk.Button(ctrl, text="Стрелка", command=lambda: self.send_mode("A")).grid(row=0, column=1, padx=4)

        self.status_var = tk.StringVar()
        ttk.Label(main, textvariable=self.status_var, foreground="#555").grid(
            row=2, column=0, columnspan=2, sticky="w", pady=(8, 0))
        self.event_var = tk.StringVar(value=f"Журнал: {journal_path}")
        ttk.Label(main, textvariable=self.event_var, foreground="#555").grid(
            row=3, column=0, columnspan=2, sticky="w")

        self.draw_dial()
        self.send_ping()
        root.after(POLL_MS, self.poll)

    # --- обмен ---

    def send_ping(self):
        self.link.send("PING")
        self.ping_sent_at = time.monotonic()

    def send_mode(self, mode):
        self.link.send(f"MODE {mode}")
        self.pending_ack = (f"OK MODE {mode}", time.monotonic())

    def handle_line(self, line):
        parts = line.split()
        if not parts:
            return
        kind = parts[0]

        if kind == "ACC" and len(parts) == 6:
            self.handle_acc(parts[1:])
        elif line == "PONG":
            if not self.pong_received:
                ms = (time.monotonic() - self.ping_sent_at) * 1000
                self.event_var.set(f"Стенд ответил на PING за {ms:.0f} мс")
            self.pong_received = True
        elif kind == "OK":
            if self.pending_ack and line == self.pending_ack[0]:
                self.pending_ack = None
            self.event_var.set(f"Стенд подтвердил: {line}")
        elif kind == "KEY" and len(parts) == 2:
            self.key_var.set(f"Клавиатура стенда: нажата кнопка {parts[1]}")
        elif kind == "ERR":
            self.event_var.set(f"Стенд сообщил об ошибке: {line}")
        else:
            self.event_var.set(f"Непонятная строка от стенда: {line!r}")

    def handle_acc(self, f):
        x, y, z, mode, deg = f
        valid = x != "-"
        try:
            values = [int(v) for v in (x, y, z)] if valid else None
            angle = None if deg == "-" else int(deg)
        except ValueError:
            values = angle = None
        if mode not in MODE_NAMES or (valid and values is None) or (angle is not None and not 0 <= angle < 360):
            # Строка испорчена (например, оборвана при сбросе стенда) - пропускаем
            self.event_var.set(f"Испорченная строка ACC: {' '.join(f)!r}")
            return

        self.board_mode = mode
        self.last_data = time.monotonic()
        self.data_valid = valid
        self.angle = angle
        for i, (var, name) in enumerate(zip(self.axis_vars, "XYZ")):
            var.set(f"{name}: {format_g(values[i])} g" if valid else f"{name}:   ---")

    def poll(self):
        while True:
            try:
                direction, line = self.link.lines.get_nowait()
            except queue.Empty:
                break
            if direction == "!":
                self.event_var.set(line)
            else:
                self.handle_line(line)

        now = time.monotonic()
        if not self.pong_received and now - self.ping_sent_at > PING_TIMEOUT_S:
            self.event_var.set("Стенд не ответил на PING, повторяю")
            self.send_ping()
        if self.pending_ack and now - self.pending_ack[1] > ACK_TIMEOUT_S:
            self.event_var.set(f"Нет подтверждения: ожидалось {self.pending_ack[0]}")
            self.pending_ack = None

        live = now - self.last_data <= DATA_TIMEOUT_S
        if not self.pong_received:
            self.status_var.set("Связь: ожидание ответа на PING")
        elif not live:
            self.status_var.set("Связь: нет данных от стенда")
        elif not self.data_valid:
            self.status_var.set("Связь есть, но стенд не получает данные датчика")
        else:
            self.status_var.set("Связь есть")

        mode_text = MODE_NAMES.get(self.board_mode, "-") if live else "-"
        self.mode_var.set(f"Указатель на стенде: {mode_text}")
        self.draw_dial(live)
        self.root.after(POLL_MS, self.poll)

    # --- рисование: повторяет level_draw_pointer() прошивки ---

    def draw_dial(self, live=False):
        c = self.canvas
        c.delete("all")
        cx = cy = DIAL_SIZE / 2
        fg = "#40c8ff" if live else "#40505a"
        c.create_oval(cx - DIAL_R, cy - DIAL_R, cx + DIAL_R, cy + DIAL_R, outline=fg, width=2)

        if not live or not self.data_valid:
            return
        if self.angle is None:
            r = 3 * DIAL_SCALE
            c.create_oval(cx - r, cy - r, cx + r, cy + r, fill=fg, outline=fg)
            c.create_text(cx, cy + DIAL_R + 14, text="стенд горизонтален: направление не определено",
                          fill="#8899a0", font=("Segoe UI", 8))
            return

        a = math.radians(self.angle)
        ux, uy = math.cos(a), math.sin(a)
        if self.board_mode == "A":
            tip = (30 - 3) * DIAL_SCALE
            tx, ty = cx + ux * tip, cy + uy * tip
            head = 8 * DIAL_SCALE
            h = math.radians(ARROW_HEAD_DEG)
            c.create_line(cx, cy, tx, ty, fill=fg, width=3)
            c.create_polygon(tx, ty,
                             tx - math.cos(a - h) * head, ty - math.sin(a - h) * head,
                             tx - math.cos(a + h) * head, ty - math.sin(a + h) * head,
                             fill=fg, outline=fg)
            r = DIAL_SCALE
            c.create_oval(cx - r, cy - r, cx + r, cy + r, fill=fg, outline=fg)
        else:
            r = (30 - 1) * DIAL_SCALE
            hx, hy = -uy * r, ux * r
            c.create_line(cx - hx, cy - hy, cx + hx, cy + hy, fill=fg, width=3)


def main():
    ap = argparse.ArgumentParser(description="Клиент ЛР3 «Уровень» для стенда SDK-1.1M")
    ap.add_argument("port", help="имя COM-порта стенда, например COM3")
    ap.add_argument("--log", help="файл журнала обмена (по умолчанию level_<дата>_<время>.log)")
    args = ap.parse_args()

    log_path = args.log or datetime.datetime.now().strftime("level_%Y%m%d_%H%M%S.log")
    journal = Journal(log_path)
    try:
        link = Link(args.port, journal)
    except serial.SerialException as exc:
        journal.close()
        sys.exit(f"Не удалось открыть {args.port}: {exc}")

    root = tk.Tk()
    App(root, link, args.port, log_path)

    def on_close():
        link.close()
        journal.close()
        root.destroy()

    root.protocol("WM_DELETE_WINDOW", on_close)
    root.mainloop()


if __name__ == "__main__":
    main()
