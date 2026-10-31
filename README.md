# OSINAPI: sistema de gestión de pedidos e inventario

## IMPORTANTE MIRAR EL VIDEO PARA ENTENDER MEJOR(Corregido)
```
[https://youtu.be/f1pAgcBTV0k](https://youtu.be/XeBDBUWzIG0)
```

API REST para registrar pedidos y administrar la disponibilidad de productos de un pequeño comercio (examen técnico). Java 17, Spring Boot 4, Maven y Oracle Database Free.

> Estado: **backend y base de datos terminados, con pruebas automáticas.**

## Contenido

1. [Documentación](#documentación)
2. [Reglas de negocio](#reglas-de-negocio-implementadas)
3. [Endpoints](#endpoints)
4. [Cómo correr el proyecto](#cómo-correr-el-proyecto) ← **empieza aquí**
5. [Uso diario: detener e iniciar](#uso-diario-detener-e-iniciar)
6. [Pruebas](#pruebas)
7. [Solución de problemas](#solución-de-problemas)
8. [Pendientes](#pendientes-antes-de-la-entrega)

## Documentación

| # | Documento | Contenido |
|---|---|---|
| 1 | [Stack tecnológico](docs/01-stack-tecnologico.md) | Versiones, dependencias, configuración, transacciones y concurrencia |
| 2 | [Requerimientos](docs/02-requerimientos.md) | Casos de uso, requerimientos, errores, supuestos y escenarios A–F |
| 3 | [Entidades y normalización](docs/03-entidades-y-normalizacion.md) | Tablas, 1FN–3FN, diccionario de datos |
| 4 | [Inicialización de Oracle](docs/04-inicializacion-oracle.md) | Docker, SQL Developer, scripts y errores frecuentes |
| 5 | [Pruebas](docs/05-pruebas.md) | Pruebas automáticas y plan manual |

## Reglas de negocio implementadas

| Tema | Regla |
|---|---|
| Estados | `BORRADOR → CONFIRMADO → COMPLETADO`; `BORRADOR` o `CONFIRMADO` pueden pasar a `CANCELADO`. `COMPLETADO` y `CANCELADO` son finales |
| Crear pedido | Genera un `BORRADOR` con folio único (`PED-000001`). Los importes los calcula el servidor. **No descuenta inventario** |
| Editar | Solo en `BORRADOR`: reemplaza las partidas y todas toman el precio vigente del catálogo. No mueve inventario. En otro estado: 409 |
| Confirmar | En una transacción valida el stock de **todas** las partidas y después descuenta. Si falta stock: 409 y nada cambia |
| Cancelar | Desde `BORRADOR` (no mueve inventario) o `CONFIRMADO` (repone el inventario exactamente una vez). Cancelar de nuevo: 409 |
| Completar | Solo desde `CONFIRMADO`. No mueve inventario; después ya no se puede cancelar |
| Concurrencia | `@Transactional` en los servicios + bloqueo optimista (`@Version`, columna `VERSION_LOCK`) en `Producto` y `Pedido`: si dos peticiones modifican el mismo registro, la segunda recibe 409 |

## Endpoints

Documentación interactiva con la aplicación en ejecución: `http://localhost:8080/swagger-ui.html` (OpenAPI en `/v3/api-docs`).

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| GET | `/api/productos?pagina=&tamano=` | Listar productos (paginado) | 200, 400 |
| GET | `/api/productos/activos` | Listar productos activos | 200 |
| GET | `/api/productos/{id}` | Detalle de producto | 200, 404 |
| POST | `/api/productos` | Crear producto | 201, 400, 409 |
| PUT | `/api/productos/{id}` | Actualizar producto (incluye existencia) | 200, 400, 404, 409 |
| PATCH | `/api/productos/{id}/desactivar` | Baja lógica | 204, 404, 409 |
| PATCH | `/api/productos/{id}/activar` | Reactivar | 204, 404, 409 |
| POST | `/api/pedidos` | Crear pedido (`BORRADOR`) | 201, 400, 404 |
| PUT | `/api/pedidos/{id}` | Actualizar partidas de un `BORRADOR` (precios vigentes) | 200, 400, 404, 409 |
| GET | `/api/pedidos?estado=&folio=&pagina=&tamano=` | Listar con filtros (paginado) | 200, 400 |
| GET | `/api/pedidos/{id}` | Detalle con partidas | 200, 404 |
| POST | `/api/pedidos/{id}/confirmar` | Confirmar y descontar inventario | 200, 404, 409 |
| POST | `/api/pedidos/{id}/cancelar` | Cancelar (y reponer inventario si estaba confirmado) | 200, 404, 409 |
| POST | `/api/pedidos/{id}/completar` | Completar (solo desde `CONFIRMADO`) | 200, 404, 409 |

Ejemplo de cuerpo para crear o editar un pedido:

```json
{ "productos": [ { "id": 5, "cantidad": 2 }, { "id": 3, "cantidad": 1 } ] }
```

Errores: formato `ProblemDetail` (RFC 9457) con `status` y `detail`; en validaciones, además `errores`. Sin trazas.

---

## Cómo correr el proyecto

Orden obligatorio: **1. Oracle → 2. esquema y datos → 3. configuración → 4. backend.** El backend no arranca si Oracle no está listo o si faltan las tablas.

Puertos que deben estar libres:

| Servicio | Puerto | URL |
|---|---|---|
| Oracle | `1521` | servicio `FREEPDB1` |
| Backend | `8080` | `http://localhost:8080/api` |

### Paso 0. Instalar y comprobar los requisitos

| Herramienta | Versión | Comprobar con | Debe mostrar |
|---|---|---|---|
| JDK | 17 o superior | `java -version` | `17` o mayor |
| IDE con soporte para Maven | IntelliJ IDEA, Eclipse, VS Code o Spring Tool Suite | Abrir el proyecto | El IDE importa el `pom.xml` y descarga las dependencias |
| Docker | Cualquier versión reciente (Docker Desktop con WSL2 en Windows) | `docker --version` y `docker ps` | Versión y una tabla (aunque vacía) sin errores |
| Oracle SQL Developer | Cualquier versión reciente | Abrir la aplicación | — |

También necesitas acceso a Internet la primera vez: el IDE descarga las dependencias de Maven y Docker descarga la imagen de Oracle.

> Si `docker ps` muestra *"Cannot connect to the Docker daemon"*, abre Docker Desktop (Windows/macOS) o ejecuta `sudo service docker start` (WSL/Ubuntu) y vuelve a probar.

### Paso 1. Levantar Oracle en Docker (solo la primera vez)

Desde una terminal (WSL, Linux o PowerShell):

```bash
docker pull container-registry.oracle.com/database/free:latest-lite
```

```bash
docker run -d --name oracle-local -p 1521:1521 -e ORACLE_PWD=Oracle_2026 -v oracle-data:/opt/oracle/oradata container-registry.oracle.com/database/free:latest-lite
```

| Parte | Significado |
|---|---|
| `--name oracle-local` | Nombre del contenedor; se usa para iniciarlo y detenerlo después |
| `-p 1521:1521` | Publica el puerto de Oracle en tu máquina |
| `-e ORACLE_PWD=Oracle_2026` | Contraseña de `SYS` y `SYSTEM` (solo se aplica al crear la base por primera vez) |
| `-v oracle-data:...` | Volumen donde se guardan los datos; **sobrevive** a reinicios del contenedor |

Espera a que termine de inicializar (la primera vez tarda varios minutos):

```bash
docker logs -f oracle-local
```

Cuando aparezca `DATABASE IS READY TO USE!`, pulsa `Ctrl + C` (solo sale de los logs; Oracle sigue corriendo). Comprueba:

```bash
docker inspect --format='{{.State.Health.Status}}' oracle-local
```

Debe responder `healthy`.

> Si el contenedor ya existe, **no** vuelvas a ejecutar `docker run`: usa `docker start oracle-local`.

### Paso 2. Crear el usuario, el esquema y los datos (solo la primera vez)

Todo se hace en **Oracle SQL Developer**. Cada script se abre con *File > Open* y se ejecuta con **Run Script (F5)**, no con *Run Statement* (Ctrl+Enter), porque los scripts tienen varias sentencias.

**2.1. Conexión de administrador.** Crea una conexión nueva:

| Campo | Valor |
|---|---|
| Name | `SYSTEM_LOCAL` |
| Username / Password | `system` / `Oracle_2026` |
| Hostname / Port | `localhost` / `1521` |
| Service name | `FREEPDB1` (elige *Service name*, no *SID*) |

Pulsa **Test** (debe decir *Success*) y luego **Connect**.

**2.2. Crear el usuario `OSIN`.** Con `SYSTEM_LOCAL` abierta, ejecuta `database/oracle/00_crear_usuario.sql` (F5). Crea el usuario `osin` con contraseña `osin` y le otorga todos los permisos (`GRANT ALL PRIVILEGES`), suficiente para crear y llenar las tablas. Es una configuración para desarrollo local; ver [docs/04](docs/04-inicializacion-oracle.md#7-crear-el-usuario-osin).

**2.3. Conexión del proyecto.** Crea otra conexión:

| Campo | Valor |
|---|---|
| Name | `OSIN_LOCAL` |
| Username / Password | `osin` / `osin` |
| Hostname / Port | `localhost` / `1521` |
| Service name | `FREEPDB1` |

**2.4. Esquema y datos.** Con `OSIN_LOCAL` abierta (no con `SYSTEM_LOCAL`), ejecuta **en este orden** y revisa que *Script Output* no muestre errores:

| Orden | Script | Qué hace |
|---|---|---|
| 1 | `database/oracle/01_schema.sql` | Tablas, restricciones, índices, secuencia del folio y vista |
| 2 | `database/oracle/02_data.sql` | 8 productos y 3 pedidos de ejemplo (`PED-000001` a `PED-000003`) |
| 3 | `database/oracle/03_verificacion.sql` | Consultas para comprobar que todo quedó bien |

Al terminar, `SELECT COUNT(*) FROM PRODUCTOS;` debe devolver `8`.

Para empezar de cero: ejecuta `99_limpiar.sql` como `OSIN` y repite el paso 2.4. Detalle y errores comunes en [docs/04-inicializacion-oracle.md](docs/04-inicializacion-oracle.md).

### Paso 3. Configurar el `.env`

Copia `.env.example` como `.env` en la raíz del proyecto (junto a `pom.xml`) y ajusta los valores si los tuyos son distintos:

```properties
PORT=8080
DB_URL=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
DB_USER=osin
DB_PASS=osin
CORS_ORIGENES=http://localhost:4200
```

| Variable | Uso |
|---|---|
| `PORT` | Puerto del backend |
| `DB_URL` | Conexión a Oracle (host, puerto y servicio `FREEPDB1`) |
| `DB_USER` / `DB_PASS` | Usuario y contraseña creados en el paso 2.2 |
| `CORS_ORIGENES` | Orígenes (separados por coma) a los que el navegador permite llamar a la API |

Spring Boot lee el `.env` al arrancar (`spring.config.import=optional:file:.env[.properties]`). `.env` está en `.gitignore`; en el repositorio solo va `.env.example`.

### Paso 4. Ejecutar el proyecto

Con Oracle corriendo y el `.env` configurado, el proyecto está listo: ábrelo en el IDE y ejecuta la clase `OsinapiApplication` (`src/main/java/com/lacomer/osinapi/OsinapiApplication.java`).

El backend está listo cuando el log muestra:

```
Started OsinapiApplication in X seconds
```

### Paso 5. Comprobar que el backend responde

En otra terminal o en el navegador:

| Prueba | Comando o URL | Resultado esperado |
|---|---|---|
| Productos activos | `curl http://localhost:8080/api/productos/activos` | JSON con 8 productos (Arroz, Frijol, Aceite, …) |
| Pedidos | `curl "http://localhost:8080/api/pedidos?pagina=0&tamano=10"` | JSON con `content` (3 pedidos) y `page` |
| Error controlado | `curl http://localhost:8080/api/pedidos/999999` | 404 con `"detail": "Pedido no encontrado con id: 999999"` |
| Swagger | `http://localhost:8080/swagger-ui.html` | Documentación interactiva con los grupos *Pedidos* y *Productos* |

> En PowerShell, `curl` es un alias de `Invoke-WebRequest`; usa `curl.exe` o abre las URL en el navegador.

### Resumen rápido (cuando ya está todo instalado)

```bash
docker start oracle-local     # 1. Oracle (esperar a "healthy")
# 2. Ejecutar OsinapiApplication desde el IDE
# 3. Probar: http://localhost:8080/swagger-ui.html
```

---

## Uso diario: detener e iniciar

**Detener** (en orden inverso):

1. Backend: detener la ejecución en el IDE.
2. Oracle: `docker stop oracle-local`.

**Iniciar de nuevo:** `docker start oracle-local`, esperar a `healthy` (`docker inspect --format='{{.State.Health.Status}}' oracle-local`), después el paso 4. No repitas los pasos 1 y 2: los datos siguen en el volumen `oracle-data` (escenario F de persistencia).

> **No** ejecutes `docker rm -v oracle-local` ni borres el volumen `oracle-data` si quieres conservar la base de datos.

---

## Pruebas

Desde el IDE: clic derecho sobre `src/test/java` → *Run 'All Tests'*.

No necesitan Oracle, Docker ni variables de entorno: usan H2 en memoria en modo Oracle con el perfil `test` (`src/test/resources/application-test.properties`). Puedes correrlas con Oracle apagado. Todas deben pasar.

Plan manual de los escenarios A–F en [docs/05-pruebas.md](docs/05-pruebas.md).

---

## Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| `docker run` dice *"name oracle-local is already in use"* | El contenedor ya existe | `docker start oracle-local` en lugar de `docker run` |
| `docker run` dice *"port is already allocated"* (1521) | Otra instancia de Oracle usa el puerto | Detén la otra instancia, o usa `-p 1522:1521` y pon `DB_URL=jdbc:oracle:thin:@//localhost:1522/FREEPDB1` |
| SQL Developer: `ORA-12514 ... FREEPDB1 is not registered` | Oracle sigue iniciando | Espera `DATABASE IS READY TO USE!` en `docker logs -f oracle-local` |
| SQL Developer: `ORA-01017 invalid username/password` | El volumen ya existía con otra contraseña, o usuario/contraseña mal escritos | Usa la contraseña con la que se creó la base; ver [docs/04](docs/04-inicializacion-oracle.md#13-errores-frecuentes) |
| `ORA-01920` al ejecutar `00_crear_usuario.sql` | El usuario `OSIN` ya existe | Salta al paso 2.3 |
| `ORA-00955` al ejecutar `01_schema.sql` | Las tablas ya existen | Ejecuta `99_limpiar.sql` como `OSIN` y repite el paso 2.4 |
| El backend falla con `Schema-validation: missing table` o `missing column` | Faltan los scripts o se ejecutaron como otro usuario | Ejecuta el paso 2.4 conectado como `OSIN` |
| El backend falla con `Connection refused` o `IO Error: The Network Adapter could not establish the connection` | Oracle no está corriendo o no está `healthy` | `docker start oracle-local` y espera `healthy` |
| El backend falla con `Port 8080 was already in use` | Otra aplicación usa el 8080 | Ciérrala o usa `PORT=8081` en `.env` |
| El IDE no compila o marca errores en todo el proyecto | El proyecto usa un JDK menor a 17 o no se importó el `pom.xml` | Configura un JDK 17+ en el proyecto y recarga Maven desde el IDE |
| Un cliente web recibe un error de **CORS** | Su origen no está en `CORS_ORIGENES` | Agrega ese origen a `CORS_ORIGENES` y reinicia el backend |
