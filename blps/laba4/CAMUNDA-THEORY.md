# Как устроена работа с Camunda в проекте (от начала до конца)

Документ объясняет полный путь: как встроенный движок Camunda поднимается внутри
приложения, как BPMN-процессы попадают в движок, и что происходит на каждом шаге
от HTTP-запроса (или нажатия в Tasklist) до записи в БД и MinIO.

---

## 1. Что такое embedded-режим и из чего он собран

Camunda работает **встроенно (embedded)**: движок — это не отдельный сервер, а
библиотека внутри нашего Spring Boot WAR. Поднимается автоматически Spring-стартерами.

**Зависимости — `pom.xml`:**
- `camunda-bpm-spring-boot-starter` (v7.21.0) — сам движок + автоконфигурация Spring Boot.
- `camunda-bpm-spring-boot-starter-webapp` — веб-приложения **Cockpit** и **Tasklist**.
- `camunda-engine-plugin-spin` + `camunda-spin-dataformat-json-jackson` — работа с JSON-переменными.
- `narayana-spring-boot-starter` — менеджер распределённых (JTA/XA) транзакций.
- `spring-boot-starter-web`, `io.minio:minio` — REST и объектное хранилище.

**Точка входа — `src/main/java/org/example/LabaApplication.java`:**
Класс наследует `SpringBootServletInitializer` — это и делает приложение **WAR-ом**,
который разворачивается на WildFly (а не запускает встроенный Tomcat). Метод `configure()`
говорит серверу приложений, какой класс является корнем Spring-контекста.

---

## 2. Конфигурация движка — `src/main/resources/application.properties`

Camunda настраивается свойствами `camunda.bpm.*`:
```properties
camunda.bpm.admin-user.id=admin            # учётка администратора Cockpit/Tasklist
camunda.bpm.admin-user.password=admin
camunda.bpm.database.schema-update=true    # движок сам создаёт свои таблицы ACT_*
camunda.bpm.auto-deployment-enabled=true   # авто-деплой BPMN из classpath
camunda.bpm.job-execution.enabled=true     # фоновый исполнитель (async, таймеры)
camunda.bpm.history-level=full             # полная история процессов
camunda.bpm.generic-properties.properties.historyTimeToLive=P180D  # срок хранения истории
```
- `schema-update=true` — при старте движок создаёт в PostgreSQL свои служебные таблицы
  с префиксом `ACT_` (см. раздел 9).
- `historyTimeToLive=P180D` — обязателен в Camunda 7.21, иначе движок не стартует.

БД и MinIO здесь же: `spring.datasource.*` (+ XA-вариант для Narayana) и `minio.*`.

---

## 3. Инициализация пользователей/групп Camunda

**`src/main/java/org/example/config/CamundaIdentityConfig.java`:**
По событию `ApplicationReadyEvent` (после полного старта) через `IdentityService` создаёт:
- группы `moderator` и `user`,
- пользователей Camunda `admin` и `moderator`, добавляет их в группу `moderator`.

Это **внутренние учётки Camunda** (для входа в Cockpit/Tasklist и для механизма
`candidateGroups`). Они **не связаны** с пользователями приложения (таблица `users`).
Группа `moderator` нужна, чтобы задачи с `candidateGroups="moderator"` попадали модераторам.

---

## 4. Где лежат процессы и как они попадают в движок

**Папка `src/main/resources/processes/` — 16 файлов `*.bpmn`.**
Благодаря `auto-deployment-enabled=true` при старте приложения движок сканирует
classpath, находит все `.bpmn` и **разворачивает (deploy)** их как определения процессов.
Каждый деплой создаёт запись в `ACT_RE_DEPLOYMENT` / `ACT_RE_PROCDEF`. После этого
процессы видны в Cockpit и доступны для запуска по ключу (`id` процесса в BPMN).

При повторном деплое с изменённым BPMN создаётся **новая версия** определения
(в Cockpit виден растущий «Definition Version»).

---

## 5. Из чего состоит BPMN-процесс (на примере `video-upload.bpmn`)

