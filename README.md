# OSINAPI: sistema de gestión de pedidos e inventario

API REST para registrar pedidos y administrar la disponibilidad de productos de un pequeño comercio (examen técnico). Java 17, Spring Boot 4, Maven, Oracle Database Free.

> Estado: **backend y base de datos corregidos y con pruebas. El frontend Angular está pendiente.**

## Documentación

| # | Documento | Contenido |
|---|---|---|
| 1 | [Stack tecnológico](docs/01-stack-tecnologico.md) | Versiones, dependencias, perfiles |
| 2 | [Requerimientos](docs/02-requerimientos.md) | Casos de uso, requerimientos, escenarios A–F |
| 3 | [Entidades y normalización](docs/03-entidades-y-normalizacion.md) | Tablas, 1FN–3FN |
| 4 | [Inicialización de Oracle](docs/04-inicializacion-oracle.md) | Docker, SQL Developer y scripts |
| 5 | [Revisión y correcciones](docs/05-revision-y-correcciones.md) | Hallazgos, cambios y pendientes |
| 6 | [Pruebas](docs/06-pruebas.md) | Pruebas automáticas y plan manual |

## Reglas de negocio implementadas

| Tema | Regla |
|---|---|
| Estados | `BORRADOR → CONFIRMADO → CANCELADO` (y `COMPLETADO`, estado final definido en el modelo) |
| Crear pedido | Genera un `BORRADOR` con folio único (`PED-000001`). Los importes los calcula el servidor. **No descuenta inventario** |
| Confirmar | En una transacción: bloquea pedido y productos, valida el stock de **todas** las partidas y descuenta. Si falta stock: 409 y nada cambia |
| Editar | Solo en `BORRADOR`: reemplaza las partidas y todas toman el precio vigente del catálogo. No mueve inventario. En otro estado: 409 |
| Cancelar | Solo desde `CONFIRMADO`; repone el inventario exactamente una vez. Cancelar de nuevo: 409 |
| Concurrencia | Bloqueo pesimista (`SELECT ... FOR UPDATE`) ordenado por id + `@Version` en `Producto` |

## Endpoints

Documentación interactiva con la aplicación en ejecución: `http://localhost:8080/swagger-ui.html` (OpenAPI en `/v3/api-docs`).

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/productos` | Listar productos (paginado) | 200 |
| GET | `/api/productos/activos` | Listar productos activos | 200 |
| GET | `/api/productos/{id}` | Detalle de producto | 200, 404 |
| POST | `/api/productos` | Crear producto | 201, 400, 409 |
| PUT | `/api/productos/{id}` | Actualizar producto (incluye existencia) | 200, 400, 404, 409 |
| PATCH | `/api/productos/{id}/desactivar` | Baja lógica | 204, 404, 409 |
| PATCH | `/api/productos/{id}/activar` | Reactivar | 204, 404, 409 |
| POST | `/api/pedidos` | Crear pedido (`BORRADOR`) | 201, 400, 404 |
| PUT | `/api/pedidos/{id}` | Actualizar partidas de un `BORRADOR` (precios vigentes) | 200, 400, 404, 409 |
| GET | `/api/pedidos?estado=&folio=` | Listar con filtros (paginado) | 200, 400 |
| GET | `/api/pedidos/{id}` | Detalle con partidas | 200, 404 |
| POST | `/api/pedidos/{id}/confirmar` | Confirmar y descontar inventario | 200, 404, 409 |
| POST | `/api/pedidos/{id}/cancelar` | Cancelar y reponer inventario | 200, 404, 409 |
| POST | `/api/pedidos/{id}/completar` | Completar (solo desde `CONFIRMADO`) | 200, 404, 409 |

Ejemplo de cuerpo para crear un pedido:

```json
{ "productos": [ { "id": 5, "cantidad": 2 }, { "id": 3, "cantidad": 1 } ] }
```

Errores: formato `ProblemDetail` (RFC 9457) con `status` y `detail`; en validaciones, además `errores`. Sin trazas.

## Requisitos previos

- JDK 17 y Maven 3.9+ (acceso a Maven Central).
- Docker (WSL2 o Ubuntu) y Oracle SQL Developer, para Oracle.
- Node.js 20+ (cuando exista el frontend).

## Variables de entorno

Valores de ejemplo en [`.env.example`](.env.example). Spring Boot no lee `.env` por sí solo: cárgalas como variables de entorno o configúralas en el IDE.

| Variable | Ejemplo | Uso |
|---|---|---|
| `PORT` | `8080` | Puerto del backend (opcional, por defecto 8080) |
| `DB_URL` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` | URL JDBC (opcional, ese es el valor por defecto) |
| `DB_USER` | `osin` | Usuario del esquema |
| `DB_PASS` | `osin` | Contraseña de ejemplo para desarrollo local |

Bash (Linux/WSL): `set -a; source .env; set +a`
PowerShell: `Get-Content .env | ForEach-Object { if ($_ -match '^\s*([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }`

## Base de datos (Oracle)

Pasos completos en [docs/04-inicializacion-oracle.md](docs/04-inicializacion-oracle.md). Resumen:

1. `docker pull container-registry.oracle.com/database/free:latest-lite` y crear el contenedor `oracle-local`.
2. Esperar `DATABASE IS READY TO USE!` en `docker logs -f oracle-local`.
3. En SQL Developer ejecutar, en este orden:
   `00_crear_usuario.sql` (como SYS en `FREEPDB1`), luego `01_schema.sql`, `02_data.sql` y `03_verificacion.sql` (como `OSIN`).

Los scripts están en [`database/oracle/`](database/oracle/). `99_limpiar.sql` borra los objetos para empezar de cero.

## Compilar y ejecutar

```bash
mvn clean package -DskipTests    # compilar
mvn spring-boot:run              # ejecutar (con las variables de entorno cargadas y Oracle listo)
```

## Pruebas

```bash
mvn test
```

No necesitan Oracle ni variables de entorno (usan H2 en memoria con el perfil `test`). Detalle y plan manual (incluye el escenario F, reinicio de la aplicación) en [docs/06-pruebas.md](docs/06-pruebas.md).

## Pendientes antes de la entrega

- [x] Frontend Angular (proyecto aparte `osin-front`).
- [ ] Decidir si se conserva el estado `COMPLETADO` (ver docs/05).
- [ ] Ejecutar `mvn test` y los scripts SQL en un clon limpio y registrar el resultado.
- [ ] Video o capturas de los escenarios A–F.
