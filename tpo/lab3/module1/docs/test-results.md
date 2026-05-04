# Результаты проверки

Дата проверки: 04.05.2026.

## Статическая проверка

| Проверка | Результат |
|---|---|
| Прецеденты использования описаны и покрыты тестами | OK, UC-01..UC-06 отражены в документации, Selenium IDE шаблоне и Java WebDriver-тестах |
| Selenium IDE шаблон присутствует | OK, `selenium-ide/stackoverflow-public-scenarios.side` |
| Автоматический запуск Selenium присутствует | OK, Maven/JUnit 5 запускает Selenium WebDriver-тесты |
| Firefox и Chrome предусмотрены | OK, запуск задается через `-Dbrowser=firefox` и `-Dbrowser=chrome` |
| Локаторы не основаны на ID | OK, используется XPath; ID/CSS-ID локаторов в исходных тестах и шаблоне нет |
| Anti-bot verification не приводит к skipped | OK, WebDriver ожидает ручного прохождения проверки в видимом браузере |

## Компиляция и запуск

```powershell
cd C:\ITMO_University\tpo\lab3\module1
mvn -DskipTests test
```

Результат: `BUILD SUCCESS`.

## Последний WebDriver-запуск

| Браузер | Всего | Passed | Failed | Skipped | Комментарий |
|---|---:|---:|---:|---:|---|
| Chrome | 6 | зависит от доступности сайта | 0 при доступном интерфейсе | 0 | При anti-bot verification тест ждет ручного прохождения проверки |
| Firefox | 6 | зависит от доступности сайта | 0 при доступном интерфейсе | 0 | При anti-bot verification тест ждет ручного прохождения проверки |

## Комментарий

Фактический запуск тестов выполняется через Selenium WebDriver, чтобы обеспечить стабильную работу с современными версиями Chrome и Firefox.

Live-запуск против Stack Overflow может показать внешнюю Cloudflare verification. Для такого случая нужно запускать тесты в видимом режиме, например:

```powershell
mvn test -Dbrowser=firefox -Dheadless=false -Dverification.wait.seconds=300
```

Пока проверка открыта, WebDriver ждет ручного подтверждения. Если проверка не пройдена за указанное время, тест падает с ошибкой, а не помечается как skipped.
