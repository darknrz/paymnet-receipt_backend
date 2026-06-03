# Payment Receipt API

API REST del proyecto `payment-receipt`, desarrollada con Spring Boot. Expone endpoints para analizar comprobantes, guardar resultados, consultar historial del chat y obtener insights del sistema.

## Descripcion

Este backend recibe texto, PDFs e imagenes de comprobantes. Luego orquesta la extraccion, el analisis con IA, el mapeo a entidades y la persistencia en PostgreSQL. Tambien expone endpoints para consulta, edicion y eliminacion de comprobantes, ademas del historial del chat.

## Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring AI
- PostgreSQL
- Maven Wrapper
- Docker Compose para la base de datos

## Arquitectura

El proyecto sigue una arquitectura por capas:

- `controller`: entrada HTTP y validacion basica.
- `service`: logica de negocio y orquestacion.
- `agent`: integracion con IA y analisis multimodal.
- `model`: entidades, DTOs y enums.
- `repository`: acceso a datos con JPA.
- `exception`: manejo centralizado de errores.

## Model

La capa `model` contiene:

- `entity`: `Comprobante`, `ItemComprobante`, `MensajeChat`.
- `dto`: `ChatRequest`, `ChatResponse`, `ComprobanteResponse`, `InsightResponse`, entre otros.
- `enum`: `TipoComprobante`, `EstadoComprobante`, `RolMensaje`.

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
GROQ_MODEL=meta-llama/llama-4-scout-17b-16e-instruct
```

Para analisis de imagenes se requiere un modelo con soporte de vision.

## Ejecucion local

```bash
./mvnw spring-boot:run
```

En Windows:

```bash
mvnw.cmd spring-boot:run
```

## Base de datos con Docker

```bash
docker compose -f compose.yaml up -d
```

## Pruebas y build

```bash
./mvnw test
./mvnw clean package
```

## Flujo principal

1. El controller recibe la peticion.
2. El service decide si debe analizar texto, PDF o imagen.
3. El agent llama al modelo de IA.
4. El resultado JSON se convierte a entidad.
5. El repository persiste en PostgreSQL.
6. El backend responde con un `ApiResponse<T>`.

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

## Contrato de respuesta

La API responde con `ApiResponse<T>` para mantener consistencia entre casos exitosos y de error.

## Manejo de errores

El proyecto usa `GlobalExceptionHandler` para centralizar:

- comprobantes no encontrados
- errores del agente de IA
- validaciones de entrada
- archivos demasiado grandes
- errores inesperados

## Estructura

- `src/main/java/com/paymentReceipt/controller/`: controladores REST.
- `src/main/java/com/paymentReceipt/service/`: logica de negocio.
- `src/main/java/com/paymentReceipt/agent/`: integracion con IA.
- `src/main/java/com/paymentReceipt/model/`: entidades, DTOs y enums.
- `src/main/java/com/paymentReceipt/repository/`: acceso a datos.
- `src/main/java/com/paymentReceipt/exception/`: manejo de errores.
- `src/main/resources/application.properties`: configuracion principal.

## Notas de diseno

- Se usa `@Transactional` para mantener consistencia al persistir comprobantes e historial.
- Se separan DTOs y entidades para no exponer directamente el modelo interno.
- La respuesta de IA se normaliza antes de mapearse al dominio.
- El backend expone endpoints REST y usa una configuracion compatible con OpenAI/Groq.

## Compilacion

```bash
./mvnw clean package
```

El artefacto se genera en `target/`.