- `<process id="video-upload-process">` — ключ, по которому процесс запускается.
- `<startEvent>` — старт. Может нести **стартовую форму** (`camunda:formData`) и
  **execution listener** (`<camunda:executionListener>`).
- `<serviceTask camunda:delegateExpression="${beanName}">` — автоматический шаг,
  вызывающий Java-бин (делегат). **Главная точка интеграции** движка с кодом.
- `<userTask camunda:formKey/​formData ...>` — шаг с участием человека: процесс
  **останавливается** и ждёт, пока задачу выполнят в Tasklist.
- `<exclusiveGateway>` — ветвление по условию (`${fileValid == true}`).
- `<sequenceFlow>` — стрелки между элементами; на ветвях шлюза несут `conditionExpression`.
- `<bpmndi:BPMNDiagram>` — координаты для отрисовки схемы в Cockpit/Modeler.

Переменные процесса (`execution.getVariable/setVariable`) — это «память» экземпляра,
хранится в `ACT_RU_VARIABLE`. Через них шаги обмениваются данными (`videoId`, `userId`,
`fileValid`, `jwtToken` и т.д.).

---

## 6. Два способа запустить процесс

### Сценарий A. Через REST (Insomnia)

1. Запрос приходит в **контроллер**, например
   `src/main/java/org/example/controller/VideoController.java` (`POST /api/videos/upload`).
2. Перед этим запрос проходит **слой безопасности**:
   `security/JwtAuthenticationFilter.java` достаёт JWT из заголовка,
   `security/JwtTokenProvider.java` валидирует его,
   `security/SecurityUtils.java` отдаёт текущего пользователя.
   Доступ к путям настроен в `config/SecurityConfig.java`.
3. Контроллер кладёт нужные данные в `Map<String,Object> variables` и вызывает
   **`runtimeService.startProcessInstanceByKey("video-upload-process", variables)`**
   — это и есть точка входа в движок (`RuntimeService`).
4. Движок создаёт экземпляр процесса и прогоняет его по схеме до первой остановки
   (User Task) или до конца.
5. Контроллер при необходимости читает результат через
   `runtimeService.getVariable(...)` и возвращает JSON.

### Сценарий B. Через Camunda Tasklist (UI)

1. В Tasklist пользователь жмёт **Start process**, выбирает процесс.
2. Tasklist рендерит **сгенерированную форму** из `camunda:formData` стартового события
   (поля `jwtToken`, `videoId` и т.д.). Это и есть «генератор форм Camunda».
3. После Submit движок стартует процесс, положив значения полей в переменные.
4. На стартовом событии срабатывает **execution listener**
   `delegate/auth/ResolveJwtUserDelegate.java`: берёт переменную `jwtToken`,
   валидирует её (`JwtTokenProvider`), находит пользователя (`UserRepository`) и
   проставляет `userId`/`username`/`uploaderId`/`assignee`. Так процесс «узнаёт»,
   кто его запустил, без HTTP-контекста Spring Security.

> Для загрузки видео из Tasklist файл добавляется переменной `videoFile` (тип **File**)
> через generic-форму — у генерируемых форм Camunda нет типа File.

---

## 7. Service Task → Java-делегат (как выполняется автоматический шаг)

Когда исполнение доходит до `<serviceTask camunda:delegateExpression="${validateVideoFileDelegate}">`,
движок ищет Spring-бин с таким именем и вызывает его метод `execute(DelegateExecution)`.

**Делегаты лежат в `src/main/java/org/example/delegate/`** по подпапкам:
- `upload/` — загрузка видео: `ValidateVideoFileDelegate` (проверка + загрузка файла в MinIO
  при запуске из Tasklist), `CreateVideoRecordDelegate` (запись в БД),
  `ValidateDescriptionDelegate`, `SaveVideoAsDraftDelegate`, `CleanupInvalidFileDelegate`.
- `publish/` — публикация: `SetPendingPublicationDelegate`, `AutoModeratePublishDelegate`
  (автомодерация описания), `ChangeVideoStatusPublishedDelegate`,
  `SavePublicationParamsDelegate`, `PublishVideoFileDelegate` (копирование файла в MinIO),
  `RejectPublicationDelegate`.
