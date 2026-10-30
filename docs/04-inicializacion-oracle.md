# OSINAPI — Inicialización de Oracle Database Free

Esta guía deja Oracle funcionando en Docker y permite administrar el esquema desde **Oracle SQL Developer**. Los scripts SQL se ejecutan desde SQL Developer; no es necesario copiarlos al contenedor ni crear carpetas dentro de él.

## 1. Requisitos

- Docker instalado y funcionando.
- Oracle SQL Developer instalado.
- Puerto `1521` disponible.
- Conexión a Internet para descargar la imagen de Oracle.

## 2. Descargar la imagen de Oracle

Desde WSL o una terminal con Docker:

```bash
docker pull container-registry.oracle.com/database/free:latest-lite
```

## 3. Crear el contenedor

Ejecuta este comando una sola vez:

```bash
docker run -d \
  --name oracle-local \
  -p 1521:1521 \
  -e ORACLE_PWD=Oracle_2026 \
  -v oracle-data:/opt/oracle/oradata \
  container-registry.oracle.com/database/free:latest-lite
```

- `oracle-local`: nombre del contenedor.
- `1521`: puerto de conexión.
- `ORACLE_PWD`: contraseña inicial de SYS y SYSTEM cuando se crea una base de datos nueva.
- `oracle-data`: volumen persistente donde Oracle almacena los datos.

> Si ya existe el contenedor `oracle-local`, no ejecutes de nuevo `docker run`. Inícialo con `docker start oracle-local`.

## 4. Esperar a que Oracle esté listo

```bash
docker logs -f oracle-local
```

Espera hasta que aparezca:

```text
DATABASE IS READY TO USE!
```

Pulsa `Ctrl + C` para salir de los logs. Esto no detiene el contenedor.

Comprueba el estado:

```bash
docker ps
```

También puedes consultar el estado de salud:

```bash
docker inspect --format='{{.State.Health.Status}}' oracle-local
```

Cuando el contenedor termine de iniciar, debe indicar `healthy`.

## 5. Iniciar y detener Oracle posteriormente

Iniciar un contenedor existente:

```bash
docker start oracle-local
```

Detenerlo:

```bash
docker stop oracle-local
```

Consultar los logs:

```bash
docker logs -f oracle-local
```

**No uses `docker rm -v` ni elimines el volumen `oracle-data`** si quieres conservar la base de datos.

## 6. Conectarse desde Oracle SQL Developer como SYS

Crea una conexión nueva con estos datos:

| Campo | Valor |
|---|---|
| Connection Name | `SYS_LOCAL` |
| Username | `system` |
| Password | `Oracle_2026` |
| Connection Type | `Basic` |
| Hostname | `localhost` |
| Port | `1521` |
| Connection role | `SYSDBA` |
| Connection identifier | `Service name` |
| Service name | `FREEPDB1` |

Pulsa **Test**. Debe indicar `Success`. Después pulsa **Connect**.

> Si reutilizas un volumen de una instalación anterior, la contraseña SYS puede ser distinta de la indicada en `ORACLE_PWD`. La variable `ORACLE_PWD` establece la contraseña durante la inicialización inicial; no garantiza que se cambie en una base de datos ya existente.

## 7. Crear el usuario OSIN

Con la conexión `SYS_LOCAL` abierta, abre un **SQL Worksheet** y ejecuta el contenido de:

`database/oracle/00_crear_usuario.sql`

En SQL Developer, usa **Run Script (F5)**.

El script ejecuta:

```sql
ALTER SESSION SET "_oracle_script" = TRUE;
CREATE USER osin IDENTIFIED BY osin;
GRANT ALL PRIVILEGES TO osin;
commit;
```

El usuario `OSIN` será propietario de las tablas, índices, secuencia y vista del proyecto.

> El comando `/ as sysdba` corresponde a una conexión local de SQL*Plus. En SQL Developer, el equivalente es crear la conexión con usuario `SYS` y seleccionar el rol `SYSDBA`. No escribas `/ as sysdba` dentro del Worksheet.

## 8. Crear una conexión normal como OSIN

Crea otra conexión en SQL Developer:

