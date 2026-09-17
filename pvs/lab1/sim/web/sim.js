'use strict';

/*
 * Графический интерфейс к журналам работы модели GPIO.
 * Никаких внешних библиотек: обычный JavaScript и встроенный SVG,
 * чтобы страница открывалась прямо с диска.
 */

const NOTES = {
    morse:    'Ввод восьми символов «...---..» нажатиями кнопки с дребезгом контактов, ' +
              'затем передача последовательности зелёным светодиодом.',
    overflow: 'Девять нажатий при ёмкости буфера восемь: девятое не запоминается, ' +
              'о чём сообщает частое мигание красным.',
    edge:     'Граница классификации: нажатия длительностью 299 и 301 мс при пороге 300 мс.',
    glitch:   'Сравнение правильного и наивного порядка записи управляющих регистров ' +
              'при переводе вывода в режим выхода.'
};

const TRACKS = [
    { port: 'C', pin: 15, label: 'PC15 — кнопка' },
    { port: 'D', pin: 13, label: 'PD13 — зелёный' },
    { port: 'D', pin: 14, label: 'PD14 — двухцветный, вывод A' },
    { port: 'D', pin: 15, label: 'PD15 — двухцветный, вывод B' }
];

const REGS = ['MODER', 'OTYPER', 'OSPEEDR', 'PUPDR', 'IDR', 'ODR', 'BSRR', 'LCKR', 'AFRL', 'AFRH'];
const MODE_NAMES = ['вход', 'выход', 'альт. функция', 'аналоговый'];
const PULL_NAMES = ['нет', 'вверх', 'вниз', '?'];

const state = {
    scenario: null,
    trace: null,
    duration: 0,
    cursor: 0,
    playing: false,
    speed: 10,
    filter: 'all',
    timer: null
};

const $ = (id) => document.getElementById(id);

/* ---------- выборка состояния на момент времени ---------- */

/* Уровень вывода на момент t: последнее изменение до этого момента */
function padLevelAt(port, pin, t) {
    let level = 'Z';
    for (const e of state.trace.pads) {
        if (e.t > t) { break; }
        if (e.port === port && e.pin === pin) { level = e.lvl; }
    }
    return level;
}

/* Значения регистров порта на момент t: воспроизводим записи по журналу */
function registersAt(port, t) {
    const values = {};
    for (const r of state.trace.regs) {
        if (r.t > t) { break; }
        if (r.port !== port) { continue; }
        if (r.rw === 'W' && r.reg === 'BSRR') { continue; }   /* BSRR читается как ноль */
        values[r.reg] = r.val;
    }
    return values;
}

function bicolorAt(t) {
    const a = padLevelAt('D', 14, t) === '1';
    const b = padLevelAt('D', 15, t) === '1';
    if (a && !b) { return 'yellow'; }
    if (!a && b) { return 'red'; }
    return 'off';
}

/* ---------- виртуальный стенд ---------- */

function drawStand() {
    const t = state.cursor;
    const pressed = padLevelAt('C', 15, t) === '0';
    const green = padLevelAt('D', 13, t) === '1';
    const bi = bicolorAt(t);

    const biFill = bi === 'yellow' ? '#ffcf3a' : bi === 'red' ? '#ff4b3a' : '#3a3f48';
    const biGlow = bi === 'off' ? 0 : 0.55;

    $('stand').innerHTML = `
      <rect x="0" y="0" width="260" height="120" rx="6" fill="#23262d"/>

      <circle cx="60" cy="60" r="${pressed ? 20 : 22}"
              fill="${pressed ? '#8d939e' : '#5a606b'}" stroke="#9aa1ad" stroke-width="2"/>
      <text x="60" y="100" fill="#c9ced8" font-size="11" text-anchor="middle">
        кнопка ${pressed ? 'нажата' : 'отпущена'}
      </text>

      <circle cx="150" cy="60" r="18" fill="${green ? '#3ddc5f' : '#2b3a2f'}"
              stroke="#4a5160" stroke-width="2" opacity="${green ? 1 : 0.85}"/>
      <text x="150" y="100" fill="#c9ced8" font-size="11" text-anchor="middle">зелёный</text>

      <circle cx="215" cy="60" r="18" fill="${biFill}" stroke="#4a5160" stroke-width="2"
              opacity="${bi === 'off' ? 0.85 : 1}"/>
      <circle cx="215" cy="60" r="26" fill="${biFill}" opacity="${biGlow * 0.35}"/>
      <text x="215" y="100" fill="#c9ced8" font-size="11" text-anchor="middle">
        ${bi === 'yellow' ? 'жёлтый' : bi === 'red' ? 'красный' : 'погашен'}
      </text>`;
}

