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

### Анонимные сценарии

| ID | Прецедент | Актор | Предусловие | Основной сценарий | Ожидаемый результат |
|---|---|---|---|---|---|
| UC-01 | Просмотр новых вопросов | Гость | Сайт доступен | Открыть `/questions`, проверить заголовок, список вопросов и навигацию | Пользователь видит список вопросов и кнопку Ask Question |
| UC-02 | Поиск вопроса | Гость | Открыта страница вопросов | Ввести `selenium webdriver` в строку поиска и отправить форму | Открыта страница поиска с результатами |
| UC-03 | Открытие вопроса | Гость | В списке есть вопросы | Кликнуть первый вопрос | Открыта страница вопроса с заголовком, текстом и тегами |
| UC-04 | Открытие популярного тега | Гость | Открыта страница тегов | Найти в списке популярный тег `python` и открыть его | Открыта страница вопросов по тегу `python` |
| UC-05 | Сортировка вопросов | Гость | Открыта страница вопросов | Нажать вкладку `Active` | Список переключен на активные вопросы |
| UC-06 | Попытка задать вопрос без входа | Гость | Пользователь не авторизован | Нажать `Ask Question` | Сайт показывает страницу входа |

### Авторизованные сценарии (повторение UC-01..UC-06 от имени зарегистрированного пользователя)

| ID | Прецедент | Актор | Предусловие | Основной сценарий | Ожидаемый результат |
|---|---|---|---|---|---|
| UC-01a | Просмотр новых вопросов авторизованным | Зарегистрированный пользователь | Сессия активна (cookies загружены) | Открыть `/questions`, проверить заголовок, список вопросов, навигацию и аватар | Виден список вопросов, кнопка Ask Question, аватар; ссылки Log in нет |
| UC-02a | Поиск вопроса авторизованным | Зарегистрированный пользователь | Открыта страница вопросов под логином | Ввести `selenium webdriver` и отправить форму | Открыта страница поиска с результатами; аватар по-прежнему виден |
| UC-03a | Открытие вопроса авторизованным | Зарегистрированный пользователь | В списке есть вопросы | Кликнуть первый вопрос | Открыта страница вопроса; видны кнопки голосования (доступны только авторизованным) |
| UC-04a | Открытие популярного тега авторизованным | Зарегистрированный пользователь | Открыта страница тегов под логином | Найти и открыть тег `python` | Открыта страница вопросов по тегу `python` |
| UC-05a | Сортировка вопросов авторизованным | Зарегистрированный пользователь | Открыта страница вопросов под логином | Нажать вкладку `Active` | Список переключен на активные вопросы |
| UC-06a | Задание вопроса авторизованным | Зарегистрированный пользователь | Пользователь авторизован | Нажать `Ask Question` | Открывается форма создания вопроса (НЕ редирект на login) |

### Сценарий взаимодействия двух пользователей

| ID | Прецедент | Актор | Предусловие | Основной сценарий | Ожидаемый результат |
|---|---|---|---|---|---|
| UC-X | Создание вопроса, ответ и удаление | Два зарегистрированных пользователя (User1, User2) | Обе сессии активны | (1) User1 создаёт вопрос по python с уникальным заголовком; (2) User2 публикует ответ; (3) User1 видит ответ на странице; (4) User2 удаляет свой ответ; (5) User1 удаляет вопрос | Вопрос и ответ опубликованы, оба удалены без следов спама |

## Матрица покрытия

### Анонимные тесты — `StackOverflowFunctionalTest`

| Тест | Покрытые прецеденты | Проверяемые элементы | XPath-подход |
|---|---|---|---|
| `newestQuestionsPageShowsQuestionListAndNavigation` | UC-01 | Заголовок, список вопросов, Ask Question, Tags | По тексту, `href` и классам Stack Overflow |
| `visitorSearchesQuestionsByTextQuery` | UC-02 | Форма поиска, результаты | По `action='/search'`, `name='q'`, ссылкам `/questions/` |
| `visitorOpensQuestionAndSeesContent` | UC-03 | Ссылка вопроса, тело вопроса, теги | По контейнерам `s-post-summary`, `question`, `post-tag` |
| `visitorOpensPopularTagPage` | UC-04 | Список тегов, тег `python` | По тексту тега и ссылке на страницу тега |
| `visitorSwitchesQuestionsToActiveSorting` | UC-05 | Вкладка Active | По `href` с `tab=Active` |
| `anonymousVisitorIsRedirectedToLoginWhenAskingQuestion` | UC-06 | Ask Question, страница входа | По `href='/questions/ask'` и тексту сообщения |

### Авторизованные тесты — `StackOverflowAuthenticatedTest`

| Тест | Покрытые прецеденты | Проверяемые элементы | XPath-подход |
|---|---|---|---|
| `authenticatedNewestQuestionsShowQuestionListAndAvatar` | UC-01a | Аватар, отсутствие Log in, список вопросов, Ask Question, Tags | XPath по `header`, `gravatar`, `href`, классам |
| `authenticatedUserSearchesQuestionsByTextQuery` | UC-02a | Форма поиска, результаты + аватар | Те же XPath что в UC-02 + проверка аватара |
| `authenticatedUserOpensQuestionAndSeesContent` | UC-03a | Заголовок, тело, теги, кнопка голосования | По `js-vote-up-btn`, `aria-label='up vote'` |
| `authenticatedUserOpensPopularTagPage` | UC-04a | Список тегов, тег `python` + аватар | По тексту тега и `href` |
| `authenticatedUserSwitchesQuestionsToActiveSorting` | UC-05a | Вкладка Active + аватар | По `href` с `tab=Active` |
| `authenticatedUserOpensAskQuestionForm` | UC-06a | Форма Ask Question (НЕ login) | По `name='post-title'`, `aria-label='Title'` |

### Сценарий двух пользователей — `StackOverflowTwoUserScenarioTest`

| Тест | Покрытые прецеденты | Проверяемые элементы | XPath-подход |
|---|---|---|---|
| `twoUserQuestionAnswerDeleteScenario` | UC-X | Title/Body/Tags формы вопроса, форма ответа, ANSWER_BLOCK, ссылки Delete, модальное окно подтверждения, DELETED_BADGE | По `name='post-title'`, `name='post-text'`, тексту `Delete`/`Post Your Answer`, классам `s-modal`/`js-popup-submit` |

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

## Запуск авторизованных тестов

Авторизованные сюиты (UC-01a..UC-06a и UC-X) требуют cookies из ручной сессии — Stack Overflow защищён Cloudflare и reCAPTCHA, автологин через UI не работает. Полная инструкция: [`auth-setup.md`](auth-setup.md).

Краткая шпаргалка:

```powershell
# Только анонимные тесты (cookies не нужны)
mvn test

# + 6 авторизованных тестов одного пользователя
mvn test -Dso.user1.cookies=.so-cookies/user1.json -Dheadless=false

# + сценарий двух пользователей (создание вопроса, ответ, удаление)
mvn test -Dso.user1.cookies=.so-cookies/user1.json -Dso.user2.cookies=.so-cookies/user2.json -Dheadless=false
```

Если соответствующее свойство не задано — тесты помечаются как **skipped** (через `Assumptions.assumeTrue`), а не падают. Это позволяет запускать `mvn test` без cookies и получать BUILD SUCCESS.