- `monetization/` — `CheckMonetizationEligibilityDelegate`, `SaveMonetizationDelegate`.
- `method/` — способы монетизации (реклама/подписка): `CheckMonetizationApprovedDelegate`,
  `AutoModerateAdDelegate`, `SaveAdPendingReviewDelegate`, `SaveAdTagsDelegate`,
  `ApproveAdMethodDelegate`, `AddAdToDbDelegate`, `RejectAdMethodDelegate`,
  `SaveSubscriptionMethodDelegate`.
- `view/` — чтение данных для list/get-процессов: `LoadMyVideosDelegate`, `LoadDraftsDelegate`,
  `LoadVideoByIdDelegate`, `LoadPendingVideosDelegate` и т.д.
- `auth/` — `RegisterUserDelegate`, `LoginUserDelegate`, `ResolveJwtUserDelegate`.

**Что делает делегат:** читает переменные (`execution.getVariable`), вызывает
**бизнес-сервис** (`service/`) или **репозиторий** (`repository/`), пишет результат
обратно в переменные (`execution.setVariable`). Например `AutoModeratePublishDelegate`
вызывает `ValidationService.isDescriptionClean()` и кладёт `autoApproved`.

Делегаты помечены `@Transactional`, где нужно — изменения в PostgreSQL идут в общей
JTA-транзакции (Narayana), в которой участвует и сам движок (атомарность шага процесса).

---

## 8. User Task → остановка и форма

Дойдя до `<userTask>`, движок создаёт задачу в `ACT_RU_TASK` и **приостанавливает**
экземпляр. Задача:
- видна в Tasklist (фильтр по `assignee` или `candidateGroups="moderator"`);
- показывает форму из `camunda:formData` (поля заполняет человек);
- по нажатию **Complete** значения формы пишутся в переменные, и движок продолжает
  процесс с этого места.

Завершить задачу можно и программно: `TaskService.complete(taskId, vars)` —
так делают `ModerationController` (решение модератора) и `ProcessController`.

---

## 9. Где Camunda хранит состояние (таблицы `ACT_*`)

Движок ведёт собственную схему в той же БД `studs`:
- `ACT_RE_*` — определения процессов (задеплоенные BPMN).
- `ACT_RU_*` — runtime **работающих** процессов: `ACT_RU_EXECUTION` (экземпляры),
  `ACT_RU_TASK` (активные задачи), `ACT_RU_VARIABLE` (переменные),
  `ACT_RU_JOB` (async/таймеры), `ACT_RU_INCIDENT` (ошибки).
- `ACT_HI_*` — история завершённых процессов/задач/переменных.
- `ACT_ID_*` — пользователи/группы Camunda (из `CamundaIdentityConfig`).
- `ACT_GE_*` — общее (байты деплоя, версия схемы).

Когда процесс завершается, записи переезжают из `ACT_RU_*` в `ACT_HI_*`.

---

## 10. Развёртывание под WildFly

- **`src/main/webapp/WEB-INF/jboss-deployment-structure.xml`** — исключает подсистемы
  WildFly, конфликтующие со встроенной Camunda/Spring (`weld`, `jsf`, `jaxrs`,
  `batch-jberet`) и дублирующиеся модули (jackson, slf4j, hibernate).
- **`src/main/webapp/WEB-INF/jboss-web.xml`** — `context-root=/` (приложение в корне).
- WAR разворачивается на WildFly; Camunda-движок и Cockpit/Tasklist поднимаются внутри
  него на HTTP-порту WildFly.

---

## 11. Сквозной пример: «Загрузка видео» из Tasklist от и до

1. **Tasklist → Start process «Загрузка видео»**: пользователь добавляет переменные
   `videoFile` (File) и `jwtToken` (String), жмёт Start.
2. **Стартовое событие**: срабатывает `ResolveJwtUserDelegate` → из `jwtToken`
   получены `userId`, `uploaderId`, `assignee`.
