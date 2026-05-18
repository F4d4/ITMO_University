# Подготовка авторизации для тестов

Авторизованные сценарии (`StackOverflowAuthenticatedTest`, `StackOverflowTwoUserScenarioTest`) поддерживают **три способа** получить залогиненную сессию. Выбор зависит от того, насколько надёжность важнее простоты.

Проверено через Playwright (май 2026): на `/users/login` **нет reCAPTCHA** при первой попытке (поле email/password принимает обычный sendKeys), но Cloudflare периодически выдаёт challenge в headless-режиме. Cookies-подход обходит обе проблемы.

## Три способа авторизации

| Способ | Что задаётся | Когда подходит |
|---|---|---|
| **Полный авто-логин** (рекомендуется) | `-Dso.userN.email=...` и `-Dso.userN.password=...` | Тест **сам** запускает обычный Chrome через `ChromeLauncher` с `--remote-debugging-port` и персистентным `--user-data-dir`, подключается через CDP (Cloudflare пропускает) и сам логинится через `/users/login` (reCAPTCHA там нет). Никаких ручных шагов. После первого прогона Cloudflare запоминает профиль — следующие запуски летают |
| **Cookies из ручной сессии** | `-Dso.userN.cookies=<path>` | Один раз залогинился руками, экспортировал JSON, дальше прогоны на любом Chrome. Минус — cookies протухают через дни |
| **Подключение к ручному Chrome** | `-Dchrome.debuggerAddress=127.0.0.1:9222` (для одного пользователя) или `-Dchrome.debuggerAddress.userN=...` (для UC-X) | Полностью ручной: вы сами запускаете Chrome и логинитесь, тест только подключается. Полезно если нет паролей в коде вообще |

Запуск без любого из них → `Assumptions.assumeTrue` → сюита помечается **skipped** (не failed), сборка остаётся зелёной.

## Когда что нужно

| Команда | Что выполняется |
|---|---|
| `mvn test` | Только анонимные UC-01..UC-06; авторизованные пропущены |
| `mvn test -Dso.user1.cookies=...` (или email+password, или debuggerAddress) | + UC-01a..UC-06a |
| `mvn test -Dso.user1.cookies=... -Dso.user2.cookies=...` (или соответствующие email+password) | + UC-X (сценарий двух пользователей) |

## Шаг 1. Создать два тестовых аккаунта на stackoverflow.com

1. Зарегистрируйте два аккаунта: `user1@…`, `user2@…`. Желательно в **разных** браузерах или с разных IP-адресов — два аккаунта с одного IP помечаются «possible multiple accounts».
2. После регистрации обязательно подтвердите email.
3. **Важно про Ask Wizard**: свежие аккаунты получают новую upgrade-форму создания вопроса, в которой по умолчанию вопрос отправляется в **Staging Ground** (песочница для review). Чтобы вопрос попал на основной SO, тест UC-X явно выбирает radio «Post question on Stack Overflow now» перед submit. Это требование значит, что новый вопрос пойдёт через автомодерацию SO и может быть закрыт как low-quality — заголовок и тело в `StackOverflowTwoUserScenarioTest` сделаны реалистичными, чтобы избежать этого.
4. Чтобы аккаунт не висел в review queue при первом постинге, можно «прогреть» его: проголосовать за чужой ответ, добавить избранное, заполнить профиль. Без этого первый вопрос может остаться в hold-у.
5. Запомните, что Delete своего поста работает только пока на пост нет **внешних** upvote-ов. Тесты проводите быстро.

## Шаг 2. Получить cookies

Самый простой путь — расширение `Cookie-Editor` (есть для Chrome и Firefox). Альтернатива — DevTools → Application → Cookies (для каждого cookie создаётся объект руками, дольше).

1. Откройте Chrome, зайдите под user1, пройдите все Cloudflare и email-верификации.
2. Перейдите на `https://stackoverflow.com/questions` — убедитесь, что в верхнем правом углу виден ваш аватар.
3. Клик по иконке `Cookie-Editor` → `Export` → `Export as JSON`.
4. Сохраните файл как `.so-cookies/user1.json` (директория `.so-cookies/` добавлена в `.gitignore`, в репозиторий не попадёт).
5. Повторите для user2 → `.so-cookies/user2.json`.

### Формат файла

Принимается JSON-массив объектов:

```json
[
  {
    "domain": ".stackoverflow.com",
    "expirationDate": 1761000000.0,
    "httpOnly": true,
    "name": "prov",
    "path": "/",
    "secure": true,
    "value": "..."
  },
  {
    "domain": ".stackoverflow.com",
    "name": "acct",
    "path": "/",
    "value": "..."
  }
]
```

Поля:

| Поле | Обязательно | Заметки |
|---|---|---|
| `name` | да | Имя cookie |
| `value` | да | Значение |
| `domain` | желательно | `.stackoverflow.com` (с точкой) |
| `path` | нет | По умолчанию `/` |
| `expirationDate` / `expiry` / `expires` | нет | Unix timestamp (в секундах). Если нет — session cookie |
| `secure`, `httpOnly` | нет | Булевы, по умолчанию `false` |
| `sameSite` | нет | `Strict`, `Lax`, `None`, либо `unspecified` (игнорируется) |