| Campo | Valor |
|---|---|
| Connection Name | `OSIN_LOCAL` |
| Username | `OSIN` |
| Password | `osin` |
| Connection Type | `Basic` |
| Hostname | `localhost` |
| Port | `1521` |
| Connection role | `Default` |
| Connection identifier | `Service name` |
| Service name | `FREEPDB1` |

Pulsa **Test** y después **Connect**.

A partir de aquí, ejecuta los scripts restantes usando esta conexión, no la conexión SYS.

## 9. Ejecutar los scripts del esquema

En la conexión `OSIN_LOCAL`, abre cada archivo desde **File > Open** y ejecútalo con **Run Script (F5)**, en este orden:

1. `database/oracle/01_schema.sql` — crea tablas, restricciones, índices, secuencia y vista.
2. `database/oracle/02_data.sql` — inserta los datos de prueba.
3. `database/oracle/03_verificacion.sql` — consulta los datos y valida los objetos.

Es importante respetar el orden. Ejecuta cada archivo una vez y revisa la pestaña **Script Output** para confirmar que no hubo errores.

## 10. Consultar los datos manualmente

Conectado como `OSIN`, abre un Worksheet y ejecuta:

```sql
SELECT * FROM producto;
SELECT * FROM pedido;
SELECT * FROM pedido_detalle;
SELECT * FROM vw_pedido_resumen;
```

## 11. Limpiar el esquema

`99_limpiar.sql` elimina la vista, tablas y secuencia del usuario `OSIN`. Ejecútalo únicamente desde la conexión `OSIN_LOCAL` y solo si quieres borrar los objetos para volver a crear el esquema.

Este script **no elimina el usuario OSIN** ni el contenedor Docker ni el volumen de Oracle.

Después de limpiarlo, puedes volver a ejecutar `01_schema.sql`, `02_data.sql` y `03_verificacion.sql`.

## 12. Configuración para Spring Boot

Si la aplicación se ejecuta desde Windows o WSL y Oracle publica el puerto `1521`:

```properties
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
spring.datasource.username=osin
spring.datasource.password=osin
spring.jpa.hibernate.ddl-auto=validate
```
Para el env usar el ejemplo .env.example
```properties
PORT=8080
DB_URL=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
DB_USER=osin
DB_PASS=osin
```

El esquema se administra mediante los scripts SQL. Hibernate debe validar el esquema, no crearlo.

## 13. Errores frecuentes

### ORA-12514: service FREEPDB1 is not registered

Oracle todavía está iniciando o el servicio aún no se ha registrado. Espera a que los logs muestren `DATABASE IS READY TO USE!` y vuelve a probar.

### ORA-01017 al conectar como SYS

Comprueba que la contraseña corresponda a la base de datos inicializada. Si el volumen `oracle-data` ya existía, puede conservar una contraseña SYS anterior a la indicada en `ORACLE_PWD`.

### ORA-01920 al crear OSIN

El usuario ya existe o hay un conflicto de nombre. No vuelvas a ejecutar el script de creación sin comprobar primero el estado del usuario.

### ORA-00955 al crear objetos

Ya existe un objeto con ese nombre en el esquema. Si quieres reconstruir el esquema, ejecuta primero `99_limpiar.sql` como `OSIN` y después vuelve a ejecutar los scripts en orden.

### No aparecen las tablas en SQL Developer

Haz clic derecho sobre **Tables** y selecciona **Refresh**. Confirma que estás conectado como `OSIN`, no como `SYS`.

---

## Resumen del flujo

1. Descargar la imagen.
2. Crear el contenedor una sola vez.
3. Esperar a `DATABASE IS READY TO USE!`.
4. Conectar SQL Developer como `SYS` con rol `SYSDBA`.
5. Ejecutar `00_crear_usuario.sql`.
6. Conectar SQL Developer como `OSIN`.
7. Ejecutar `01_schema.sql`.
8. Ejecutar `02_data.sql`.
9. Ejecutar `03_verificacion.sql`.

Los archivos SQL se ejecutan directamente desde Oracle SQL Developer. No es necesario copiarlos al sistema de archivos del contenedor.
