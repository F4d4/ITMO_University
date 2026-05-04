# Лабораторная работа. Вариант 2422

Тестируемый сайт: Stack Overflow, публичные разделы вопросов и тегов.

## Проверка требований ТЗ

| Требование | Реализация в проекте |
|---|---|
| Тестовое покрытие сформировано на основании прецедентов использования сайта | Прецеденты UC-01..UC-06 описаны ниже, каждый автотест привязан к одному пользовательскому сценарию |
| Тестирование выполняется автоматически с помощью Selenium | Автоматический запуск реализован в Maven-модуле `selenium-rc` через `com.thoughtworks.selenium.DefaultSelenium`; дополнительный современный WebDriver-runner оставлен в основном модуле |
| Шаблоны сформированы при помощи Selenium IDE | Современный шаблон: `selenium-ide/stackoverflow-public-scenarios.side`; RC-совместимые HTML/Selenese шаблоны: `selenium-ide/legacy-rc/TestSuite.html` |
| Шаблоны исполняются Selenium RC в Firefox и Chrome | Для прямого запуска IDE HTML suite через RC: `selenium-rc/run-ide-suite.ps1`; для JUnit-обертки над теми же RC-сценариями: `mvn test -Drc.browser=*firefox` и `mvn test -Drc.browser=*googlechrome` |
| Выбор элементов DOM не основан на ID | В Java-тестах и Selenium IDE шаблонах используются только XPath-локаторы; `By.id`, `id=...` и CSS `#...` не применяются |

## Прецеденты использования

| ID | Прецедент | Актор | Предусловие | Основной сценарий | Ожидаемый результат |
|---|---|---|---|---|---|
| UC-01 | Просмотр новых вопросов | Гость | Сайт доступен | Открыть `/questions`, проверить заголовок, список вопросов и навигацию | Пользователь видит список вопросов и кнопку Ask Question |
| UC-02 | Поиск вопроса | Гость | Открыта страница вопросов | Ввести `selenium webdriver` в строку поиска и отправить форму | Открыта страница поиска с результатами |
| UC-03 | Открытие вопроса | Гость | В списке есть вопросы | Кликнуть первый вопрос | Открыта страница вопроса с заголовком, текстом и тегами |
| UC-04 | Открытие популярного тега | Гость | Открыта страница тегов | Найти в списке популярный тег `python` и открыть его | Открыта страница вопросов по тегу `python` |
| UC-05 | Сортировка вопросов | Гость | Открыта страница вопросов | Нажать вкладку `Active` | Список переключен на активные вопросы |
| UC-06 | Попытка задать вопрос без входа | Гость | Пользователь не авторизован | Нажать `Ask Question` | Сайт показывает страницу входа |

## Матрица покрытия

| Тест | Покрытые прецеденты | Проверяемые элементы | XPath-подход |
|---|---|---|---|
| `newestQuestionsPageShowsQuestionListAndNavigation` | UC-01 | Заголовок, список вопросов, Ask Question, Tags | По тексту, `href` и классам Stack Overflow |
| `visitorSearchesQuestionsByTextQuery` | UC-02 | Форма поиска, результаты | По `action='/search'`, `name='q'`, ссылкам `/questions/` |
| `visitorOpensQuestionAndSeesContent` | UC-03 | Ссылка вопроса, тело вопроса, теги | По контейнерам `s-post-summary`, `question`, `post-tag` |
| `visitorOpensPopularTagPage` | UC-04 | Список тегов, тег `python` | По тексту тега и ссылке на страницу тега |
| `visitorSwitchesQuestionsToActiveSorting` | UC-05 | Вкладка Active | По `href` с `tab=Active` |
| `anonymousVisitorIsRedirectedToLoginWhenAskingQuestion` | UC-06 | Ask Question, страница входа | По `href='/questions/ask'` и тексту сообщения |

## Запуск Selenium RC

Сначала можно запустить Selenium RC server в отдельном терминале:

```powershell
cd C:\ITMO_University\tpo\lab3\module1\selenium-rc
.\start-rc-server.ps1
```

Затем выполнить JUnit-тесты через Selenium RC:

```powershell
cd C:\ITMO_University\tpo\lab3\module1\selenium-rc
mvn test -Drc.browser=*firefox
mvn test -Drc.browser=*googlechrome
```

Также можно выполнить непосредственно HTML-шаблон Selenium IDE через Selenium RC:

```powershell
cd C:\ITMO_University\tpo\lab3\module1\selenium-rc
.\run-ide-suite.ps1 *firefox
.\run-ide-suite.ps1 *googlechrome
```

Основной Maven-модуль с Selenium WebDriver остается дополнительной проверкой для современных браузеров:

```powershell
cd C:\ITMO_University\tpo\lab3\module1
mvn test -Dbrowser=chrome
mvn test -Dbrowser=firefox
```
