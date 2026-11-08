# User Service

Учебный проект на Java: REST API для управления пользователями и отдельный сервис email-уведомлений. Сервисы обмениваются событиями через Kafka; PostgreSQL хранит пользователей, Mailpit принимает письма в локальной среде разработки.

## Архитектура

```text
Клиент
  │ HTTP :8080
  ▼
User Service ───────► PostgreSQL :5432
  │ CREATED / DELETED
  ▼
Kafka (user-events) ───────► Notification Service ───────► Mailpit :1025
                                    │ HTTP :8081               └─ UI :8025
```

- **`service` — User Service.** Spring MVC принимает запросы на `/api/users`; DTO проверяются Bean Validation, бизнес-логика находится в `UserService`, а `UserRepository` на Spring Data JPA работает с PostgreSQL. `GlobalExceptionHandler` формирует JSON-ответы об ошибках.
- **`notification-service` — Notification Service.** Получает события `CREATED` и `DELETED` из Kafka, формирует письмо и отправляет его по SMTP. Кроме того, сервис предоставляет HTTP API для прямой отправки уведомлений.
- **PostgreSQL** хранит данные в таблице `users`. SQL-схема находится в `docker/init/01-schema.sql`. Hibernate настроен на `ddl-auto: validate`: при запуске проверяет схему, но не создаёт и не изменяет её.
- **Kafka** передаёт между сервисами события с операцией и email пользователя. Создание и удаление пользователя публикуют события; обновление пользователя уведомление не отправляет.
- **Mailpit** — локальный SMTP-сервер для разработки: письма можно просматривать в веб-интерфейсе, не отправляя их реальным адресатам.

Проект собран как Maven multi-module:

```text
.
├── service/                 # REST API пользователей
├── notification-service/    # обработка и отправка уведомлений
├── docker/
│   └── init/01-schema.sql   # начальная схема PostgreSQL
├── docker-compose.yml       # PostgreSQL, Kafka, Mailpit
└── pom.xml                  # родительский Maven-проект
```

## Технологии

- Java 21, Maven;
- Spring Boot 4.1.2, Spring MVC, Spring Data JPA, Jakarta Bean Validation;
- PostgreSQL и Kafka;
- SMTP / Mailpit;
- JUnit, Mockito, Spring MockMvc и Testcontainers;
- JaCoCo для проверки покрытия модуля `service`.

## Запуск локально

Необходимы JDK 21, Maven и Docker с Docker Compose.

### 1. Запустить инфраструктуру

Из корня проекта в PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d
```

При необходимости измените параметры PostgreSQL в `.env` перед запуском. Compose поднимает:

| Компонент | Адрес | Назначение |
|---|---|---|
| PostgreSQL | `localhost:5432` | База `user_service` |
| Kafka | `localhost:9092` | Топик `user-events` |
| Mailpit SMTP | `localhost:1025` | Приём тестовых писем |
| Mailpit Web UI | <http://localhost:8025> | Просмотр писем |

Скрипт схемы выполняется PostgreSQL автоматически только при первом запуске с пустым томом данных.

### 2. Запустить User Service

В отдельном окне PowerShell из корня проекта:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/user_service"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "postgres"
mvn -pl service spring-boot:run
```

Укажите `DB_USER` и `DB_PASSWORD`, соответствующие значениям `POSTGRES_USER` и `POSTGRES_PASSWORD` в `.env`. API будет доступен на <http://localhost:8080>.

### 3. Запустить Notification Service

В другом окне PowerShell из корня проекта:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:SMTP_HOST = "localhost"
$env:SMTP_PORT = "1025"
mvn -pl notification-service spring-boot:run
```

HTTP API уведомлений будет доступен на <http://localhost:8081>. Настройки подключения также можно переопределять переменными окружения: `KAFKA_BOOTSTRAP_SERVERS`, `SMTP_HOST`, `SMTP_PORT`, `NOTIFICATION_SITE_NAME` и `NOTIFICATION_FROM`.

Чтобы остановить инфраструктуру, выполните `docker compose down`. Данные PostgreSQL сохраняются в Docker-томе; удаляйте том отдельно, только если хотите сбросить локальную базу.

## REST API

### Пользователи

Базовый путь: `/api/users`.

| Метод | Путь | Результат |
|---|---|---|
| `GET` | `/api/users` | Список пользователей |
| `GET` | `/api/users/{id}` | Пользователь по ID |
| `POST` | `/api/users` | Создать пользователя (`201 Created`) |
| `PUT` | `/api/users/{id}` | Полностью обновить пользователя |
| `DELETE` | `/api/users/{id}` | Удалить пользователя (`204 No Content`) |

Для `POST` и `PUT` передайте JSON с непустыми `name`, корректным `email` и положительным `age`:

```json
{
  "name": "Иван",
  "email": "ivan@example.com",
  "age": 30
}
```

Email должен быть уникальным. Типовые ответы об ошибках: `400 Bad Request` при ошибке валидации, `404 Not Found` для отсутствующего пользователя, `409 Conflict` при повторном email.

Пример создания пользователя в PowerShell:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/users `
  -ContentType "application/json" `
  -Body '{"name":"Иван","email":"ivan@example.com","age":30}'
```

### Уведомления

`POST /api/notifications` на `localhost:8081` принимает `operation` (`CREATED` или `DELETED`) и корректный `email`. При успешном принятии запроса сервис отвечает `202 Accepted`.

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8081/api/notifications `
  -ContentType "application/json" `
  -Body '{"operation":"CREATED","email":"ivan@example.com"}'
```

Письма, отправленные напрямую через API или по событию Kafka, можно посмотреть в Mailpit: <http://localhost:8025>.

## Сборка и тесты

Из корня проекта:

```powershell
# Собрать оба Maven-модуля
mvn clean package

# Модульные тесты User Service
mvn -pl service test

# Проверка User Service, включая интеграционные тесты с PostgreSQL через Testcontainers
mvn -pl service verify

# Модульные тесты Notification Service
mvn -pl notification-service test
```

Для `service verify` нужен работающий Docker, но отдельная локальная база PostgreSQL не требуется: интеграционные тесты запускают временный контейнер. `verify` также создаёт отчёт JaCoCo в `service/target/site/jacoco/index.html` и проверяет минимальное покрытие ветвлений 85%.
