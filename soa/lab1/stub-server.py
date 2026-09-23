"""Заглушка веб-сервисов лабораторной работы №1.

Раздаёт статическую документацию Swagger UI и отвечает на запросы к API так,
как это описано в спецификации: коллекция билетов пуста, операции её изменения
пока не реализованы. Нужна для того, чтобы кнопка Try it out возвращала
настоящие JSON-ответы. Реализация сервисов на Java появится в следующей
лабораторной работе.

Запуск:  python stub-server.py [порт]        (по умолчанию 8000)
"""

import json
import pathlib
import re
import sys
from datetime import datetime
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

DATE_FORMAT = "%d.%m.%Y %H:%M:%S"

WRITE_NOT_IMPLEMENTED = (
    "Изменение коллекции пока не реализовано: развёрнута только спецификация. "
    "Реализация сервиса появится в следующей лабораторной работе"
)


def error(status, name, message):
    return status, {
        "timestamp": datetime.now().strftime(DATE_FORMAT),
        "status": status,
        "error": name,
        "message": message,
    }


def not_found(message):
    return error(404, "Not Found", message)


def unavailable(_match, _query):
    return error(503, "Service Unavailable", WRITE_NOT_IMPLEMENTED)


def ticket_page(_match, query):
    page = int(query.get("page", ["1"])[0])
    size = int(query.get("size", ["20"])[0])
    return 200, {"items": [], "page": page, "size": size, "totalElements": 0, "totalPages": 0}


def average_price(_match, _query):
    return 200, {"average": 0, "count": 0}


def min_comment(_match, _query):
    return not_found("В коллекции нет билетов с непустым полем comment")


def ticket_by_id(match, _query):
    return not_found(f"Билет с id = {match['id']} не найден")


def sell_vip(match, _query):
    return not_found(f"Билет с id = {match['ticketId']} не найден в коллекции первого сервиса")


def cancel_bookings(match, _query):
    person = int(match["personId"])
    return 200, {"personId": person, "cancelledCount": 0, "cancelledTicketIds": []}


# Порядок важен: более специфичные пути идут первыми.
ROUTES = [
    ("GET", r"/tickets/price/average", average_price),
    ("GET", r"/tickets/comment/min", min_comment),
    ("GET", r"/tickets/(?P<id>\d+)", ticket_by_id),
    ("GET", r"/tickets", ticket_page),
    ("POST", r"/tickets", unavailable),
    ("PUT", r"/tickets/(?P<id>\d+)", unavailable),
    ("DELETE", r"/tickets/(?P<id>\d+)", unavailable),
    ("DELETE", r"/tickets/price/(?P<price>[0-9.]+)", unavailable),
    ("POST", r"/booking/sell/vip/(?P<ticketId>\d+)/(?P<personId>\d+)", sell_vip),
    ("POST", r"/booking/person/(?P<personId>\d+)/cancel", cancel_bookings),
]

COMPILED = [(method, re.compile(f"^{pattern}/?$"), handler) for method, pattern, handler in ROUTES]


class StubHandler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if not self.serve_api("GET"):
            super().do_GET()

    def do_HEAD(self):
        super().do_HEAD()

    def do_POST(self):
        self.serve_api("POST") or self.send_json(*not_found(f"Ресурс {self.path} не найден"))

    def do_PUT(self):
        self.serve_api("PUT") or self.send_json(*not_found(f"Ресурс {self.path} не найден"))

    def do_DELETE(self):
        self.serve_api("DELETE") or self.send_json(*not_found(f"Ресурс {self.path} не найден"))

    def serve_api(self, method):
        parsed = urlparse(self.path)
        for route_method, pattern, handler in COMPILED:
            if route_method != method:
                continue
            match = pattern.match(parsed.path)
            if match:
                self.read_body()
                status, payload = handler(match, parse_qs(parsed.query))
                if status >= 400:
                    payload["path"] = parsed.path
                self.send_json(status, payload)
                return True
        return False

    def read_body(self):
        length = int(self.headers.get("Content-Length", 0))
        if length:
            self.rfile.read(length)

    def send_json(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json;charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    handler = partial(StubHandler, directory=str(pathlib.Path(__file__).parent))
    print(f"Документация и заглушка API: http://localhost:{port}/")
    ThreadingHTTPServer(("", port), handler).serve_forever()