Также принимается формат `{"cookies": [...]}` (некоторые экспортёры заворачивают массив).

## Способ 1: полный авто-логин (рекомендуется)

Тест **сам** делает всю работу:

1. Запускает обычный Chrome через `ChromeLauncher` с `--remote-debugging-port=9222`, `--user-data-dir=~/.so-test-chrome/userN` (персистентный профиль).
2. Подключается через CDP — Cloudflare пропускает, потому что Chrome выглядит как ручной.
3. Открывает `https://stackoverflow.com`, проверяет авторизованность.
4. Если не залогинен — открывает `/users/login` и логинится через форму (reCAPTCHA нет).

Один пользователь:

```bash
mvn test -Dtest=StackOverflowAuthenticatedTest \
         "-Dso.user1.email=mihan201557@gmail.com" \
         "-Dso.user1.password=M20348722m" \
         -Dheadless=false
```

Два пользователя:

```bash
mvn test -Dtest=StackOverflowTwoUserScenarioTest \
         "-Dso.user1.email=mihan201557@gmail.com" \
         "-Dso.user1.password=M20348722m" \
         "-Dso.user2.email=lollypopoftheworld@gmail.com" \
         "-Dso.user2.password=test1234567890test" \
         -Dheadless=false
```

Два разных Chrome поднимутся автоматически (порты 9222 и 9223). Логи покажут какой порт занят и какой user-data-dir используется.

**Дополнительные системные свойства**:

| Свойство | По умолчанию | Описание |
|---|---|---|
| `chrome.launchPort` | `9222` (auth) | Порт remote-debugging для одного-пользовательского теста |
| `chrome.profileDir.userN` | `~/.so-test-chrome/userN` | Папка профиля Chrome для конкретного пользователя |
| `chrome.binary` | определяется по ОС | Путь к Chrome executable (если не в стандартном месте) |

**Почему через `ChromeLauncher`, а не через Selenium-managed Chrome?**

Cloudflare детектит автоматизированный Chrome даже с `excludeSwitches=enable-automation` — fingerprint всё равно отличается. Свежезапущенный Selenium-Chrome попадает в loop: «I'm not a robot» → пройти → снова. Обычный Chrome, поднятый через `ProcessBuilder`, в этом отношении неотличим от ручного запуска и проходит Cloudflare нормально.

**Безопасность паролей**: пароль в `mvn`-аргументах остаётся в shell history. Лучше — через `~/.m2/settings.xml`:

```xml
<profiles>
  <profile>
    <id>so-test</id>
    <properties>
      <so.user1.email>ya@example.com</so.user1.email>
      <so.user1.password>secret</so.user1.password>
    </properties>
  </profile>
</profiles>
```

```bash
mvn test -Pso-test -Dtest=StackOverflowAuthenticatedTest -Dheadless=false
```

## Альтернатива (рекомендуется при Cloudflare loop): подключение к запущенному Chrome

**Симптом проблемы**: Selenium открывает SO → Cloudflare показывает «I'm not a robot» → вы нажимаете → проверка снова появляется → бесконечный цикл. Это потому что Cloudflare детектит fingerprint Selenium-управляемого Chrome даже после прохождения CAPTCHA. Anti-detect-флаги (`excludeSwitches`, `disable-blink-features=AutomationControlled`) помогают частично, но не на 100%.

**Решение**: запустить **обычный** Chrome с remote-debugging-портом, пройти Cloudflare и залогиниться руками, затем подключить тест через CDP к этому уже-«подтверждённому» браузеру.

### Один пользователь — `StackOverflowAuthenticatedTest`

macOS / Linux:

```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --remote-debugging-port=9222 \
  --user-data-dir=/tmp/so-chrome-user1
```

Windows (PowerShell):

```powershell
$profile="$env:TEMP\selenium-stackoverflow-chrome-user1"
Start-Process "chrome.exe" -ArgumentList "--remote-debugging-port=9222","--user-data-dir=$profile"
```

В открывшемся Chrome:
1. Откройте `https://stackoverflow.com/questions` → пройдите Cloudflare один раз
2. Залогиньтесь под user1
3. Окно не закрывайте

В терминале с проектом:

```bash
mvn test -Dtest=StackOverflowAuthenticatedTest \
         -Dchrome.debuggerAddress=127.0.0.1:9222 \
         -Dheadless=false
```

Driver присоединится к уже залогиненной сессии. `so.user1.cookies` и credentials указывать не нужно.

### Firefox (альтернатива Chrome)

Firefox обычно проходит Cloudflare снисходительнее, чем Selenium-Chrome. Не нужен `ChromeLauncher` — Selenium-managed Firefox умеет работать с persistent профилем напрямую через `-profile <path>`.

