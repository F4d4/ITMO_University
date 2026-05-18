# Разбор кода — `lab3/module1`

Подробное описание устройства Selenium-проекта тестирования Stack Overflow (вариант 2422). Документ дополняет:
- [`test-coverage.md`](test-coverage.md) — соответствие требованиям, прецеденты UC-01..UC-06, инструкции запуска
- [`test-results.md`](test-results.md) — результаты статической проверки и протокол прогона

---

## 1. Что это за проект

Учебный модуль, автоматически проверяющий публичные сценарии Stack Overflow без авторизации: просмотр списка вопросов, поиск, открытие вопроса, переход по тегу, переключение сортировки, обработка попытки задать вопрос анонимом.

**Технологический стек:**

| Компонент | Версия | Назначение |
|---|---|---|
| Java | 17 | Язык, `maven.compiler.release=17` |
| Maven | — | Сборка, прогон тестов, генерация Allure-отчёта |
| JUnit Jupiter | 5.10.1 | Тестовый фреймворк |
| Selenium WebDriver | 4.19.1 | Управление Chrome/Firefox |
| Allure | 2.34.0 (BOM) | Отчётность (`allure-junit5`) |
| Allure Maven plugin | 2.17.0 | Генерация HTML-отчёта |

**Структура каталогов:**

```
lab3/module1/
├── pom.xml                                  ← Maven-конфигурация
├── .gitignore                               ← /target/
├── .allure/allure-2.34.0/                   ← локальная установка Allure CLI
├── src/
│   ├── main/java/com/example/App.java       ← пустая заглушка
│   └── test/java/com/example/stackoverflow/
│       ├── StackOverflowFunctionalTest.java ← 6 тестов UC-01..UC-06
│       └── StackOverflowLocators.java       ← все XPath-локаторы
├── selenium-ide/
│   └── stackoverflow-public-scenarios.side  ← .side-шаблон тех же сценариев
└── docs/
    ├── test-coverage.md
    ├── test-results.md
    └── code-analysis.md                     ← этот файл
```

---

## 2. Конфигурация сборки — `pom.xml`

Артефакт: `com.example:stackoverflow-selenium-lab:1.0-SNAPSHOT`.

### 2.1. Конфигурируемые свойства

Описаны в `<properties>`, перепробрасываются `maven-surefire-plugin` в JVM теста:

| Свойство | По умолчанию | Назначение |
|---|---|---|
| `browser` | `chrome` | Какой драйвер поднимать (`chrome` / `firefox`) |
| `headless` | `true` | Запуск без UI |
| `selenium.timeout.seconds` | `20` | Базовый таймаут `WebDriverWait` |
| `verification.wait.seconds` | `180` (в коде, не в pom) | Сколько ждать ручного прохождения anti-bot верификации |
| `chrome.debuggerAddress` | пусто | Адрес уже запущенного Chrome для подключения через CDP |

Любое значение можно переопределить из CLI: `mvn test -Dbrowser=firefox -Dheadless=false`.

### 2.2. Зависимости

Через `dependencyManagement` подключён Allure BOM, благодаря чему в `<dependencies>` версия Allure указывать не нужно:

```xml
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>${junit.jupiter.version}</version>
</dependency>
<dependency>
  <groupId>org.seleniumhq.selenium</groupId>
  <artifactId>selenium-java</artifactId>
  <version>${selenium.version}</version>
</dependency>
<dependency>
  <groupId>io.qameta.allure</groupId>
  <artifactId>allure-junit5</artifactId>
</dependency>
```

Все три — `scope=test`, продакшен-кода нет.

### 2.3. Плагины

| Плагин | Зачем |
|---|---|
| `maven-compiler-plugin:3.11.0` | Компиляция под Java 17 |
| `maven-surefire-plugin:3.2.5` | Прогон JUnit-тестов; пробрасывает `browser`, `headless`, `selenium.timeout.seconds`, `allure.results.directory=${project.build.directory}/allure-results` |
| `allure-maven:2.17.0` | Цели `allure:report`, `allure:serve`, читает результаты из `target/allure-results` |

---

## 3. Точка входа `src/main/java/com/example/App.java`

Класс пустой:

```java
public class App {
}
```

Существует только потому, что Maven по умолчанию ожидает наличие `src/main/java`. Вся логика — в тестовом коде. Заглушка позволяет команде `mvn -DskipTests test` (см. `test-results.md`) проходить как `BUILD SUCCESS`.

---

## 4. Локаторы — `StackOverflowLocators.java`

Финальный utility-класс с приватным конструктором — нельзя ни наследовать, ни инстанцировать. Все константы — `static final By`, только XPath. Это требование лабораторной работы: «выбор элементов DOM не основан на ID» (см. `test-coverage.md`). Никаких `By.id(...)`, `By.cssSelector("#…")` в коде нет.

