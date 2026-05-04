# Лабораторная работа. Вариант 2422

Тестируемый сайт: Stack Overflow, публичные разделы вопросов и тегов.

## Проверка требований

| Требование | Реализация в проекте |
|---|---|
| Тестовое покрытие сформировано на основании прецедентов использования сайта | Прецеденты UC-01..UC-06 описаны ниже, каждый автотест привязан к пользовательскому сценарию |
| Тестирование выполняется автоматически с помощью Selenium | Автоматический запуск реализован через Selenium WebDriver и Maven/JUnit 5 |
| Шаблоны сформированы при помощи Selenium IDE | Selenium IDE шаблон сохранен в `selenium-ide/stackoverflow-public-scenarios.side` |
| Запуск выполняется в Firefox и Chrome | Браузер выбирается параметром `-Dbrowser=firefox` или `-Dbrowser=chrome` |
| Выбор элементов DOM не основан на ID | В Java-тестах и Selenium IDE шаблоне используются XPath-локаторы; `By.id`, `id=...` и CSS `#...` не применяются |

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

## Запуск WebDriver-тестов

Chrome:

```powershell
cd C:\ITMO_University\tpo\lab3\module1
mvn test -Dbrowser=chrome
```

Firefox:

```powershell
cd C:\ITMO_University\tpo\lab3\module1
mvn test -Dbrowser=firefox
```

Видимый режим браузера:

```powershell
mvn test -Dbrowser=chrome -Dheadless=false
```

Если Stack Overflow показывает проверку "are you human", запускайте тесты в видимом режиме. WebDriver будет ждать ручного прохождения проверки до 180 секунд:

```powershell
mvn test -Dbrowser=firefox -Dheadless=false
```

Время ожидания можно увеличить:

```powershell
mvn test -Dbrowser=firefox -Dheadless=false -Dverification.wait.seconds=300
```

Если проверка Cloudflare в Chrome повторяется бесконечно, запустите обычный Chrome с портом удаленной отладки:

```powershell
$profile="$env:TEMP\selenium-stackoverflow-chrome"
Start-Process "chrome.exe" -ArgumentList "--remote-debugging-port=9222","--user-data-dir=$profile"
```

В открывшемся Chrome вручную откройте `https://stackoverflow.com/questions` и пройдите проверку. Затем, не закрывая это окно, запустите тесты с подключением к нему:

```powershell
mvn test -Dbrowser=chrome -Dheadless=false -Dchrome.debuggerAddress=127.0.0.1:9222 -Dverification.wait.seconds=300
```

Selenium IDE шаблон можно открыть в расширении Selenium IDE через `Open an existing project` и выбрать файл:

```text
C:\ITMO_University\tpo\lab3\module1\selenium-ide\stackoverflow-public-scenarios.side
```
