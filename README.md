# Payment Receipt API

API REST del proyecto `payment-receipt`, desarrollada con Spring Boot. Expone endpoints para analizar comprobantes, guardar resultados, consultar historial del chat y obtener insights del sistema.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring Web MVC / WebFlux
- Spring Data JPA
- PostgreSQL
- Spring AI con proveedor OpenAI compatible
- Docker Compose

## Requisitos

- Java 21
- Maven Wrapper incluido en el repositorio
- PostgreSQL 15 o superior
- Variable de entorno con la API key del proveedor de IA

## Configuracion

El proyecto lee variables desde `.env` o desde el entorno del sistema.

Variables principales:

```bash
APP_NAME=payment-receipt
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/payment-receipt_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=change_me
GROQ_API_KEY=tu_clave
GROQ_BASE_URL=https://api.groq.com/openai/v1
GROQ_MODEL=llama-3.3-70b-versatile
```

## Ejecucion local

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

## Base de datos con Docker

El repositorio incluye `compose.yaml` para levantar PostgreSQL.

```bash
docker compose -f compose.yaml up -d
```

## Scripts utiles

```bash
./mvnw test
./mvnw clean package
```

## Endpoints

### Chat

- `POST /api/chat/mensaje`
- `GET /api/chat/stream?mensaje=...`
- `POST /api/chat/analizar`
- `POST /api/chat/analizar-texto`
- `GET /api/chat/historial`
- `DELETE /api/chat/historial`

### Comprobantes

- `GET /api/comprobantes`
- `GET /api/comprobantes/{id}`
- `GET /api/comprobantes/buscar?q=...`
- `GET /api/comprobantes/tipo/{tipo}`
- `GET /api/comprobantes/estado/{estado}`
- `PATCH /api/comprobantes/{id}`
- `DELETE /api/comprobantes/{id}`
- `GET /api/comprobantes/insights`

## Respuesta estandar

La API responde con un envoltorio `ApiResponse<T>` para mantener consistencia entre exitos y errores.

## Estructura

- `src/main/java/com/paymentReceipt/controller/`: controladores REST.
- `src/main/java/com/paymentReceipt/service/`: logica de negocio.
- `src/main/java/com/paymentReceipt/model/`: entidades, DTOs y enums.
- `src/main/resources/application.properties`: configuracion principal.

## Compilacion

```bash
./mvnw clean package
```

El artefacto se genera en `target/`.
