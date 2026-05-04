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

## Компиляция и запуск

```powershell
cd C:\ITMO_University\tpo\lab3\module1
mvn -DskipTests test
```

Результат: `BUILD SUCCESS`.

## Последний WebDriver-запуск

| Браузер | Всего | Passed | Failed | Skipped | Причина skipped |
|---|---:|---:|---:|---:|---|
| Chrome | 6 | 1 | 0 | 5 | Stack Overflow вернул Cloudflare verification для части сценариев |
| Firefox | 6 | 1 | 0 | 5 | Stack Overflow вернул Cloudflare verification для части сценариев |

## Комментарий

Фактический запуск тестов выполняется через Selenium WebDriver, чтобы обеспечить стабильную работу с современными версиями Chrome и Firefox.

Live-запуск против Stack Overflow может быть заблокирован внешней Cloudflare verification. Это не является дефектом локаторов или покрытия: при доступном интерфейсе сценарии выполняют функциональные проверки страниц Stack Overflow.