**Шаг 1.** Создайте Firefox-профили для тестовых пользователей. Откройте `about:profiles` в обычном Firefox → «Create a New Profile» → задайте имя `so-user1`, аналогично `so-user2`. Запомните пути (показаны в строке `Root Directory`), например:
- macOS: `~/Library/Application Support/Firefox/Profiles/abc123.so-user1`
- Linux: `~/.mozilla/firefox/abc123.so-user1`

**Шаг 2.** Запустите Firefox с этим профилем (`about:profiles` → кнопка «Launch profile in new browser»), залогиньтесь под user1, пройдите Cloudflare, выйдите.

Повторите для user2.

**Шаг 3.** Запуск тестов:

```bash
# UC-01a..UC-06a
mvn test -Dtest=StackOverflowAuthenticatedTest \
         -Dbrowser=firefox \
         '-Dfirefox.profileDir.user1=/Users/myasnov/Library/Application Support/Firefox/Profiles/abc123.so-user1' \
         -Dheadless=false

# UC-X
mvn test -Dtest=StackOverflowTwoUserScenarioTest \
         -Dbrowser=firefox \
         '-Dfirefox.profileDir.user1=/Users/myasnov/Library/Application Support/Firefox/Profiles/abc123.so-user1' \
         '-Dfirefox.profileDir.user2=/Users/myasnov/Library/Application Support/Firefox/Profiles/def456.so-user2' \
         -Dheadless=false
```

Кавычки вокруг пути обязательны — `Application Support` содержит пробел.

**Особенности Firefox:**
- WebDriver запускает Firefox с указанным профилем напрямую — отдельный launcher не нужен
- `dom.webdriver.enabled=false` выставлен в options, чтобы скрыть основной WebDriver-флаг
- Firefox не имеет CDP — `applyStealth()` для него пропускается (внутренний `instanceof ChromiumDriver` check)
- На Firefox cookies session живут в профиле, поэтому ручной логин в `about:profiles` сохраняется между прогонами автоматически

### Два пользователя — `StackOverflowTwoUserScenarioTest`

Запустите **два** Chrome на разных портах с разными `--user-data-dir`. В **каждом** руками пройдите Cloudflare и залогиньтесь под соответствующим пользователем:

Терминал 1:
```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --remote-debugging-port=9222 \
  --user-data-dir=/tmp/so-chrome-user1
# залогиньтесь под user1
```

Терминал 2:
```bash
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --remote-debugging-port=9223 \
  --user-data-dir=/tmp/so-chrome-user2
# залогиньтесь под user2
```

Терминал 3 — запуск тестов:
```bash
mvn test -Dtest=StackOverflowTwoUserScenarioTest \
         -Dchrome.debuggerAddress.user1=127.0.0.1:9222 \
         -Dchrome.debuggerAddress.user2=127.0.0.1:9223 \
         -Dheadless=false
```

`-Dchrome.debuggerAddress.userN` — per-user свойство, поддерживается специально для UC-X.

## Шаг 3. Запустить тесты

```bash
# Только авторизованные сценарии user1
mvn test -Dtest=StackOverflowAuthenticatedTest \
         -Dso.user1.cookies=.so-cookies/user1.json \
         -Dheadless=false

# Сценарий двух пользователей
mvn test -Dtest=StackOverflowTwoUserScenarioTest \
         -Dso.user1.cookies=.so-cookies/user1.json \
         -Dso.user2.cookies=.so-cookies/user2.json \
         -Dheadless=false

# Полный прогон, включая анонимные
mvn test -Dso.user1.cookies=.so-cookies/user1.json \
         -Dso.user2.cookies=.so-cookies/user2.json \
         -Dheadless=false
```

`-Dheadless=false` для первой настройки очень рекомендую — будет видно, что cookies подцепились и аватар появился. После того как тесты стабилизировались, можно убрать.

## Если тесты падают

| Симптом | Причина | Лечение |
|---|---|---|
| `Cookies are invalid or expired` (skipped) | Cookies протухли (срок истёк или Stack Overflow инвалидировал сессию) | Повторите шаг 2 |
| Anti-bot verification page | Сработала Cloudflare-проверка | Запустите с `-Dheadless=false` и пройдите вручную; WebDriver ждёт до 180 секунд |
| `Question was not posted` | Аккаунт user1 в review queue, либо вопрос помечен low-quality | Прогрейте аккаунт (см. шаг 1), измените тело вопроса в `StackOverflowTwoUserScenarioTest` |
| `Delete link not found for ...` | Кнопки Delete нет (внешний upvote или ответ принят) | Удалите вручную: лог содержит `MANUAL CLEANUP NEEDED: <url>` |

## Безопасность

- **Никогда не коммитьте** содержимое `.so-cookies/` или `*.cookies.json` — cookies дают полный доступ к аккаунту.
- `.gitignore` уже содержит правила; перед коммитом проверьте `git status`.
- Если случайно опубликовали cookies — немедленно выйдите из всех сессий через `https://stackoverflow.com/users/logout` и сбросьте пароль.