### 4.1. Навигация и общие элементы

| Локатор | XPath (упрощённо) | Где используется |
|---|---|---|
| `PAGE_HEADING` | `//h1[normalize-space()]` | Любой `<h1>` с непустым текстом — общий маркер «страница загрузилась» |
| `ASK_QUESTION` | `//a[href contains '/questions/ask' AND text contains 'Ask Question']` | UC-01, UC-06 |
| `TAGS_NAVIGATION` | `//a[href contains '/tags' AND text contains 'Tags']` | UC-01 |
| `COOKIE_BUTTON` | `//button[text contains 'accept' OR 'necessary']` (case-insensitive через `translate`) | Закрытие cookie-баннера |

### 4.2. Поиск

| Локатор | Идея |
|---|---|
| `MAIN_SEARCH` | `//form[@action contains '/search']//input[(name~='q' OR aria-label~='Search') AND not(@type='hidden')]` |
| `SEARCH_RESULTS` | Любые ссылки `/questions/...`, исключая `/questions/ask` и `/questions/tagged/...` |

`not(@type='hidden')` важен: на странице есть скрытые поля с `name="q"`, без фильтра WebDriver попытается ввести текст в невидимый input и провалится.

### 4.3. Список вопросов

| Локатор | Идея |
|---|---|
| `QUESTION_SUMMARIES` | `//div[class contains 's-post-summary' OR 'question-summary']` |
| `FIRST_QUESTION_LINK` | Первый `<a>` внутри `<h3>` вопроса с непустым текстом и `href` содержащим `/questions/` |

Поддерживаются оба класса (`s-post-summary` — текущий, `question-summary` — legacy), чтобы тест не сломался при A/B-тестах Stack Overflow.

### 4.4. Страница вопроса

| Локатор | Идея |
|---|---|
| `QUESTION_TITLE` | `//h1[normalize-space()]` |
| `QUESTION_BODY` | Контейнер с `s-prose` или `@itemprop='text'` внутри блока вопроса; либо любой `s-prose` с непустым текстом |
| `POST_TAGS` | `//a[class contains 'post-tag' AND непустой текст]` |

### 4.5. Авторизация

| Локатор | Идея |
|---|---|
| `LOGIN_REQUIRED_MESSAGE` | Любой узел с текстом «You must be logged in to ask a question» или «Log in below» |

### 4.6. Теги и сортировка

| Локатор | Идея |
|---|---|
| `TAG_SEARCH_INPUT` | Input с placeholder/aria-label/name, содержащим «filter» и «tag» (case-insensitive) |
| `PYTHON_TAG_LINK` | `<a>` с текстом ровно `python` и href на `/questions/tagged/python` или `/tags/python` |
| `ACTIVE_TAB` | `<a href contains 'tab=Active' AND текст ровно 'Active']` |
| `ACTIVE_HEADING` | `<h1>` или `<h2>`, в тексте которого встречается `active` (case-insensitive) |

### 4.7. Приём case-insensitive в XPath 1.0

XPath 1.0 (на котором работает Selenium) не поддерживает `lower-case()`. Вместо этого используется идиома:

```xpath
translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')
```

Преобразует все латинские заглавные в строчные. Такой приём встречается в `COOKIE_BUTTON`, `TAG_SEARCH_INPUT`, `ACTIVE_HEADING`.

---

## 5. Основной тестовый класс — `StackOverflowFunctionalTest.java`

### 5.1. Конфигурация класса

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class StackOverflowFunctionalTest { ... }
```

- `Lifecycle.PER_CLASS` — один экземпляр класса на все тесты, что позволяет использовать нестатический `@BeforeAll` и держать один WebDriver на всю сюиту (быстрее, меньше anti-bot триггеров)
- `OrderAnnotation` + `@Order(N)` — фиксирует порядок тестов; UC-01 идёт первым, чтобы прогреть драйвер на самой простой странице

Константы:

| Константа | Значение | Источник |
|---|---|---|
| `BASE_URL` | `https://stackoverflow.com` | Хардкод |
| `TIMEOUT_SECONDS` | `20` | `Long.getLong("selenium.timeout.seconds", 20L)` |
| `MANUAL_VERIFICATION_WAIT_SECONDS` | `180` | `Long.getLong("verification.wait.seconds", 180L)` |

### 5.2. Жизненный цикл

```java
@BeforeAll void setUp()
@AfterAll  void tearDown()
```

