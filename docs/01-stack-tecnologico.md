# 01. Stack tecnológico

Este documento fija las tecnologías, versiones y convenciones técnicas del proyecto. Las versiones exactas las determina el BOM de Spring Boot al generar el proyecto en [start.spring.io](https://start.spring.io); lo marcado como "referencial" debe confirmarse ahí.

## 1. Resumen

| Capa | Tecnología |
|---|---|
| Frontend | Angular 19 + Angular Material 19, TypeScript 5.7, RxJS 7.8 |
| Backend | Java 17, Spring Boot 4, Maven |
| Persistencia | Spring Data JPA (Hibernate), Oracle (oficial) y H2 (desarrollo) |
| Documentación API | SpringDoc OpenAPI (Swagger UI) |
| Pruebas | JUnit 5 + Spring Boot Test (backend), Jasmine + Karma (frontend) |
| Infraestructura local | Docker (WSL2 o Ubuntu) para Oracle |

## 2. Versiones

| Componente | Versión | Observación |
|---|---|---|
| JDK | 17 (LTS) | Línea base de Spring Boot 4 |
| Maven | 3.9+ | Usar el wrapper `mvnw` incluido en el proyecto |
| Spring Boot | 4.0.x | Basado en Spring Framework 7 (referencial) |
| Hibernate ORM | 7.x | Lo trae `spring-boot-starter-data-jpa`; detecta el dialecto de Oracle solo |
| Servidor embebido | Tomcat (el de Spring Boot 4) | Lo trae el starter web |
| SpringDoc OpenAPI | 3.x | Es la línea compatible con Spring Boot 4 (confirmar en Maven Central) |
| Lombok | gestionada por Spring Boot | |
| Oracle JDBC | `ojdbc11` gestionada por Spring Boot | Es lo que agrega Initializr con "Oracle Driver" |
| H2 | 2.x gestionada por Spring Boot | |
| Node.js | 20 LTS (>= 20.11.1) o 22 LTS | Requisito de Angular 19 |
| Angular CLI | 19.x | |
| Oracle Database Free | `free:latest-lite` | Imagen oficial de Oracle Container Registry |

## 3. Backend

### 3.1 Dependencias elegidas

| Dependencia (nombre en Initializr) | Para qué se usa en el proyecto |
|---|---|
| Spring Web | Controladores REST (Spring MVC) con Tomcat embebido |
| Spring Data JPA | Entidades, repositorios y bloqueos (`@Lock`, `@Version`) |
| Validation | Bean Validation (`@NotNull`, `@Positive`, `@Size`, ...) en DTOs |
| Lombok | Reducir getters, setters y constructores |
| SpringDoc OpenAPI Web | Swagger UI y especificación OpenAPI |
| Oracle Driver | Conexión al motor oficial |
| H2 Database | Perfil de desarrollo sin contenedor |

Además, de forma estándar: `spring-boot-starter-test` (y los módulos de prueba que genere Initializr).

### 3.2 Fragmento de `pom.xml` (referencial)

En Spring Boot 4 los starters se modularizaron y cambiaron algunos nombres (por ejemplo, el starter web ahora se llama `spring-boot-starter-webmvc`). Initializr genera los nombres correctos; este fragmento solo orienta:

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.0.x</version> <!-- usar la versión que entregue Initializr -->
</parent>

<properties>
  <java.version>17</java.version>
</properties>

<dependencies>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
  <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>3.x</version></dependency>
  <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
  <dependency><groupId>com.oracle.database.jdbc</groupId><artifactId>ojdbc11</artifactId><scope>runtime</scope></dependency>
  <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
</dependencies>
```

### 3.3 Organización de paquetes

```
com.<organizacion>.pedidos
├── config/          # OpenAPI, CORS
├── controller/      # ProductoController, PedidoController
├── service/         # ProductoService, PedidoService (reglas de negocio y @Transactional)
├── repository/      # ProductoRepository, PedidoRepository
├── domain/          # Producto, Pedido, PedidoDetalle, EstadoPedido (enum)
├── dto/             # request/response; los importes viven solo en las respuestas
└── exception/       # excepciones de negocio + @RestControllerAdvice
```

Regla: los controladores no contienen lógica; toda regla de negocio y toda transacción viven en `service`.

### 3.4 Perfiles de base de datos

| Perfil | Base de datos | Esquema | Cuándo usarlo |
|---|---|---|---|
| `h2` (por defecto) | H2 en archivo `./data/pedidos` con `MODE=Oracle` | Hibernate `ddl-auto=update` | Desarrollo rápido, sin contenedor |
| `oracle` | Oracle en Docker | Scripts SQL, Hibernate `ddl-auto=validate` | Pruebas de escenarios, demo y entrega |

Se usa H2 **en archivo y no en memoria** para que los datos sobrevivan al reinicio (escenario F).

`application-oracle.properties` (ejemplo):

```properties
spring.datasource.url=${DB_URL:jdbc:oracle:thin:@//localhost:1521/FREEPDB1}
spring.datasource.username=${DB_USER:pedidos}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

`application-h2.properties` (ejemplo):

```properties
spring.datasource.url=jdbc:h2:file:./data/pedidos;MODE=Oracle;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
```

Nota: en Spring Boot 4 la autoconfiguración quedó repartida en módulos; si la consola web de H2 no aparece, hay que agregar la dependencia que indique la guía de migración de Spring Boot 4.

### 3.5 Concurrencia y transacciones

- Confirmar y cancelar se ejecutan en un método `@Transactional` del servicio.
- Los productos de un pedido se bloquean con `@Lock(PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`), **ordenados por `id`** para evitar interbloqueos entre dos pedidos que comparten productos.
- `@Version` en `Producto` como segunda defensa (bloqueo optimista).
- La cancelación comprueba el estado dentro de la misma transacción: si ya está `CANCELADO` responde 409 y no toca el inventario.

### 3.6 Puertos y rutas

| Servicio | Valor |
|---|---|
| Backend | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Frontend | `http://localhost:4200` |
| Oracle | `localhost:1521` |

## 4. Frontend

### 4.1 Dependencias (según el `package.json` definido)

| Paquete | Versión | Uso |
|---|---|---|
| `@angular/core`, `common`, `compiler`, `platform-browser`, `platform-browser-dynamic` | ^19.1.0 | Núcleo |
| `@angular/router` | ^19.1.0 | Navegación entre catálogo, pedidos y detalle |
| `@angular/forms` | ^19.1.0 | Reactive Forms y `FormArray` para las partidas |
| `@angular/material` y `@angular/cdk` | ^19.2.19 | Tablas, formularios, diálogos, snackbars |
| `@angular/animations` | ^19.1.0 | Requerido por Material |
| `rxjs` | ~7.8.0 | Flujos HTTP |
| `zone.js`, `tslib` | ~0.15.0, ^2.3.0 | Soporte de Angular |
| `typescript` | ~5.7.2 | Lenguaje |
| `@angular/cli`, `@angular-devkit/build-angular`, `@angular/compiler-cli` | ^19.x | Compilación |
| `karma`, `karma-chrome-launcher`, `karma-coverage`, `karma-jasmine`, `karma-jasmine-html-reporter`, `jasmine-core`, `@types/jasmine` | ver `package.json` | Pruebas unitarias |

### 4.2 Organización

```
src/app
├── core/        # servicios HTTP, interceptor de errores, modelos
├── features/
│   ├── productos/   # catálogo y alta/edición
│   └── pedidos/     # formulario, listado, detalle
└── shared/      # componentes y pipes comunes
```

### 4.3 Convenciones

- Componentes standalone.
- Formularios reactivos con validaciones (cantidad entera mayor a 0, producto obligatorio).
- La URL del API sale de `environment.ts`; en desarrollo se usa `proxy.conf.json` hacia `http://localhost:8080` para evitar problemas de CORS.
- Un interceptor HTTP traduce los errores del backend a mensajes con `MatSnackBar`; nunca se muestran trazas técnicas.
- El resumen de importes del formulario es **preliminar**; el total definitivo lo devuelve el backend.

## 5. Calidad y pruebas

| Nivel | Herramienta | Qué cubre |
|---|---|---|
| Unitarias backend | JUnit 5 + Mockito | Reglas de estados, cálculo de importes |
| Integración backend | Spring Boot Test + H2 | Stock insuficiente sin cambios parciales, doble cancelación, transición inválida |
| Unitarias frontend | Jasmine + Karma | Servicios y validaciones de formularios |

## 6. Riesgos y decisiones a vigilar

| Riesgo | Mitigación |
|---|---|
| El enunciado acepta **MySQL u Oracle**; H2 no está entre los motores aceptados | H2 se documenta solo como perfil de desarrollo. Los escenarios A–F, el video y los scripts de la entrega se hacen contra Oracle |
| Spring Boot 4 es reciente: cambian nombres de starters y módulos | Generar el proyecto con Initializr y no copiar coordenadas de memoria |
| El comportamiento de bloqueos puede diferir entre H2 y Oracle | Validar concurrencia y escenario B contra Oracle |
| Los scripts SQL se ejecutan "tal cual" en la evaluación | Probarlos desde cero en un contenedor limpio, en el orden documentado |
| Contraseñas en el repositorio | Solo variables de entorno y valores de ejemplo no sensibles |