3. **Service Task «Проверить видеофайл»** (`ValidateVideoFileDelegate`): т.к. `minioKey`
   ещё нет, делегат читает `videoFile`, грузит файл в MinIO (`MinioService.uploadVideoFile`),
   ставит `minioKey/fileName/fileSize`, затем проверяет метаданные (`ValidationService`)
   и пишет `fileValid`.
4. **Шлюз «Файл валиден?»**: если `false` → `CleanupInvalidFileDelegate` удаляет файл
   из MinIO → конец с ошибкой. Если `true` → дальше.
5. **Service Task «Создать запись видео»** (`CreateVideoRecordDelegate`): создаёт строку
   в таблице `videos` (`VideoRepository`) со статусом `UPLOADING`, кладёт `videoId`.
6. **User Task «Заполнить информацию о видео»**: процесс **останавливается**.
   В Tasklist открывается сгенерированная форма (поля `title`, `description`, `tags`).
   Пользователь заполняет → Complete.
7. **Service Task «Проверить описание»** (`ValidateDescriptionDelegate`) →
   **шлюз «Описание валидно?»**: невалидно → возврат к заполнению (цикл); валидно → дальше.
8. **Service Task «Сохранить как черновик»** (`SaveVideoAsDraftDelegate`): обновляет
   запись видео (title/description/tags, статус DRAFT) → **конец процесса**.
9. Экземпляр завершён: его данные уходят в `ACT_HI_*`, видео лежит в MinIO
   (бакет `uploaded-videos`) и в таблице `videos`.

---

## 12. Карта файлов

| Слой | Файлы | Роль |
|---|---|---|
| Точка входа | `LabaApplication.java` | WAR-инициализатор Spring Boot |
| Конфиг движка | `application.properties` (`camunda.bpm.*`) | настройка Camunda, БД, MinIO |
| Идентичность Camunda | `config/CamundaIdentityConfig.java` | пользователи/группы Camunda |
| Безопасность | `security/JwtAuthenticationFilter`, `JwtTokenProvider`, `SecurityUtils`, `config/SecurityConfig` | JWT, доступ |
| Определения процессов | `resources/processes/*.bpmn` (16 шт.) | BPMN-схемы |
| Запуск процессов (REST) | `controller/AuthController`, `VideoController`, `MonetizationController`, `UserController`, `ModerationController` | `RuntimeService.startProcessInstanceByKey`, `TaskService.complete` |
| Управление задачами (REST) | `controller/ProcessController` | список/завершение задач, переменные |
| Service Task | `delegate/upload/**`, `publish/**`, `monetization/**`, `method/**`, `view/**`, `auth/**` | автоматические шаги |
| Execution Listener | `delegate/auth/ResolveJwtUserDelegate` | разбор JWT внутри процесса |
| Бизнес-логика | `service/AuthService`, `VideoService`, `MonetizationService`, `UserService`, `ValidationService`, `MinioService` | доменные операции |
| Персистентность | `repository/*Repository`, `entity/*` | JPA → PostgreSQL |
| Деплой WildFly | `webapp/WEB-INF/jboss-deployment-structure.xml`, `jboss-web.xml` | совместимость с сервером приложений |

---

## 13. Главное в одном абзаце

Spring Boot WAR поднимает встроенный движок Camunda, который при старте сам деплоит
16 BPMN-процессов из `resources/processes/` и создаёт свои таблицы `ACT_*` в PostgreSQL.
Процесс запускается двумя путями: через REST-контроллер
(`RuntimeService.startProcessInstanceByKey`) или из Tasklist по сгенерированной форме.
Внутри процесса автоматические шаги (Service Task) через `delegateExpression` вызывают
Java-делегаты, которые обращаются к бизнес-сервисам, MinIO и БД; шаги с участием человека
(User Task) останавливают процесс и ждут заполнения формы в Tasklist. Идентификация
пользователя внутри процесса выполняется по JWT (`ResolveJwtUserDelegate`), а атомарность
шагов обеспечивает JTA-транзакция Narayana.
