# User Service

Консольное CRUD-приложение: Java 21, Hibernate, PostgreSQL.

## Требования

- JDK 21;
- Maven 3.9+;
- Docker Desktop — для локальной БД приложения и для интеграционных тестов.

Проверить окружение можно так:

```powershell
java -version
mvn -version
docker info
```

## Быстрый старт: тесты

```powershell
# Unit-тесты. Docker не нужен.
mvn test

# Unit- и интеграционные тесты. Docker Desktop должен быть запущен.
mvn verify
```

## Тестирование

В проекте два независимых набора тестов.

| | Unit-тесты | Интеграционные тесты |
|---|---|---|
| Команда | `mvn test` | `mvn verify` |
| Docker | не нужен | обязателен |
| База данных | фейки и заглушки DAO | PostgreSQL 18 в Testcontainers |
| Maven-плагин | surefire | failsafe |
| Имена классов | `*Test` | `*IT` |
| JUnit-тег | без тега | `@Tag("integration")` |
| Отчёты | `target/surefire-reports/` | `target/failsafe-reports/` |

`mvn test` запускает только unit-тесты: surefire исключает группу `integration`, а классы `*IT` не подходят под его стандартные шаблоны. `mvn verify` сначала выполняет unit-тесты, затем failsafe запускает интеграционные.

### Unit-тесты

```powershell
mvn test
```

Что проверяется:

- `AppTest` — консольный слой `App` с подставным `UserService`;
- `InputValidatorTest` — валидация имени, email, возраста и id;
- `UserServiceTest` — бизнес-логика `UserService` на заглушках `UserDao`;
- `UserTest` — сущность `User`;
- `ExceptionTranslatorTest` — преобразование исключений.

Docker, запущенный PostgreSQL, `.env` и `db.properties` для unit-тестов не нужны.

Отдельный класс или метод:

```powershell
mvn "-Dtest=UserServiceTest" test
mvn "-Dtest=UserServiceTest#createUser_normalizesEmail" test
```

В логах `ExceptionTranslatorTest` могут появляться stack trace — тест намеренно передаёт сломанные исключения в `translate()`. Ориентируйтесь на итоговый `BUILD SUCCESS`.

### Интеграционные тесты

```powershell
mvn verify
```

Требования:

- Docker Desktop запущен и `docker info` завершается успешно;
- при первом запуске нужен интернет, чтобы скачать образ `postgres:18`.

Что происходит:

- `UserDaoImplIT` проверяет DAO на реальном PostgreSQL;
- `HibernateUtilIT` проверяет жизненный цикл `SessionFactory`;
- Testcontainers поднимает отдельную временную PostgreSQL 18 с уникальными именем БД, пользователем и паролем;
- схема загружается из `docker/init/01-schema.sql`;
- в `UserDaoImplIT` перед каждым тестом таблица `users` очищается;
- контейнер удаляется вместе с тестовым процессом.

Интеграционные тесты **не используют** локальную БД приложения: запускать `docker compose up` и создавать `db.properties` для них не нужно. Занятый порт 5432 им также не мешает — Testcontainers выбирает свободные порты автоматически.

### Теги и правила добавления тестов

- Интеграционный тест обязан иметь имя `*IT` и аннотацию `@Tag("integration")`.
- Для доступа к БД используется `@ExtendWith(PostgresExtension.class)`.
- Unit-тесты тег не получают и работают без Docker.
- Failsafe включает тесты по тегу `integration` и шаблону `**/*IT.java`; surefire, наоборот, исключает этот тег.

Если добавить интеграционный класс с именем `*Test` без тега, он попадёт в unit-прогон. Если назвать класс `*IT`, но забыть тег, failsafe его не запустит.

### Отдельный интеграционный тест

```powershell
mvn "-Dit.test=UserDaoImplIT" verify
```

При этом unit-тесты тоже выполнятся, а из интеграционных будет выбран только указанный класс.

### Покрытие и отчёты

Покрытие собирает JaCoCo. Цель `report` привязана к фазе `verify`, в отчёт попадают и unit-, и интеграционные тесты:

```powershell
mvn verify
```

Где смотреть:

| Что | Путь |
|---|---|
| HTML-отчёт по покрытию | `target/site/jacoco/index.html` |
| Машиночитаемое покрытие | `target/site/jacoco/jacoco.csv`, `target/site/jacoco/jacoco.xml` |
| Отчёт unit-тестов | `target/surefire-reports/` |
| Отчёт интеграционных тестов | `target/failsafe-reports/` |

После одного `mvn test` HTML-отчёт JaCoCo не формируется — нужен `mvn verify`.

### CI

GitHub Actions (`.github/workflows/tests.yml`) выполняет:

1. `mvn -B test` — unit-тесты;
2. `mvn -B verify` — unit- и интеграционные тесты;
3. загружает отчёты surefire, failsafe и JaCoCo как artifacts.

