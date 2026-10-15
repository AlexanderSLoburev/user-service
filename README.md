## Запуск БД
1. Установите Docker Desktop.
2. `cp .env.example .env` (при занятом порте 5432 поменяйте POSTGRES_PORT).
3. `docker compose up -d`.
4. Дождитесь статуса healthy: `docker compose ps`.

## Шпаргалка по командам

| Команда | Действие |
|---|---|
| `docker compose up -d` | Запустить БД в фоне |
| `docker compose ps` | Статус и healthcheck |
| `docker compose logs -f postgres` | Логи БД (Ctrl+C — выйти) |
| `docker compose exec postgres psql -U postgres -d user_service` | Консоль psql |
| `docker compose stop` / `start` | Остановить / запустить (данные на месте) |
| `docker compose down` | Удалить контейнеры и сеть, **том с данными остаётся** |
| `docker compose down -v` | То же + удалить том (**все данные сотрутся!**) |

## Типичные проблемы

| Симптом | Причина | Решение |
|---|---|---|
| `port is already allocated` / порт занят | Локальный PostgreSQL уже стоит на 5432 | В `.env` поставить `POSTGRES_PORT=5433` и тот же порт в URL приложения (либо остановить локальный сервис) |
| Правки в `01-schema.sql` не применились | Том уже инициализирован — скрипты повторно не запускаются | `docker compose down -v && docker compose up -d` (⚠️ данные сотрутся) |
| `password authentication failed` | Пароль в `.env` поменяли после первого запуска, а том помнит старый | Тоже `down -v` и заново |
| Контейнер постоянно перезапускается | Нет `.env` или опечатка в нём | `docker compose logs postgres` — в логах будет причина |
| `cannot connect to the Docker daemon` | Docker Desktop не запущен | Запустить приложение Docker и повторить |

Полезно для отладки: `docker compose config` — покажет итоговый файл со всеми подставленными переменными из `.env`.
