# Sistema de gestión de pedidos e inventario

Aplicación web para registrar pedidos y administrar la disponibilidad de productos de un pequeño comercio. Es la solución al examen técnico: **Angular** (frontend), **Java 17 + Spring Boot 4 + Maven** (backend) y base de datos relacional (**Oracle** como motor oficial, **H2** como perfil de desarrollo).

> Estado: **fase de levantamiento de requerimientos y diseño de datos**. El código aún no existe; los comandos de compilación y ejecución de este README son el objetivo y se validarán al implementar.

## Documentación (orden de lectura)

| # | Documento | Contenido |
|---|---|---|
| 1 | [Stack tecnológico](docs/01-stack-tecnologico.md) | Versiones, dependencias, perfiles, puertos, variables de entorno y riesgos |
| 2 | [Requerimientos](docs/02-requerimientos.md) | Casos de uso, requerimientos funcionales y no funcionales, restricciones, supuestos, escenarios de aceptación |
| 3 | [Entidades y normalización](docs/03-entidades-y-normalizacion.md) | Tablas, tipos, restricciones y análisis 1FN, 2FN y 3FN |
| 4 | [Inicialización de Oracle](docs/04-inicializacion-oracle.md) | Docker en WSL/Ubuntu, conexión, usuario, scripts SQL y verificación |

Scripts SQL listos para ejecutar: [`database/oracle/`](database/oracle/).

## Decisiones de diseño (resumen)

| Tema | Decisión |
|---|---|
| Motor oficial | Oracle Database Free (contenedor Docker, imagen `free:latest-lite`) |
| Perfil de desarrollo | H2 (sin instalar nada); no sustituye a Oracle en la entrega |
| Estados del pedido | `BORRADOR`, `CONFIRMADO`, `CANCELADO` |
| Descuento de inventario | Al **confirmar** el pedido |
| Cancelación | Repone stock solo si el pedido estaba `CONFIRMADO` (idempotente) |
| Importes | Se calculan en el backend; no se almacenan `subtotal` ni `total` (3FN) |
| Concurrencia | Bloqueo pesimista de productos, siempre en orden por `id` |

## Estructura propuesta del repositorio

```
.
├── README.md
├── docs/                  # esta documentación + capturas/video
├── database/
│   └── oracle/            # 00_crear_usuario.sql, 01_schema.sql, 02_data.sql, ...
├── backend/               # Spring Boot (Maven)
└── frontend/              # Angular 19
```

## Versiones utilizadas

| Componente | Versión |
|---|---|
| JDK | 17 |
| Spring Boot | 4.0.x |
| Maven | 3.9+ (se usa el wrapper `mvnw`) |
| Angular / Angular Material | 19.x |
| Node.js | 20 LTS (>= 20.11.1) o 22 LTS |
| Oracle Database Free | imagen `container-registry.oracle.com/database/free:latest-lite` |

Detalle completo en [docs/01-stack-tecnologico.md](docs/01-stack-tecnologico.md).

## Requisitos previos

- JDK 17 y acceso a Maven Central.
- Node.js 20+ y npm.
- Docker (Docker Desktop con WSL2, o Docker Engine en Ubuntu) para Oracle.
- Git.

## Variables de entorno

Valores de ejemplo no sensibles. Nunca se versionan contraseñas reales.

| Variable | Ejemplo | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `h2` o `oracle` | Selecciona la base de datos |
| `DB_URL` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` | URL JDBC (perfil `oracle`) |
| `DB_USER` | `pedidos` | Usuario de aplicación |
| `DB_PASSWORD` | `Pedidos_2026` | Contraseña del usuario de aplicación (ejemplo) |
| `SERVER_PORT` | `8080` | Puerto del backend |

## Compilación y ejecución (objetivo, pendiente de validar)

```bash
# Base de datos Oracle: ver docs/04-inicializacion-oracle.md

# Backend (perfil H2, sin dependencias externas)
cd backend
./mvnw clean package
./mvnw spring-boot:run

# Backend contra Oracle
SPRING_PROFILES_ACTIVE=oracle DB_USER=pedidos DB_PASSWORD=Pedidos_2026 ./mvnw spring-boot:run

# Frontend
cd frontend
npm install
npm start            # http://localhost:4200
```

## Pruebas (objetivo)

```bash
cd backend  && ./mvnw test
cd frontend && npm test
```

## Documentación de la API

Con el backend en ejecución: `http://localhost:8080/swagger-ui.html` (OpenAPI en `/v3/api-docs`).

## Pendientes antes de la entrega

- [ ] Implementar backend y frontend.
- [ ] Probar los escenarios A–F (ver [requerimientos](docs/02-requerimientos.md#7-escenarios-de-aceptación)).
- [ ] Validar este README y los scripts en un clon limpio del repositorio.
- [ ] Capturas o video de los escenarios.
