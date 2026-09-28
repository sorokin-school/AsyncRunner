# Order Payment System

Шаблон для ДЗ по асинхронной оплате заказов (AsyncRunner 2049). Содержит общий модуль с DTO, оркестратор заказов и стабы внешних сервисов.

## Архитектура
- `common-libs` — общие модели/DTO для платежного шлюза и ценового сервиса.
- `order-orchestrator` — API для заказов + каркас оркестрации платежа (бизнес-логика пишется студентами).
- `payment-stub` — HTTP-стабы платежного шлюза и пересчета цен с настраиваемым поведением.
- `order-orchestrator/docker-compose.dev.yaml` — Postgres + стабы для разработки через IDE.
- `infra/docker-compose.dev.yaml` — полный стек (Postgres + стабы + оркестратор).

## Требования
- Docker Desktop (Compose v2).
- JDK 21 нужен только для локального запуска без Docker.

## Режим разработки (IntelliJ IDEA)
1. Убедись, что Docker Desktop запущен.
2. Подними инфраструктуру для разработки:
   ```bash
   docker compose -f order-orchestrator/docker-compose.dev.yaml up --build
   ```
3. Запусти `order-orchestrator` из IntelliJ IDEA.
   - По умолчанию используются:
     - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/orders`
     - `SPRING_DATASOURCE_USERNAME=user`
     - `SPRING_DATASOURCE_PASSWORD=pass`
     - `PAYMENT_STUB_URL=http://localhost:8081`
   - При необходимости переопредели их в конфигурации запуска.

## Полный стек (одна команда)
```bash
docker compose -f infra/docker-compose.dev.yaml up --build
```
По умолчанию: оркестратор на `http://localhost:8080`, стаб на `http://localhost:8081`, Postgres на `localhost:5432`.

## Локальный запуск сервисов без Docker
macOS/Linux:
```bash
./gradlew :order-orchestrator:bootRun
./gradlew :payment-stub:bootRun
```

Windows PowerShell:
```powershell
.\gradlew.bat :order-orchestrator:bootRun
.\gradlew.bat :payment-stub:bootRun
```

## Swagger/UI
- Оркестратор (если включен SpringDoc): `http://localhost:8080/swagger-ui/index.html`
- Payment Stub: `http://localhost:8081/swagger-ui/index.html`

## Compose команды
- Старт общего стека (с пересборкой):
  ```bash
  docker compose -f infra/docker-compose.dev.yaml up --build
  ```
- Остановка с удалением контейнеров/сетей (тома остаются):
  ```bash
  docker compose -f infra/docker-compose.dev.yaml down
  ```
- Полное удаление с томами:
  ```bash
  docker compose -f infra/docker-compose.dev.yaml down -v
  ```
- Просмотр логов:
  ```bash
  docker compose -f infra/docker-compose.dev.yaml logs -f
  ```