/* ---------- временная диаграмма ---------- */

function drawTimeline() {
    const W = 1000, H = 220, left = 190, right = 20, top = 16;
    const rowH = (H - top - 26) / TRACKS.length;
    const plotW = W - left - right;
    const scale = (t) => left + (state.duration ? (t / state.duration) * plotW : 0);

    let svg = '';

    /* сетка времени */
    const stepMs = niceStep(state.duration);
    for (let t = 0; t <= state.duration; t += stepMs) {
        const x = scale(t);
        svg += `<line x1="${x}" y1="${top}" x2="${x}" y2="${H - 26}" stroke="#e8eaee"/>`;
        svg += `<text x="${x}" y="${H - 10}" font-size="10" fill="#5c6270" text-anchor="middle">` +
               `${(t / 1000).toFixed(t >= 1000 ? 1 : 2)}</text>`;
    }

    TRACKS.forEach((track, i) => {
        const yTop = top + i * rowH + 6;
        const yBot = top + (i + 1) * rowH - 8;
        const yMid = (yTop + yBot) / 2;

        svg += `<text x="8" y="${yMid + 4}" font-size="11.5" fill="#16181d">${track.label}</text>`;

        /* собираем переходы этой дорожки */
        const points = [{ t: 0, lvl: 'Z' }];
        for (const e of state.trace.pads) {
            if (e.port === track.port && e.pin === track.pin) { points.push(e); }
        }

        const yOf = (lvl) => lvl === '1' ? yTop : lvl === '0' ? yBot : yMid;
        let d = '';
        for (let k = 0; k < points.length; k++) {
            const x0 = scale(points[k].t);
            const x1 = k + 1 < points.length ? scale(points[k + 1].t) : scale(state.duration);
            const y = yOf(points[k].lvl);
            d += (k === 0 ? `M ${x0} ${y}` : ` L ${x0} ${y}`) + ` L ${x1} ${y}`;
        }
        const color = track.pin === 13 ? '#29a745'
                    : track.pin === 14 ? '#d8a200'
                    : track.pin === 15 && track.port === 'D' ? '#cf3a2e' : '#2f5fd0';
        svg += `<path d="${d}" fill="none" stroke="${color}" stroke-width="1.8"/>`;
    });

    const cx = scale(state.cursor);
    svg += `<line x1="${cx}" y1="${top - 6}" x2="${cx}" y2="${H - 26}" stroke="#16181d" stroke-width="1.2"/>`;

    $('timeline').innerHTML = svg;
}

function niceStep(duration) {
    const target = duration / 10;
    const steps = [10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000];
    for (const s of steps) { if (s >= target) { return s; } }
    return 10000;
}

/* ---------- инспектор регистров ---------- */

function field2(value, pin) { return (value >>> (pin * 2)) & 3; }

function decodePins(reg, value) {
    const pins = reg === 'MODER' || reg === 'PUPDR' || reg === 'OSPEEDR' ? [13, 14, 15] : [13, 14, 15];
    return pins.map((pin) => {
        if (reg === 'MODER')   { return `PD${pin}: ${MODE_NAMES[field2(value, pin)]}`; }
        if (reg === 'PUPDR')   { return `PD${pin}: ${PULL_NAMES[field2(value, pin)]}`; }
        if (reg === 'OSPEEDR') { return `PD${pin}: ${field2(value, pin)}`; }
        if (reg === 'OTYPER')  { return `PD${pin}: ${((value >>> pin) & 1) ? 'откр. сток' : 'двухтактный'}`; }
        return `PD${pin}: ${(value >>> pin) & 1}`;
    }).join('; ');
}

function drawRegisters() {
    const values = registersAt('D', state.cursor);
    let html = '<tr><th>Регистр</th><th class="num">Значение</th><th>Поля выводов PD13–PD15</th></tr>';

    for (const reg of REGS) {
        const v = values[reg] === undefined ? 0 : values[reg] >>> 0;
        const hex = '0x' + v.toString(16).toUpperCase().padStart(8, '0');
        const decoded = (reg === 'AFRL' || reg === 'AFRH' || reg === 'LCKR' || reg === 'BSRR')
            ? '' : decodePins(reg, v);
        html += `<tr><td>${reg}</td><td class="num">${hex}</td><td class="bits">${decoded}</td></tr>`;
    }
    $('registers').innerHTML = html;
}

/* ---------- журнал ---------- */

