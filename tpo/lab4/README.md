# Лабораторная работа №4 — Нагрузочное и стресс-тестирование (Apache JMeter)

Нагрузочное и стресс-тестирование веб-приложения `stload.se.ifmo.ru` средствами Apache JMeter 5.6.3.
Полная постановка — в [task.md](task.md).

## Краткие результаты

**Нагрузочный тест** (3 конфигурации при штатной нагрузке 100 запр./мин, SLA ≤ 840 мс):

| config | цена | время отклика (avg/P95/max) | вердикт |
|---|---:|---:|:--:|
| cfg1 | $3000 | 1114 / 1115 / 1121 мс | ✗ не проходит |
| **cfg2** | **$3600** | **714 / 715 / 721 мс** | **✓ выбрана (самая дешёвая)** |
| cfg3 | $5900 | 314 / 315 / 321 мс | ✓ проходит (дороже) |

**Стресс-тест** cfg2: SLA (840 мс) нарушается при нагрузке **≈ 800 запр./мин** (последний проходящий уровень — 700 запр./мин). HTTP 503 не зафиксированы вплоть до 1500 запр./мин — сервер деградирует по времени отклика.

## Документация (`docs/`)

- [methodology.md](docs/methodology.md) — методика, стенд, тест-план, критерии, команды
- [load-results.md](docs/load-results.md) — результаты нагрузочного теста, выбор конфигурации
- [stress-results.md](docs/stress-results.md) — результаты стресс-теста, график «время отклика vs нагрузка»
- [theory.md](docs/theory.md) — теория тестирования производительности и JMeter

## Артефакты

| Файл | Назначение |
|---|---|
| [load-test.jmx](load-test.jmx) | Параметризуемый тест-план JMeter (один для всех прогонов) |
| [run-load.sh](run-load.sh) | Нагрузочный тест 3 конфигураций (запуск на `hel`) |
| [run-stress.sh](run-stress.sh) | Стресс-тест: лесенка нагрузки 100…1500 запр./мин (идемпотентный) |
| [metrics.sh](metrics.sh) | Подсчёт метрик из JTL (awk/sort/sed, без Python) |
| `results/` | JTL-результаты, HTML-дашборды JMeter (`report_cfg*`), стресс-уровни |
| `docs/img/` | Графики, построенные JMeter (RT-over-time, TimesVsThreads, ThroughputVsThreads) |

## Воспроизведение

Приложение доступно только из внутренней сети кафедры, поэтому JMeter запускается на учебной машине `helios` (`ssh hel`):

```bash
# на hel: установить JMeter, плагины (jpgc-graphs-vs, CMDRunner) — см. methodology.md §2,§7
scp load-test.jmx run-load.sh run-stress.sh metrics.sh hel:~/lab4/
ssh hel 'cd ~/lab4 && bash run-load.sh && bash run-stress.sh 2'
ssh hel 'bash ~/lab4/metrics.sh load; bash ~/lab4/metrics.sh stress'
# графики: CMDRunner --tool Reporter --plugin-type TimesVsThreads (см. methodology.md §7)
scp -r 'hel:~/lab4/results' ./ ; scp 'hel:~/lab4/graphs/*.png' docs/img/
```