`setUp()`:
- Создаёт `WebDriver` через `createDriver()`
- `pageLoadTimeout = 60s`
- `implicitlyWait = 0` — отключает неявные ожидания, чтобы не накладываться на explicit `WebDriverWait` (это рекомендация Selenium при использовании `WebDriverWait`)
- Инициализирует `wait = new WebDriverWait(driver, TIMEOUT_SECONDS)`

`tearDown()` — `driver.quit()` с null-проверкой.

### 5.3. Фабрика драйвера — `createDriver()`

Читает свойства:

```java
String browser = System.getProperty("browser", "chrome").toLowerCase(Locale.ROOT);
boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
```

**Ветка Firefox:**
- `PageLoadStrategy.EAGER` — `document.readyState=interactive` (не ждём фоновых ресурсов)
- `intl.accept_languages = en-US` — стабильные английские тексты для проверок
- `-headless` если `headless=true`

**Ветка Chrome:**
- Опциональный `chrome.debuggerAddress` — если задан, драйвер цепляется к уже запущенному Chrome через CDP (см. раздел 7 — workaround Cloudflare). При этом остальные опции игнорируются
- Иначе:
  - `--window-size=1440,1000`
  - `--disable-notifications`
  - `--disable-blink-features=AutomationControlled` — скрывает признак автоматизации в `navigator.webdriver`
  - `--lang=en-US`
  - `excludeSwitches: ["enable-automation"]` — убирает «Chrome is being controlled by automated test software»
  - `--headless=new` — современный headless-режим Chrome 109+

**Любой другой `browser`** → `IllegalArgumentException`.

### 5.4. Тестовые методы (UC-01..UC-06)

| Метод | `@Order` | UC | Что делает |
|---|---:|---|---|
| `newestQuestionsPageShowsQuestionListAndNavigation` | 1 | UC-01 | Открывает `/questions`. Ждёт `<h1>`, ждёт текст «Newest Questions». Проверяет: список вопросов не пуст, кнопка `Ask Question` присутствует, ссылка `Tags` присутствует |
| `visitorSearchesQuestionsByTextQuery` | 2 | UC-02 | Вводит `selenium webdriver` в `MAIN_SEARCH`, отправляет ENTER. Если URL не сменился на `/search` за 6с — fallback: `driver.get(BASE_URL + "/search?q=selenium+webdriver")`. Ждёт результатов или сообщения «Search Results»/«No results found» |
| `visitorOpensPopularTagPage` | 3 | UC-04 | Открывает `/tags`, ждёт «Tags», кликает по `PYTHON_TAG_LINK`, проверяет что URL содержит `python` |
| `visitorSwitchesQuestionsToActiveSorting` | 4 | UC-05 | Открывает `/questions`, кликает `ACTIVE_TAB`, ждёт `tab=active` в URL или `ACTIVE_HEADING` |
| `anonymousVisitorIsRedirectedToLoginWhenAskingQuestion` | 5 | UC-06 | Открывает `/questions`, кликает `Ask Question`. Если URL за 4с не поменялся — fallback: `driver.get(BASE_URL + "/questions/ask")`. Проверяет редирект на `/users/login` или появление `LOGIN_REQUIRED_MESSAGE` |
| `visitorOpensQuestionAndSeesContent` | 6 | UC-03 | Открывает `/questions`, запоминает текст первой ссылки, кликает по ней, ждёт URL `/questions/`, проверяет наличие заголовка/тела/тегов и совпадение `getTitle()` страницы с обрезанным до 12 символов запомненным текстом |

#### 5.4.1. Нюансы отдельных тестов

**UC-02 (поиск):** двойная защита — сначала пытается отправить форму через ENTER, при провале лезет напрямую через URL. Утверждение тоже двойное: либо есть результаты-ссылки, либо в `<body>` есть строка «Search Results»/«No results found». Это устойчиво к тому, что Stack Overflow иногда показывает пустую выдачу с сообщением.

**UC-03 (открытие вопроса):**
```java
assertTrue(driver.getTitle().toLowerCase().contains(
    questionTitle.substring(0, Math.min(12, questionTitle.length())).toLowerCase()));
```
Сравнивается не весь заголовок, а первые 12 символов — Stack Overflow в `<title>` приписывает «- Stack Overflow» и иногда обрезает длинные названия, поэтому сравнение по префиксу надёжнее.

**UC-06 (Ask Question):** успехом считается *любой* из двух исходов: редирект на `/users/login` *или* появление сообщения «You must be logged in». Stack Overflow ведёт себя по-разному в зависимости от региона/AB-теста.

### 5.5. Вспомогательные методы

