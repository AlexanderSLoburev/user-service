# User Service

Учебный проект на Java: REST API для управления пользователями и отдельный сервис email-уведомлений. Сервисы обмениваются событиями через Kafka; PostgreSQL хранит пользователей, Mailpit принимает письма в локальной среде разработки. Единой точкой входа служит API Gateway, который маршрутизирует запросы через Eureka и защищает вызов User Service с помощью Circuit Breaker (Resilience4j).

## Архитектура

```text
Клиент
  │ HTTP :8080
  ▼
User Service ───────► PostgreSQL :5432
  │ CREATED / DELETED
  ▼
Kafka (user-events) ───────► Notification Service ───────► Mailpit :1025
                                    │ HTTP :8086               └─ UI :8025
```

- **`service` — User Service.** Spring MVC принимает запросы на `/api/users`; DTO проверяются Bean Validation, бизнес-логика находится в `UserService`, а `UserRepository` на Spring Data JPA работает с PostgreSQL. `GlobalExceptionHandler` формирует JSON-ответы об ошибках.
- **`api-gateway` — API Gateway.** Единая точка входа: маршруты `/api/users/**` и `/api/notifications/**` проксируются на сервисы через Eureka. Для маршрута `/api/users/**` подключён фильтр `CircuitBreaker` (Resilience4j): при недоступности User Service запрос перенаправляется на `FallbackController` и возвращает `503` с телом `USER_SERVICE_UNAVAILABLE`.
- **`notification-service` — Notification Service.** Получает события `CREATED` и `DELETED` из Kafka, формирует письмо и отправляет его по SMTP. Кроме того, сервис предоставляет HTTP API для прямой отправки уведомлений.
- **PostgreSQL** хранит данные в таблице `users`. SQL-схема находится в `docker/init/01-schema.sql`. Hibernate настроен на `ddl-auto: validate`: при запуске проверяет схему, но не создаёт и не изменяет её.
- **Kafka** передаёт между сервисами события с операцией и email пользователя. Создание и удаление пользователя публикуют события; обновление пользователя уведомление не отправляет.
- **Mailpit** — локальный SMTP-сервер для разработки: письма можно просматривать в веб-интерфейсе, не отправляя их реальным адресатам.

Общие настройки Eureka (`defaultZone`, `prefer-ip-address`) вынесены в `config-repo/application.yaml` и применяются ко всем сервисам; в `user-service` и `notification-service` для работы с балансировщиком добавлена зависимость `spring-cloud-starter-loadbalancer`.

Проект собран как Maven multi-module:

```text
.
├── user-service/            # REST API пользователей
├── notification-service/    # обработка и отправка уведомлений
├── api-gateway/             # единая точка входа, Circuit Breaker
├── config-server/           # раздаёт конфигурации из config-repo
├── eureka-server/           # реестр сервисов
├── config-repo/             # конфигурации сервисов для Config Server
├── docker/
│   └── init/01-schema.sql   # начальная схема PostgreSQL
├── docker-compose.yml       # PostgreSQL, Kafka, Mailpit
└── pom.xml                  # родительский Maven-проект
```

## Технологии

- Java 21, Maven;
- Spring Boot 4.1.2, Spring MVC, Spring Data JPA, Jakarta Bean Validation;
- Spring Cloud Gateway, Spring Cloud Config, Netflix Eureka, Resilience4j;
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
mvn -pl user-service spring-boot:run
```

Укажите `DB_USER` и `DB_PASSWORD`, соответствующие значениям `POSTGRES_USER` и `POSTGRES_PASSWORD` в `.env`. API будет доступен на <http://localhost:8085>.

### 3. Запустить Notification Service

В другом окне PowerShell из корня проекта:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:SMTP_HOST = "localhost"
$env:SMTP_PORT = "1025"
mvn -pl notification-service spring-boot:run
```

HTTP API уведомлений будет доступен на <http://localhost:8086>. Настройки подключения также можно переопределять переменными окружения: `KAFKA_BOOTSTRAP_SERVERS`, `SMTP_HOST`, `SMTP_PORT`, `NOTIFICATION_SITE_NAME` и `NOTIFICATION_FROM`.

### 4. Запустить API Gateway

В отдельном окне PowerShell из корня проекта:

```powershell
mvn -pl api-gateway spring-boot:run
```

Gateway будет доступен на <http://localhost:9000>. Маршруты проксируют `/api/users/**` и `/api/notifications/**` через Eureka; конфигурация маршрутов приходит из Config Server (`config-repo/api-gateway.yaml`).

Для маршрута `/api/users/**` настроен Circuit Breaker Resilience4j: скользящее окно 10 вызовов, минимум 5 вызовов для расчёта, порог отказов 50%, открытое состояние 10 секунд с 3 пробными вызовами в полуоткрытом состоянии, таймаут запроса 3 секунды. Если User Service недоступен, запрос через `forward:/fallback/users` возвращает `503` с телом `USER_SERVICE_UNAVAILABLE`.

Чтобы остановить инфраструктуру, выполните `docker compose down`. Данные PostgreSQL сохраняются в Docker-томе; удаляйте том отдельно, только если хотите сбросить локальную базу.

## REST API

### Swagger UI

После запуска сервисов документацию и интерактивное тестирование API можно открыть в браузере:

- User Service: [Swagger UI](http://localhost:8085/swagger-ui/index.html), [OpenAPI JSON](http://localhost:8085/v3/api-docs);
- Notification Service: [Swagger UI](http://localhost:8086/swagger-ui/index.html), [OpenAPI JSON](http://localhost:8086/v3/api-docs).

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

`POST /api/notifications` на `localhost:8086` принимает `operation` (`CREATED` или `DELETED`) и корректный `email`. При успешном принятии запроса сервис отвечает `202 Accepted`.

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8086/api/notifications `
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
mvn -pl user-service test

# Проверка User Service, включая интеграционные тесты с PostgreSQL через Testcontainers
mvn -pl user-service verify

# Модульные тесты Notification Service
mvn -pl notification-service test
```

Для `user-service verify` нужен работающий Docker, но отдельная локальная база PostgreSQL не требуется: интеграционные тесты запускают временный контейнер. `verify` также создаёт отчёт JaCoCo в `user-service/target/site/jacoco/index.html` и проверяет минимальное покрытие ветвлений 85%.

Unit-тесты запускаются в отдельном JVM (`forkCount=1` в родительском `pom.xml`), чтобы агент JaCoCo учитывал их покрытие. Интеграционные тесты (`UserServiceApplicationIT`, `NotificationApiIT`, `KafkaToEmailIT`, `NotificationServiceApplicationIT`) отключают Eureka-клиент свойством `eureka.client.enabled=false`, поэтому для их запуска не нужен работающий реестр.