На `ubuntu-latest` Docker уже доступен, отдельная установка не требуется.

## Запуск приложения

### 1. Поднять PostgreSQL

1. Установите Docker Desktop.
2. `cp .env.example .env` (если порт 5432 занят, поменяйте `POSTGRES_PORT`).
3. `docker compose up -d`.
4. Дождитесь статуса healthy: `docker compose ps`.

### 2. Настроить подключение

```powershell
cp src\main\resources\db.properties.example src\main\resources\db.properties
```

Укажите в `db.properties` логин и пароль из `.env`. Файл `db.properties` не коммитится.

### 3. Запустить приложение

```powershell
mvn compile exec:java
```

## Шпаргалка по командам Docker

| Команда | Действие |
|---|---|
| `docker compose up -d` | Запустить БД в фоне |
| `docker compose ps` | Статус и healthcheck |
| `docker compose logs -f postgres` | Логи БД (Ctrl+C — выйти) |
| `docker compose exec postgres psql -U postgres -d user_service` | Консоль psql |
| `docker compose stop` / `start` | Остановить / запустить (данные на месте) |
| `docker compose down` | Удалить контейнеры и сеть, **том с данными остаётся** |
| `docker compose down -v` | То же + удалить том (**все данные сотрутся!**) |

В команде `psql` логин и имя БД должны совпадать с `POSTGRES_USER` и `POSTGRES_DB` из `.env`.

## Troubleshooting

### Docker Desktop не запущен

Симптомы:

- `Cannot connect to the Docker daemon`;
- `Could not find a valid Docker environment`;
- `DockerClientProviderStrategy` в stack trace.

Решение:

1. Запустите Docker Desktop и дождитесь статуса Engine running.
2. Проверьте: `docker info`.
3. Проверьте запуск контейнера: `docker run --rm hello-world`.
4. На Windows включите backend WSL2 в настройках Docker Desktop и перезапустите терминал/IDE.

### Testcontainers и Ryuk

Симптомы:

- `Can not connect to Ryuk`;
- `RyukContainer`;
- таймаут при скачивании служебного образа Testcontainers.

Что делать:

1. Перезапустите Docker Desktop.
2. Убедитесь, что Docker может скачивать образы: `docker pull postgres:18`.
3. Проверьте VPN, прокси и firewall — они могут блокировать Docker socket или загрузку образов.
4. Крайняя мера — отключить Ryuk на один запуск:

```powershell
$env:TESTCONTAINERS_RYUK_DISABLED = "true"
mvn verify
Remove-Item Env:TESTCONTAINERS_RYUK_DISABLED
```

Без Ryuk контейнеры могут не удалиться автоматически. Проверить и удалить оставшиеся контейнеры:

```powershell
docker ps -a --filter ancestor=postgres:18
docker rm -f <container_id>
```

### Порт уже занят

Для БД приложения:

1. В `.env` укажите другой порт, например `POSTGRES_PORT=5433`.
2. Такой же порт пропишите в `hibernate.connection.url` в `db.properties`.
3. Перезапустите: `docker compose up -d`.

Для интеграционных тестов менять порт не нужно: Testcontainers назначает свободные порты сам. Если порт 5432 занят локальным PostgreSQL, `mvn verify` всё равно работает.

### Отсутствует `db.properties`

Для приложения:

```powershell
cp src\main\resources\db.properties.example src\main\resources\db.properties
```

Заполните логин и пароль. Если файла нет, приложение завершится с ошибкой вида `hibernate.connection.url did not make it into the configuration`.

Для тестов файл не нужен: Testcontainers передаёт параметры подключения через system properties. Не создавайте `db.properties` специально для интеграционных тестов.

### Проблемы с локальной БД приложения

| Симптом | Причина | Решение |
|---|---|---|
| Правки в `01-schema.sql` не применились | Том уже инициализирован — init-скрипты повторно не запускаются | `docker compose down -v` и `docker compose up -d` (**данные сотрутся!**) |
| `password authentication failed` | Пароль в `.env` изменили после первого запуска, а том помнит старый | Тоже `docker compose down -v` и `docker compose up -d` |
| Контейнер постоянно перезапускается | Нет `.env` или опечатка в нём | `docker compose logs postgres` — в логах будет причина |
| Нужно проверить итоговую конфигурацию | — | `docker compose config` покажет файл со всеми подставленными переменными из `.env` |

### Не найдена схема или тесты зависли

- `NoSuchFileException: docker/init/01-schema.sql` — Maven запущен не из корня проекта. Запускайте `mvn verify` из корня или настройте working directory в IDE.
- Первый `mvn verify` долгий — скачиваются образы `postgres:18` и Ryuk. Дождитесь окончания или выполните `docker pull postgres:18` заранее.
- Если Docker Desktop выделяет мало ресурсов, увеличьте память в Settings → Resources.
- Оставшиеся после сбоя контейнеры можно найти командой `docker ps -a` и удалить через `docker rm -f`.
