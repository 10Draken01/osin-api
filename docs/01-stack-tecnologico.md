# 01. Stack tecnológico

Tecnologías, versiones y organización del backend `osinapi`.

## 1. Resumen

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 (compila con `java.version=17`; funciona con JDK 17 o superior) |
| Framework | Spring Boot 4.1.2 (Spring Framework 7) |
| Build | Maven, con Maven Wrapper incluido (`mvnw`, `mvnw.cmd`) |
| Persistencia | Spring Data JPA con Hibernate 7 |
| Base de datos | Oracle Database Free 23ai en Docker |
| Documentación API | SpringDoc OpenAPI 3.2.0 (Swagger UI) |
| Pruebas | JUnit 5, Mockito, MockMvc y Spring Boot Test con H2 en memoria |

## 2. Dependencias (`pom.xml`)

| Dependencia | Para qué se usa |
|---|---|
| `spring-boot-starter-webmvc` | Controladores REST con Tomcat embebido |
| `spring-boot-starter-data-jpa` | Entidades, repositorios, transacciones y `@Version` |
| `spring-boot-starter-validation` | Validaciones en los DTO (`@NotNull`, `@Positive`, `@Size`, ...) |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI y especificación OpenAPI |
| `ojdbc17` | Driver JDBC de Oracle |
| `lombok` | Getters, builders y constructores. Configurado como procesador de anotaciones en `maven-compiler-plugin` |
| `h2` y `spring-boot-h2console` | Base de datos en memoria para las pruebas automáticas (y opcional para desarrollo, ver `application.properties`) |
| `spring-boot-starter-*-test` | JUnit 5, Mockito, MockMvc y soporte de pruebas de JPA y validación |

## 3. Organización del código

```
src/main/java/com/lacomer/osinapi
├── configuration/   OpenApiConfig (datos de Swagger), CorsConfig (acceso desde el frontend)
├── controller/      CrudController (GET, GET por id, POST, PUT genéricos), ProductoController, PedidoController
├── converters/      BooleanToNumberConverter: Boolean <-> NUMBER(1) para la columna ACTIVO
├── docs/            ProblemaDoc: esquema del error que se muestra en Swagger
├── dto/             Requests y responses de la API
├── entities/        Producto, Pedido, PedidoDetalle (aquí están las reglas de negocio)
├── enums/           EstadoPedido y sus transiciones permitidas
├── exceptions/      Excepciones de negocio y GlobalExceptionHandler
├── mapper/          Conversión entre entidades y DTO
├── repositories/    Interfaces de Spring Data JPA
├── services/        Casos de uso; cada método público es una transacción
└── utils/           Validaciones de texto y números reutilizables
```

Responsabilidad de cada capa:

| Capa | Hace | No hace |
|---|---|---|
| `controller` | Recibe la petición, valida el formato (`@Valid`, `@Positive`), llama al servicio y devuelve el código HTTP | Reglas de negocio |
| `service` | Busca las entidades, llama a sus reglas y guarda. Define la transacción (`@Transactional`) | Cálculos de negocio |
| `entities` | Reglas: validar datos del producto, calcular subtotal y total, cambiar de estado, descontar y reponer existencias | Acceso a base de datos |
| `repositories` | Consultas a la base de datos | Lógica |

## 4. Transacciones y concurrencia

Hay dos problemas distintos y cada uno lo resuelve una herramienta distinta. Ninguna requiere código adicional.

### 4.1 Atomicidad: `@Transactional`

`PedidoServiceImpl` y `ProductoServiceImpl` llevan `@Transactional` a nivel de clase. Cada método público corre en una sola transacción de base de datos:

- Si el método termina bien, Spring hace `COMMIT` de todos los cambios (pedido y existencias).
- Si se lanza una excepción (por ejemplo `ConflictoException` por stock insuficiente), Spring hace `ROLLBACK` y la base de datos queda exactamente como estaba.

Así un pedido nunca queda registrado a medias ni con el inventario descontado parcialmente.

### 4.2 Peticiones simultáneas: `@Version` (bloqueo optimista)

`@Transactional` no evita que dos peticiones lean el mismo dato al mismo tiempo. Ejemplo: hay 5 piezas y dos pedidos de 4 se confirman a la vez; los dos leen 5, los dos validan y los dos descuentan.

Para eso `Producto` y `Pedido` tienen un campo `@Version` (columna `VERSION_LOCK`). Hibernate lo maneja solo:

1. Al leer la fila guarda su versión (por ejemplo 3).
2. Al guardar ejecuta `UPDATE ... SET ..., VERSION_LOCK = 4 WHERE ID = ? AND VERSION_LOCK = 3`.
3. Si otra transacción ya cambió la fila, la versión ya no es 3, el `UPDATE` no afecta ninguna fila y Hibernate lanza `ObjectOptimisticLockingFailureException`.
4. Spring hace rollback y `GlobalExceptionHandler` responde **409** con el mensaje "El registro fue modificado por otro usuario. Recarga la informacion e intenta de nuevo."

Casos que cubre:

| Situación | Resultado |
|---|---|
| Dos confirmaciones al mismo tiempo de pedidos con el mismo producto | La primera descuenta; la segunda recibe 409 y no descuenta nada |
| Dos cancelaciones simultáneas del mismo pedido (doble clic) | La primera repone el inventario; la segunda recibe 409. El inventario se repone una sola vez |
| Cancelar y completar el mismo pedido al mismo tiempo | Solo una de las dos se aplica |

## 5. Configuración

`src/main/resources/application.properties` usa variables de entorno con valor por defecto:

| Variable | Por defecto | Uso |
|---|---|---|
| `PORT` | `8080` | Puerto HTTP |
| `DB_URL` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` | URL de Oracle |
| `DB_USER` | `osin` | Usuario dueño del esquema |
| `DB_PASS` | `osin` | Contraseña de ejemplo |
| `CORS_ORIGENES` | `http://localhost:4200` | Orígenes que pueden llamar a la API desde el navegador |

Si existe un archivo `.env` en la raíz del proyecto, Spring Boot lo lee al arrancar (`spring.config.import=optional:file:.env[.properties]`). `.env` está en `.gitignore`; en el repositorio solo va `.env.example`.

Otras propiedades relevantes:

| Propiedad | Valor | Motivo |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `validate` | El esquema lo crean los scripts SQL. Hibernate solo comprueba al arrancar que tablas y columnas coincidan con las entidades |
| `spring.jpa.open-in-view` | `false` | Las consultas solo ocurren dentro de los servicios |
| `hibernate.default_batch_fetch_size` | `50` | Al listar pedidos carga las partidas de varios pedidos en una sola consulta |
| `spring.data.web.pageable.serialization-mode` | `via-dto` | Formato estable del JSON paginado (`content` + `page`) |

| Ambiente | Base de datos | Quién crea las tablas |
|---|---|---|
| Ejecución normal | Oracle | Scripts de `database/oracle` |
| Pruebas (`./mvnw test`, perfil `test`) | H2 en memoria con `MODE=Oracle` | Hibernate (`create-drop`) y `src/test/resources/data.sql` para la secuencia del folio |

## 6. Puertos y rutas

| Servicio | URL |
|---|---|
| API | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Oracle | `localhost:1521`, servicio `FREEPDB1` |