function drawLog() {
    const rows = [];

    if (state.filter !== 'app') {
        for (const r of state.trace.regs) {
            if (state.filter === 'W' && r.rw !== 'W') { continue; }
            rows.push({
                t: r.t,
                kind: r.rw === 'W' ? 'запись' : 'чтение',
                what: `GPIO${r.port}.${r.reg}`,
                value: '0x' + (r.val >>> 0).toString(16).toUpperCase().padStart(8, '0')
            });
        }
    }
    if (state.filter === 'app' || state.filter === 'all') {
        for (const a of state.trace.app) {
            rows.push({ t: a.t, kind: 'программа', what: a.msg, value: '' });
        }
    }
    rows.sort((x, y) => x.t - y.t);

    let html = '<tr><th class="num">Время, с</th><th>Тип</th><th>Объект</th><th class="num">Значение</th></tr>';
    for (const r of rows.slice(0, 600)) {
        html += `<tr class="log-row" data-t="${r.t}"><td class="num">${(r.t / 1000).toFixed(3)}</td>` +
                `<td>${r.kind}</td><td>${r.what}</td><td class="num">${r.value}</td></tr>`;
    }
    if (rows.length > 600) {
        html += `<tr><td colspan="4" class="bits">…показаны первые 600 из ${rows.length} записей; ` +
                `полный журнал — в файле trace_${state.scenario}.log</td></tr>`;
    }
    $('log').innerHTML = html;

    for (const row of document.querySelectorAll('.log-row')) {
        row.addEventListener('click', () => setCursor(Number(row.dataset.t)));
    }
}

/* ---------- управление ---------- */

function redraw() {
    $('clock').textContent = (state.cursor / 1000).toFixed(3) + ' с';
    $('cursor').value = String(state.cursor);
    drawStand();
    drawTimeline();
    drawRegisters();
}

function setCursor(t) {
    state.cursor = Math.max(0, Math.min(state.duration, Math.round(t)));
    redraw();
}

function loadScenario(name) {
    state.scenario = name;
    state.trace = window.TRACES[name];

    let max = 0;
    for (const list of [state.trace.regs, state.trace.pads, state.trace.app]) {
        for (const e of list) { if (e.t > max) { max = e.t; } }
    }
    state.duration = Math.max(max, 1);
    state.cursor = 0;

    $('cursor').max = String(state.duration);
    $('scenario-note').textContent = NOTES[name] || '';

    for (const b of document.querySelectorAll('#scenarios button')) {
        b.classList.toggle('active', b.dataset.name === name);
    }
    drawLog();
    redraw();
}

function togglePlay() {
    state.playing = !state.playing;
    $('play').textContent = state.playing ? 'Пауза' : 'Пуск';
    $('play').classList.toggle('active', state.playing);

    if (state.timer) { clearInterval(state.timer); state.timer = null; }
    if (state.playing) {
        state.timer = setInterval(() => {
            if (state.cursor >= state.duration) { togglePlay(); return; }
            setCursor(state.cursor + state.speed * 2);
        }, 33);
    }
}

function init() {
    const names = Object.keys(window.TRACES || {});
    if (names.length === 0) {
        document.body.insertAdjacentHTML('beforeend',
            '<p style="padding:20px">Журналы не найдены. Соберите и запустите симулятор: ' +
            'tools/build_sim.sh, затем sim/build/sim_morse.exe для каждого сценария.</p>');
        return;
    }

    $('scenarios').innerHTML = names
        .map((n) => `<button data-name="${n}">${n}</button>`).join('');
    for (const b of document.querySelectorAll('#scenarios button')) {
        b.addEventListener('click', () => loadScenario(b.dataset.name));
    }

    $('play').addEventListener('click', togglePlay);
    $('step-fwd').addEventListener('click', () => setCursor(state.cursor + 1));
    $('step-back').addEventListener('click', () => setCursor(state.cursor - 1));
    $('speed').addEventListener('click', () => {
        state.speed = state.speed === 10 ? 1 : state.speed === 1 ? 50 : 10;
        $('speed').textContent = '×' + state.speed;
    });
    $('cursor').addEventListener('input', (e) => setCursor(Number(e.target.value)));

    $('timeline').addEventListener('click', (e) => {
        const box = $('timeline').getBoundingClientRect();
        const x = ((e.clientX - box.left) / box.width) * 1000;
        const fraction = (x - 190) / (1000 - 190 - 20);
        setCursor(fraction * state.duration);
    });

    for (const b of document.querySelectorAll('.filter')) {
        b.addEventListener('click', () => {
            state.filter = b.dataset.filter;
            for (const other of document.querySelectorAll('.filter')) {
                other.classList.toggle('active', other === b);
            }
            drawLog();
        });
    }

    loadScenario(names.includes('morse') ? 'morse' : names[0]);
}

init();