| Метод | Что делает |
|---|---|
| `open(path)` | `driver.get(BASE_URL + path)` → `waitForDocumentReady()` → `acceptCookiesIfShown()` → `waitUntilInterfaceIsAvailable()` |
| `waitForVisible(By)` | `ExpectedConditions.visibilityOfElementLocated`. При `TimeoutException` проверяет anti-bot: если он — ждёт прохождения и пробует снова; иначе выбрасывает `AssertionError` с диагностикой через `pageSnapshot()` |
| `waitForPresent(By)` | То же, но через `presenceOfElementLocated` (элемент в DOM, может быть невидим) |
| `isPresent(By)` | `!driver.findElements(By).isEmpty()` — мгновенная проверка без ожидания |
| `waitForDocumentReady()` | JS: `document.readyState` ∈ {`complete`, `interactive`}. При таймауте — если `<body>` уже содержит текст, считаем успехом |
| `acceptCookiesIfShown()` | Перебирает `COOKIE_BUTTON`, кликает первую видимую и enabled |
| `waitUntilInterfaceIsAvailable()` | Если страница похожа на anti-bot — печатает в stdout инструкцию для оператора и ждёт до `MANUAL_VERIFICATION_WAIT_SECONDS` пока проверка не уйдёт; затем повторно `waitForDocumentReady` + `acceptCookiesIfShown` |
| `isAntiBotVerificationPage()` | Эвристика: ищет в `body.text` подстроки «human verification», «are you a human», «verifying you are human», «security service to protect against malicious bots»; в `<title>` — «just a moment» (Cloudflare). Завернуто в try/catch на `WebDriverException` |
| `waitForBodyText(text)` | `wait.until(d -> bodyText().contains(text))` |
| `waitForUrlChangeOrLogin()` | Короткий 4-секундный wait: URL содержит `/users/login`, либо появилось `LOGIN_REQUIRED_MESSAGE`, либо ушли с `/questions` |
| `waitForSearchNavigation()` | Короткий 6-секундный wait: URL содержит `/search` |
| `clickAskQuestion()` | Кликает `ASK_QUESTION` обычным способом; при `WebDriverException` (overlay перехватил клик) — fallback через JavaScript `arguments[0].click()` |
| `bodyText()` | `driver.findElement(By.xpath("//body")).getText()` |
| `pageSnapshot()` | Диагностика для AssertionError: URL + title + первые 500 символов `bodyText` (со схлопнутыми пробелами) |

---

## 6. Шаблон Selenium IDE — `selenium-ide/stackoverflow-public-scenarios.side`

JSON формата `version: "2.0"`, открывается расширением Selenium IDE через «Open an existing project».

**Структура файла:**

```
{
  "id": "stackoverflow-lab-2422",
  "name": "Stack Overflow public scenarios 2422",
  "url": "http://stackoverflow.com",
  "tests":  [ ... 6 элементов ... ],
  "suites": [ { id: "suite-public", tests: [...], timeout: 300, parallel: false } ],
  "urls":   [ "http://stackoverflow.com/" ],
  "plugins": []
}
```

Каждый тест — массив `commands` вида `{id, command, target, value}`. Сценарии полностью повторяют UC-01..UC-06 из Java-тестов с теми же XPath-локаторами.

**Соответствие команд Selenium IDE и действий WebDriver:**

| IDE-команда | WebDriver-эквивалент в `StackOverflowFunctionalTest` |
|---|---|
| `open` | `driver.get(BASE_URL + target)` (внутри `open(path)`) |
| `click` | `WebElement.click()` |
| `type` | `element.clear(); element.sendKeys(value)` |
| `sendKeys` (`${KEY_ENTER}`) | `element.sendKeys(Keys.ENTER)` |
| `waitForElementPresent` | `wait.until(presenceOfElementLocated(...))` (`waitForPresent`) |
| `assertElementPresent` | `assertTrue(isPresent(...))` либо `assertFalse(findElements(...).isEmpty())` |

`suite-public` объединяет все шесть тестов с `timeout=300` и `parallel=false` — последовательный прогон с ограничением 5 минут на тест.

---

## 7. Обработка anti-bot верификации

Самая нетривиальная часть проекта. Stack Overflow находится за Cloudflare и периодически показывает интерстишал «Verifying you are human» — особенно в headless-режиме и при частых запусках.

### 7.1. Детектор

`isAntiBotVerificationPage()` — эвристика по содержимому страницы. Возвращает `true` если в `<body>` или `<title>` встречаются характерные строки:
- `human verification`
- `are you a human`
- `verifying you are human`
- `security service to protect against malicious bots`
- `just a moment` (заголовок Cloudflare)

### 7.2. Стратегия обработки

Каждая точка входа на страницу — `open(path)` — после `driver.get(...)` зовёт `waitUntilInterfaceIsAvailable()`. Каждый `waitForVisible/Present` при `TimeoutException` тоже проверяет — не из-за anti-bot ли это.

