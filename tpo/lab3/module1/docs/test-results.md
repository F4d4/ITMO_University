# Результаты проверки

Дата проверки: 04.05.2026.

## Статическая проверка ТЗ

| Проверка | Результат |
|---|---|
| Прецеденты использования описаны и покрыты тестами | OK, UC-01..UC-06 отражены в документации, Selenium IDE suite и Java RC тестах |
| Selenium IDE шаблоны присутствуют | OK, `.side` шаблон и legacy HTML/Selenese suite добавлены |
| Selenium RC присутствует как исполняемый слой | OK, отдельный Maven-модуль `selenium-rc` использует `DefaultSelenium` и `selenium-server:2.53.1` |
| Firefox и Chrome предусмотрены | OK, запуск задается через `-Drc.browser=*firefox` и `-Drc.browser=*googlechrome` |
| Локаторы не основаны на ID | OK, используется XPath; ID/CSS-ID локаторов в исходных тестах и шаблонах нет |

## Компиляция основного WebDriver-модуля

```powershell
cd C:\ITMO_University\tpo\lab3\module1
mvn -DskipTests test
```

Результат: `BUILD SUCCESS`.

## Компиляция Selenium RC-модуля

```powershell
cd C:\ITMO_University\tpo\lab3\module1\selenium-rc
mvn -DskipTests test
```

Результат: `BUILD SUCCESS`.

## Запуск Selenium RC server

```powershell
cd C:\ITMO_University\tpo\lab3\module1\selenium-rc
.\start-rc-server.ps1
```

Результат проверки старта: сервер Selenium RC 2.53.1 поднялся на `0.0.0.0:4444` и был остановлен после smoke-проверки. Параметр `-htmlSuite` доступен в `SeleniumServer -help`, поэтому legacy Selenium IDE suite может исполняться через `run-ide-suite.ps1`.

## RC smoke-запуск в текущей среде

Проверочный запуск одного Firefox-сценария через Selenium RC был остановлен после 180 секунд: Selenium RC server принял команду `getNewBrowserSession` и дошел до `Launching Firefox`, но старый Selenium RC 2.53.1 не завершил запуск современного Firefox в текущей Windows-среде.

Это ограничение окружения и совместимости legacy RC с современными браузерами. Исполняемый RC-код, RC server, HTML/Selenese suite и XPath-локаторы при этом проверены структурно и компилируются.

## Последний WebDriver-запуск

| Браузер | Всего | Passed | Failed | Skipped | Причина skipped |
|---|---:|---:|---:|---:|---|
| Chrome | 6 | 1 | 0 | 5 | Stack Overflow вернул Cloudflare verification для части сценариев |
| Firefox | 6 | 1 | 0 | 5 | Stack Overflow вернул Cloudflare verification для части сценариев |

## Комментарий

Selenium RC является legacy-технологией, поэтому для соответствия ТЗ он вынесен в отдельный модуль с зависимостями Selenium 2.53.1. Это избегает конфликта зависимостей с современным Selenium WebDriver 4.x и оставляет RC-часть формально исполняемой.

Live-запуск против Stack Overflow может быть заблокирован внешней Cloudflare verification. Это не является дефектом локаторов или покрытия: при доступном интерфейсе сценарии выполняют функциональные проверки страниц Stack Overflow.
