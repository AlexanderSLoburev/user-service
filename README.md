# User Service

REST-сервис для создания и управления пользователями. Модуль принимает JSON-запросы, проверяет входные данные, выполняет бизнес-операции и сохраняет пользователей в PostgreSQL.

## Как работает приложение

Сервис построен послойно:

1. `UserController` принимает HTTP-запросы по адресу `/api/users` и преобразует их в вызовы сервиса.
2. DTO `UserCreateRequest` и `UserUpdateRequest` проверяются Bean Validation: имя и email обязательны, email должен иметь корректный формат, возраст должен быть положительным числом.
3. `UserService` выполняет операции с пользователями, проверяет уникальность email и преобразует сущности в `UserResponse`.
4. `UserRepository` на базе Spring Data JPA обращается к PostgreSQL. Идентификатор пользователя генерируется базой данных, а `createdAt` устанавливается при создании записи.
5. `GlobalExceptionHandler` формирует единый JSON-ответ для ошибок валидации, отсутствующих пользователей и конфликтов данных.

В таблице `users` email уникален. Hibernate проверяет соответствие сущностей существующей схеме (`ddl-auto: validate`), но не создаёт и не изменяет её.

## Технологический стек

- Java 21;
- Spring Boot 4.1.2, Spring MVC и встроенный Tomcat;
- Spring Data JPA и Hibernate;
- PostgreSQL;
- Jakarta Bean Validation;
- Maven;
- JUnit Jupiter, Mockito, AssertJ и Spring MockMvc;
- Testcontainers для интеграционных тестов с PostgreSQL;
- JaCoCo для сбора покрытия кода.

## REST API

Все маршруты доступны по базовому адресу `/api/users`. Тела запросов и ответов передаются в формате JSON.

| Метод | Путь | Назначение | Успешный ответ |
|---|---|---|---|
| `GET` | `/api/users` | Получить всех пользователей в порядке возрастания ID | `200 OK`, массив пользователей |
| `GET` | `/api/users/{id}` | Получить пользователя по ID | `200 OK`, пользователь |
| `POST` | `/api/users` | Создать пользователя | `201 Created`, пользователь и заголовок `Location` |
| `PUT` | `/api/users/{id}` | Полностью обновить данные пользователя | `200 OK`, обновлённый пользователь |
| `DELETE` | `/api/users/{id}` | Удалить пользователя | `204 No Content` |

Пример тела запроса для создания и обновления:

```json
{
  "name": "Иван",
  "email": "ivan@example.com",
  "age": 30
}
```

В ответе пользователя возвращаются `id`, `name`, `email`, `age` и `createdAt`.

### Ошибки

Ответ об ошибке имеет структуру:

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "invalid email format"
    }
  ]
}
```

| HTTP-статус | Условие |
|---|---|
| `400 Bad Request` | Ошибка проверки полей запроса; подробности передаются в `errors` |
| `404 Not Found` | Пользователь с указанным ID не найден |
| `409 Conflict` | Email уже используется или нарушено ограничение целостности данных |

## Запуск

Нужны JDK 21, Maven и доступная база PostgreSQL с таблицей `users`. SQL-схема находится в `../docker/init/01-schema.sql`; при первом запуске контейнера PostgreSQL через Docker Compose она применяется автоматически.

Настройки подключения считываются из переменных окружения:

| Переменная | Значение по умолчанию |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/user_service` |
| `DB_USER` | `app` |
| `DB_PASSWORD` | `app` |

Запустить базу можно из корня репозитория командой `docker compose up -d`. Убедитесь, что значения `DB_USER` и `DB_PASSWORD` соответствуют настройкам пользователя базы данных. Например, для параметров из `.env.example` в PowerShell:

```powershell
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "postgres"
mvn -pl service spring-boot:run
```

Для сборки модуля:

```powershell
mvn -pl service package
```

## Тесты

Тесты разделены на модульные и интеграционные. Из корня репозитория:

```powershell
# Модульные тесты, PostgreSQL и Docker не нужны
mvn -pl service test

# Полный прогон модуля, включая интеграционные тесты; необходим Docker
mvn -pl service verify
```

Модульные тесты запускаются Surefire:

- `UserServiceTest` проверяет бизнес-правила и взаимодействие с репозиторием на Mockito-заглушках;
- `UserControllerTest` проверяет HTTP-статусы, JSON, заголовок `Location` и обработку ошибок через MockMvc;
- `UserTest` проверяет поведение сущности пользователя.

Интеграционные тесты запускаются Failsafe и используют временный PostgreSQL `postgres:17-alpine` через Testcontainers:

- `UserRepositoryIT` проверяет JPA-репозиторий и ограничения базы данных;
- `UserServiceApplicationIT` проверяет работу приложения целиком — HTTP API, сервисного слоя и PostgreSQL.

Для интеграционных тестов нужен запущенный Docker; отдельная локальная база не требуется. JaCoCo формирует отчёт при `verify` и проверяет минимальное покрытие ветвлений 85%. Отчёт находится в `service/target/site/jacoco/index.html`.