`waitUntilInterfaceIsAvailable()`:
1. Если страница не похожа на anti-bot — мгновенный return
2. Иначе печатает в stdout: «Stack Overflow opened an anti-bot verification page. Complete it manually in the browser window; waiting up to N seconds»
3. Ждёт до `MANUAL_VERIFICATION_WAIT_SECONDS` (по умолчанию 180с), пока эвристика не вернёт `false`
4. Если оператор не успел — `AssertionError` с диагностикой
5. Если успел — повторно `waitForDocumentReady` + `acceptCookiesIfShown` и продолжает тест

### 7.3. Режимы запуска для прохождения проверки

| Сценарий | Команда |
|---|---|
| Видимый браузер, ручное прохождение проверки | `mvn test -Dbrowser=firefox -Dheadless=false` |
| Увеличенное окно ожидания | `mvn test -Dheadless=false -Dverification.wait.seconds=300` |
| Подключение к уже залогиненному Chrome | `mvn test -Dbrowser=chrome -Dheadless=false -Dchrome.debuggerAddress=127.0.0.1:9222` |

Полная инструкция (включая запуск Chrome с `--remote-debugging-port`) — в [`test-coverage.md`](test-coverage.md#запуск-webdriver-тестов).

### 7.4. Почему это важно

По требованиям лабы (см. `test-results.md`) anti-bot страница не должна приводить к `skipped`-тестам — тест либо проходит, либо падает с осмысленным сообщением. Реализованная схема выполняет это требование: при недоступности интерфейса тест падает с `AssertionError`, а не пропускается.

---

## 8. Запуск тестов — кратко

```bash
# Chrome headless (по умолчанию)
mvn test

# Firefox с UI
mvn test -Dbrowser=firefox -Dheadless=false

# Сгенерировать и открыть Allure-отчёт
mvn allure:serve
```

Полный список вариантов запуска и обходные пути для Cloudflare — в [`test-coverage.md`](test-coverage.md#запуск-webdriver-тестов).

---

## 9. Соответствие требованиям лабы

| Требование | Где реализовано в коде |
|---|---|
| Use-case'ы покрыты автотестами | `StackOverflowFunctionalTest.java` — 6 методов с `@Order(1..6)` и `@DisplayName("UC-XX: ...")` |
| Selenium-автоматизация | `pom.xml` (`selenium-java 4.19.1`) + `createDriver()` |
| Selenium IDE шаблон | `selenium-ide/stackoverflow-public-scenarios.side` |
| Поддержка Firefox и Chrome | `createDriver()` — ветки `firefox` / `chrome`, выбор через `-Dbrowser=…` |
| Локаторы не основаны на ID | `StackOverflowLocators.java` — только `By.xpath(...)`, ни одного `By.id` или CSS `#id` |
| Anti-bot verification не приводит к skipped | `waitUntilInterfaceIsAvailable()` — ждёт до 180с и падает с `AssertionError`, если не дождался |
| Сценарии для зарегистрированного пользователя | `StackOverflowAuthenticatedTest.java` — UC-01a..UC-06a (см. раздел 10) |
| Сценарий взаимодействия двух пользователей | `StackOverflowTwoUserScenarioTest.java` — UC-X (см. раздел 10) |

---

## 10. Авторизованные тесты и сценарий двух пользователей

Дополнительный слой тестов, не требующий правки анонимной сюиты. Активируется через системные свойства; без них новые тесты помечаются как **skipped**, существующие 6 анонимных тестов работают как раньше.

### 10.1. Как решена аутентификация

Авторизация в SO осложнена двумя вещами: Cloudflare-challenge в headless-режиме и периодической reCAPTCHA после подозрительной активности. Поэтому поддерживаются **три способа**, тест выбирает первый доступный (в порядке предпочтения):

| Способ | Системное свойство | Реализация |
|---|---|---|
| **1. Cookies из ручной сессии** (рекомендуется) | `-Dso.userN.cookies=<path>` | `StackOverflowAuthSession.loadCookies(driver, path)` — открывает `BASE_URL`, чистит cookies, парсит JSON через Gson, для каждой записи строит `Cookie` и `addCookie`, делает refresh |
| **2. Авто-логин email/password** | `-Dso.userN.email=...` + `-Dso.userN.password=...` | `StackOverflowAuthSession.loginWithCredentials(driver, email, password, timeout)` — открывает `/users/login`, заполняет `LOGIN_FORM_EMAIL` и `LOGIN_FORM_PASSWORD` (XPath-локаторы, без `By.id`), кликает `LOGIN_FORM_SUBMIT`, ждёт ухода с `/users/login` или появления `USER_AVATAR`. Подтверждено через Playwright: на `/users/login` нет reCAPTCHA при первом заходе |
| **3. Подключение к запущенному Chrome** | `-Dchrome.debuggerAddress=127.0.0.1:9222` | Не требует ни паролей, ни cookies в коде. Пользователь запускает Chrome с remote-debugging, логинится руками, тест присоединяется через CDP. Минус — несовместимо с UC-X (для двух пользователей нужны два независимых браузера) |

Все три способа проверяются финальным `assumeTrue(isAuthenticated(driver))` — если ни один не привёл к залогиненной сессии, сюита skipped, а не failed.

Подробная инструкция для оператора — в [`auth-setup.md`](auth-setup.md).

### 10.2. `StackOverflowAuthSession.java` — utility загрузки cookies

Финальный класс с приватным конструктором, четыре статических метода:

| Метод | Назначение |
|---|---|
| `cookiesPath(userKey)` | Читает системное свойство `so.{userKey}.cookies`. Возвращает `null` если не задано или значение — нераскрытый Maven-плейсхолдер `${...}`. Используется в `assumeTrue` |
| `loadCookies(driver, path)` | Парсит JSON через Gson (поддерживаются форматы `[...]` и `{"cookies":[...]}`), строит `Cookie` для каждой записи, добавляет в driver, обновляет страницу |
| `isAuthenticated(driver)` | Проверяет наличие `USER_AVATAR` и отсутствие `LOGIN_LINK` |
| `buildCookie(entry)` | Внутренний: обрабатывает поля разных форматов (`expirationDate` для Cookie-Editor, `expiry` для Chrome DevTools, `expires` для legacy), `sameSite` приводит к Selenium-формату |

Парсинг JSON — через `com.google.code.gson:gson:2.10.1` (test-scoped, добавлен в `pom.xml`).

### 10.3. Новые локаторы в `StackOverflowLocators.java`

Все только XPath. Локаторы выверены вручную через Playwright MCP (открыл SO в реальном Chrome, прогнал каждый XPath через `document.evaluate`, проверил что выбирается нужный узел).

| Группа | Локаторы | Где используется |
|---|---|---|
| Маркеры авторизации | `USER_AVATAR`, `LOGIN_LINK`, `REPUTATION_BADGE` | UC-01a..UC-06a — проверка что аватар виден / Log in нет |
| Login-форма (для авто-логина) | `LOGIN_FORM_EMAIL`, `LOGIN_FORM_PASSWORD`, `LOGIN_FORM_SUBMIT` | `StackOverflowAuthSession.loginWithCredentials` |
| Форма Ask Question | `ASK_TITLE_INPUT`, `ASK_BODY_BACKING_TEXTAREA`, `ASK_BODY_EDITOR_TEXTBOX`, `ASK_TAGS_INPUT`, `POST_QUESTION_BUTTON` | UC-06a (проверка появления формы), UC-X (заполнение) |
| Голосование (только для авторизованных) | `VOTE_UP_BUTTON` | UC-03a — проверка что кнопки видны |
| Форма ответа | `ANSWER_FORM_CONTAINER`, `ANSWER_BACKING_TEXTAREA`, `ANSWER_EDITOR_TEXTBOX`, `POST_ANSWER_BUTTON` | UC-X (user2 пишет ответ) |
| Блоки ответов | `ANSWER_BLOCK`, `ANSWER_BODY_TEXT` | UC-X (проверка что ответ появился) |
| Удаление | `QUESTION_DELETE_LINK`, `ANSWER_DELETE_LINK`, `DELETE_CONFIRM_BUTTON`, `DELETED_BADGE` | UC-X (cleanup) |

**Что выяснилось при ручной проверке через Playwright:**

| Находка | Реакция в коде |
|---|---|
| `//h1[normalize-space()]` ловит мусорные `<h1 class="s-modal--header">` модальных окон | `QUESTION_TITLE` ужесточён до `//h1[@itemprop='name'] \| //h1[contains(@class,'fs-headline1')] \| ...` |
| Action формы ответа — `/questions/{id}/answer/submit`, а не `/post/answer/` как я предполагал | `ANSWER_FORM_CONTAINER` ищет по `@id='post-form'` или `contains(@action,'/answer/submit')` |
| Stacks Editor — современный contenteditable rich-text — рендерится **лениво** только после клика по форме. Backing textarea `[name='post-text']` всегда есть в DOM на странице ответа, но `d-none` (Selenium не сможет sendKeys в неё) | Два разных локатора: `*_BACKING_TEXTAREA` для проверки присутствия / JS-fallback и `*_EDITOR_TEXTBOX` (`[role='textbox']`) для нормального sendKeys |
| reCAPTCHA на `/users/login` **отсутствует** при первом заходе | Добавлен альтернативный способ auth — `loginWithCredentials(driver, email, password)` |
| Анонимные не-httpOnly cookies: только `OptanonConsent`, `OTGPPConsent`, `g_state`, `notice-ssb` (никакой сессии) | В `auth-setup.md` указаны cookies сессии: `acct`, `prov` и др. — они `httpOnly`, экспортируются через Cookie-Editor |
| `USER_AVATAR` — `<a href*='/users/'>` в `<header>` с `img.gravatar-*` или `s-user-card` внутри | Локатор уточнён `//header//a[contains(@href,'/users/') and (.//img[contains(@class,'gravatar')...] or contains(@class,'s-user-card'))]` |
| **Новые аккаунты получают Ask Wizard** (svelte SPA) вместо классической формы. Title input — это `<input label="Title" data-min-length="15" data-max-length="150" id="post-title-input">`, без `name=post-title`. Body — только ProseMirror, backing textarea отсутствует. Submit-кнопка — `Submit to Staging Ground` | `ASK_TITLE_INPUT` расширен `@label='Title'` / `data-min-length+data-max-length`; `fillStacksEditor` сделан tolerant к отсутствию backing textarea; `POST_QUESTION_BUTTON` ловит и `Submit to Staging Ground` |
| **Staging Ground** — песочница для review-вопросов от новичков. Чтобы опубликовать сразу на основной SO, нужно выбрать radio `Post question on Stack Overflow now` (deployment=stackOverflowOptIn) | Новый локатор `DEPLOY_TO_STACKOVERFLOW_RADIO`; в UC-X перед submit кликаем этот label, иначе вопрос уйдёт в песочницу и user2 не сможет его найти |
| Tags input — `<input id="filterInput" aria-label="tags" placeholder="e.g. (wordpress database spring)">` — aria-label в нижнем регистре | `ASK_TAGS_INPUT` использует `translate(...,'ABC...','abc...')` для case-insensitive поиска |

### 10.4. Заполнение Stacks Editor

Stack Overflow перешёл на rich-text редактор на базе ProseMirror (`js-stacks-editor`). Это создаёт две сложности:

1. **Backing textarea скрыта** (`d-none`) — обычный `WebElement.sendKeys` в неё не сработает
2. **Реальный `[role="textbox"]` div рендерится только после фокуса/клика по форме** — на свежезагруженной странице его нет

В `StackOverflowTwoUserScenarioTest.fillStacksEditor`:

```java
private void fillStacksEditor(WebDriver driver, WebDriverWait wait, By backingTextarea, By editorTextbox, String text) {
    waitForPresent(driver, wait, backingTextarea);     // 1. backing должен быть в DOM
    try {
        WebElement textbox = wait.until(visibilityOfElementLocated(editorTextbox));  // 2. ждём contenteditable
        textbox.click();                               // 3. focus
        textbox.sendKeys(text);                        // 4. печатаем — Stacks Editor синкает в backing
        return;
    } catch (TimeoutException ignored) {
    }
    // 5. fallback — пишем напрямую в backing через JS + dispatch input/change
    WebElement backing = driver.findElement(backingTextarea);
    ((JavascriptExecutor) driver).executeScript(
            "const ta = arguments[0]; ta.value = arguments[1];"
            + " ta.dispatchEvent(new Event('input', { bubbles: true }));"
            + " ta.dispatchEvent(new Event('change', { bubbles: true }));",
            backing, text);
}
```

JS-fallback нужен на случай, если редактор не успел инициализироваться или у SO изменился markup. Dispatching `input`/`change` event'ов заставляет Svelte/Stimulus controllers SO пересчитать состояние формы (без них кнопка Post может остаться disabled).

### 10.4. `StackOverflowAuthenticatedTest.java` — UC-01a..UC-06a

Структурно зеркало `StackOverflowFunctionalTest`, но с тремя отличиями:

1. **`@BeforeAll`** дополнительно:
   - `assumeTrue(cookiesPath != null, ...)` — skip всей сюиты, если cookies не подключены
   - `StackOverflowAuthSession.loadCookies(driver, path)` — подгружает сессию
   - `assumeTrue(isAuthenticated(driver), "Cookies invalid/expired")` — skip если cookies протухли

2. **В каждом UC** добавлена проверка `assertTrue(isPresent(USER_AVATAR))` — подтверждает, что мы действительно работаем под логином, а не свалились в анонимный режим.

3. **UC-06a фундаментально другой**, чем UC-06: вместо ожидания редиректа на `/users/login` тест ждёт появления `ASK_TITLE_INPUT` (форма создания вопроса доступна авторизованному). Это критическая поведенческая разница между анонимным и авторизованным режимом.

UC-03a дополнительно проверяет `VOTE_UP_BUTTON` — кнопки голосования отображаются только для авторизованных пользователей.

### 10.5. `StackOverflowTwoUserScenarioTest.java` — UC-X

Один `@Test`-метод с явным `try/finally` для гарантированной очистки. Множественные методы не подходят: JUnit не гарантирует выполнения cleanup-теста, если предыдущий упал.

**Структура:**

```java
@TestInstance(PER_CLASS)
class StackOverflowTwoUserScenarioTest {
    private WebDriver user1Driver;
    private WebDriver user2Driver;
    private final String uniqueMarker = "tpo-2422-" + UUID.randomUUID().toString().substring(0, 8);
    private String questionUrl;

    @BeforeAll setUp()  // assumeTrue для обоих cookies → два driver → loadSession для каждого
    @AfterAll  tearDown() // quitQuietly для обоих

    @Test
    void twoUserQuestionAnswerDeleteScenario() {
        try {
            askQuestionAsUser1();   // → questionUrl
            answerQuestionAsUser2();
            verifyUser1SeesAnswer();
        } finally {
            cleanup();  // best-effort, ошибки логируются и НЕ пробрасываются
        }
    }
}
```

**Шаги сценария:**

| Шаг | Кто | Действие | Проверки |
|---|---|---|---|
| `askQuestionAsUser1` | user1 | Открывает `/questions/ask`, заполняет title (`tpo-2422-<uuid>: How to safely iterate over dict items in Python 3?`), body (~10 строк осмысленного кода + маркер `uniqueMarker`), tag `python`, нажимает Post | URL изменился на `/questions/\d+/...`; сохраняется `questionUrl` |
| `answerQuestionAsUser2` | user2 | Открывает `questionUrl` напрямую (не через поиск — поиск может не успеть проиндексировать), скроллит к форме ответа, заполняет body с тем же `uniqueMarker`, нажимает Post | На странице появляется `uniqueMarker` И блок `ANSWER_BLOCK` |
| `verifyUser1SeesAnswer` | user1 | Перезагружает `questionUrl` | Видит `uniqueMarker` и `ANSWER_BLOCK` — подтверждение того, что два пользователя действительно взаимодействуют через одну страницу |
| `cleanup` | оба | (1) user2 открывает страницу, кликает Delete на своём ответе, подтверждает; (2) user1 открывает страницу, кликает Delete на вопросе, подтверждает; (3) проверяется `DELETED_BADGE` либо редирект | Если Delete недоступен — `System.out.println("MANUAL CLEANUP NEEDED: " + questionUrl)`, тест НЕ помечается failed (cleanup — best-effort) |

**Уникальный маркер** (`tpo-2422-<uuid>`) включается и в заголовок вопроса, и в тело ответа. Это даёт детерминированный поиск (`bodyOf(driver).contains(uniqueMarker)`) для проверки «user1 видит ответ user2».

**Два WebDriver одновременно:** разные размеры окна (`1440x1000` для user1, `1280x900` для user2) и независимые наборы cookies. Selenium 4 спокойно держит несколько `ChromeDriver` инстансов параллельно — каждый получает свой `chromedriver` процесс.

### 10.6. Конфигурация запуска

Новые системные свойства, объявленные в `pom.xml` (с пустыми значениями по умолчанию, чтобы Maven не подставлял `${...}`-плейсхолдеры):

| Свойство | Назначение | Когда нужно |
|---|---|---|
| `so.user1.cookies` | Путь к JSON-файлу cookies первого пользователя | Для `StackOverflowAuthenticatedTest` и `StackOverflowTwoUserScenarioTest` |
| `so.user2.cookies` | Путь к JSON-файлу cookies второго пользователя | Только для `StackOverflowTwoUserScenarioTest` |

Без них сюиты skipped. С `so.user1.cookies` работают UC-01a..UC-06a. С обоими — также UC-X.

### 10.7. Безопасность и ограничения

| Аспект | Решение |
|---|---|
| Cookies — секрет | Папка `.so-cookies/`, маска `*.cookies.json`, `credentials.properties` добавлены в `.gitignore` |
| Cookies протухают | `Assumptions.assumeTrue(isAuthenticated)` даёт понятное сообщение и skip-статус |
| Спам на SO | Уникальный заголовок с маркером, осмысленное тело вопроса, моментальное удаление в `finally` |
| Delete недоступен | Best-effort cleanup: лог `MANUAL CLEANUP NEEDED: <url>`, тест не падает (assertions уже отработали в шагах 1-3) |
| Два аккаунта с одного IP | Документация в `auth-setup.md` рекомендует разные браузеры / VPN при создании аккаунтов |
