# Task Service

Тестовое задание: реализовать сервис управления задачами на Java 21 / Spring Boot / Spring Data JPA / PostgreSQL / Kafka / OpenAPI.

REST API:
- получение задач с пагинацией;
- получение задачи по ID;
- создание задачи;
- назначение исполнителя;
- изменение статуса.

Модель данных:
- задача: id, наименование, исполнитель, описание, статус;
- пользователь: id, имя, почта.

При создании задачи и назначении исполнителя сервис сохраняет событие в Outbox и отправляет его в Kafka. Авторизация и onboarding пользователей не входят в задание.

## Проект

Основные каталоги:

```
src/main/java/ru/pavlikov/task_service/
  config/                 конфигурация Spring
  controller/             REST API
  exception/              обработка ошибок
  infrastructure/outbox/  Outbox и Kafka
  model/                  JPA-модели и DTO
  repository/             Spring Data JPA
  service/                бизнес-логика

src/main/resources/
  db/migration/           Flyway
  static/                  OpenAPI specification

postman/
  task-service.postman_collection.json
```

OpenAPI specification:

```
src/main/resources/static/task-service-api.yaml
```

Тестовые данные создаются миграциями Flyway `V4__insert_test_users.sql` и `V5__insert_test_tasks.sql`.

## Требования

- JDK 21
- Docker
- Docker Compose
- Git

Maven отдельно устанавливать не нужно: в проекте есть Maven Wrapper.

Проверка окружения:

```
java -version
docker --version
docker compose version
git --version
```

## Генерация OpenAPI

Выполнить:

Linux/macOS:

```
./mvnw clean generate-sources
```

Windows:

```
mvnw.cmd clean generate-sources
```

Сгенерированные API и DTO находятся в `src/main/java`.

## Проверка и сборка

Linux/macOS:

```
./mvnw clean test
./mvnw clean package
```

Windows:

```
mvnw.cmd clean test
mvnw.cmd clean package
```

Интеграционные тесты используют Testcontainers, поэтому во время тестов Docker должен быть запущен.

## Запуск всего проекта через Docker

Из корня проекта:

```
docker compose down -v
docker compose up --build
```

Compose запускает:

- PostgreSQL;
- ZooKeeper;
- Kafka;
- создание topic `task-events`;
- приложение `task-service`.

После запуска доступны:

```
REST API:       http://localhost:8080
Swagger UI:     http://localhost:8080/swagger-ui/index.html
OpenAPI JSON:   http://localhost:8080/v3/api-docs
OpenAPI YAML:   http://localhost:8080/v3/api-docs.yaml
Health:         http://localhost:8080/actuator/health
PostgreSQL:     localhost:5434
Kafka:          localhost:9092
```