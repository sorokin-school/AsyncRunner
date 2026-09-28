# order-orchestrator

Сервис заказов и каркас оркестрации платежей. HTTP-слой готов, бизнес-логика оркестратора заполняется студентами.

## API
- `POST /order` — создать заказ. Тело: `OrderCreateRequestDto` (пока адрес; остальные поля добавляются студентами). Возвращает `OrderDto` c `id`.
- `GET /order/{id}` — получить заказ по id (возвращает адрес/id; статусы/суммы добавляются студентами).

## Конфигурация (env)
- `SPRING_DATASOURCE_URL` — JDBC для Postgres (по умолчанию `jdbc:postgresql://localhost:5432/orders`).
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`.
- `PAYMENT_STUB_URL` — URL платежного/warehouse стаба (по умолчанию `http://localhost:8081`).
- Liquibase changelog: `classpath:/db/changelog/changelog-master.yaml`.

## Сборка и запуск
```bash
# сборка jar
./gradlew :order-orchestrator:bootJar

# локальный запуск (нужен доступ к БД и стабу чтобы успешно запустился)
./gradlew :order-orchestrator:bootRun
```

## Docker
- Dockerfile лежит в корне модуля.
- Для разработки через IDE можно поднять только инфраструктуру: `order-orchestrator/docker-compose.dev.yaml`.
- Для полного запуска см. `infra/docker-compose.dev.yaml`.

## Что делает студент
- Добавляет недостающие поля/таблицы (orders/payments/payment_tasks).
- Реализует оркестрацию (AUTH → REPRICE → CAPTURE/FAIL) в сервисе задач.
- Настраивает клиенты к платежке/складу и транзакционный outbox/poller.
