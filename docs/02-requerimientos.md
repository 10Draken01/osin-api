# 02. Requerimientos

Requerimientos del backend obtenidos del enunciado del examen técnico, cómo se cumplen y qué supuestos se tomaron donde el enunciado no define algo.

## 1. Alcance

**Incluye:** catálogo de productos con inventario, pedidos con una o más partidas, edición de pedidos en borrador, ciclo de vida del pedido (confirmar, cancelar, completar), consulta con filtros y paginación, trazabilidad de fechas, errores de negocio claros, API REST documentada con Swagger y base de datos Oracle con scripts.

**No incluye** (el enunciado lo excluye): autenticación y roles, pagos, impuestos y descuentos, integración con servicios externos, historial de movimientos de inventario.

El frontend Angular es un proyecto aparte que consume esta API.

## 2. Actor

| Actor | Descripción |
|---|---|
| Operador de la tienda | Consulta y administra el catálogo, crea pedidos y cambia su estado. No hay autenticación |

## 3. Ciclo de vida del pedido

```
BORRADOR ──confirmar──▶ CONFIRMADO ──completar──▶ COMPLETADO
    │                       │
    └──cancelar──▶ CANCELADO ◀──cancelar
```

| Estado | Significado | Inventario |
|---|---|---|
| `BORRADOR` | Pedido capturado, todavía no confirmado | No se ha descontado |
| `CONFIRMADO` | Pedido aceptado | Ya se descontó |
| `COMPLETADO` | Compra finalizada. Estado final | Se queda descontado |
| `CANCELADO` | Pedido cancelado. Estado final | Si venía de `CONFIRMADO`, se repuso |

Transiciones (definidas en `EstadoPedido.java`; cualquier otra responde **409**):

| Estado actual | Confirmar | Cancelar | Completar |
|---|---|---|---|
| `BORRADOR` | Sí → `CONFIRMADO`, descuenta inventario | Sí → `CANCELADO`, no mueve inventario | No (409) |
| `CONFIRMADO` | No (409) | Sí → `CANCELADO`, repone inventario | Sí → `COMPLETADO`, no mueve inventario |
| `COMPLETADO` | No (409) | No (409) | No (409) |
| `CANCELADO` | No (409) | No (409) | No (409) |

**Por qué el inventario se repone una sola vez:** `CANCELADO` no tiene transiciones de salida. Una segunda cancelación del mismo pedido se rechaza con 409 antes de tocar el inventario. Si las dos cancelaciones llegan al mismo tiempo, `@Version` hace que la segunda falle con 409 (ver [01-stack-tecnologico.md](01-stack-tecnologico.md), sección 4.2).

**Por qué existe `COMPLETADO`:** `CONFIRMADO` solo indica que el pedido se aceptó y se apartó el inventario; `COMPLETADO` indica que la compra terminó y que ya no se puede cancelar.

## 4. Casos de uso

| ID | Caso de uso | Endpoint |
|---|---|---|
| CU-01 | Consultar catálogo | `GET /api/productos`, `GET /api/productos/activos`, `GET /api/productos/{id}` |
| CU-02 | Registrar y actualizar producto | `POST /api/productos`, `PUT /api/productos/{id}` |
| CU-03 | Activar / desactivar producto | `PATCH /api/productos/{id}/activar`, `PATCH /api/productos/{id}/desactivar` |
| CU-04 | Crear pedido | `POST /api/pedidos` |
| CU-05 | Consultar pedidos con filtros | `GET /api/pedidos?estado=&folio=&pagina=&tamano=` |
| CU-06 | Consultar detalle de pedido | `GET /api/pedidos/{id}` |
| CU-07 | Confirmar pedido | `POST /api/pedidos/{id}/confirmar` |
| CU-08 | Cancelar pedido | `POST /api/pedidos/{id}/cancelar` |
| CU-09 | Completar pedido | `POST /api/pedidos/{id}/completar` |
| CU-10 | Actualizar pedido en borrador | `PUT /api/pedidos/{id}` |

### CU-04 Crear pedido

| | |
|---|---|
| Entrada | `{ "productos": [ { "id": 5, "cantidad": 2 }, { "id": 3, "cantidad": 1 } ] }`. Solo producto y cantidad; precios o totales enviados por el cliente no existen en el DTO |
| Validaciones | Lista con al menos un producto (400). `id` y `cantidad` obligatorios, cantidad entera mayor a 0 (400). Un producto no se repite en el mismo pedido (400). El producto existe y está activo (404) |
| Proceso | Obtiene el siguiente número de `SEQ_PEDIDOS_FOLIO` y arma el folio `PED-000001`. Por cada partida toma el precio vigente del producto. Guarda el pedido en `BORRADOR` |
| Salida | 201 con folio, partidas (cantidad, precio unitario, subtotal), total y fechas |
| Inventario | **No cambia.** Se descuenta al confirmar |

### CU-10 Actualizar pedido en borrador

| | |
|---|---|
| Precondición | Pedido en `BORRADOR` (`EstadoPedido.actualizable`) |
| Entrada | El mismo cuerpo que CU-04: `{ "productos": [ { "id": 5, "cantidad": 3 } ] }`. Es la lista completa de partidas, no un cambio parcial |
| Validaciones | Las de CU-04 (400 y 404). Pedido inexistente: 404. Estado distinto de `BORRADOR`: 409, revisado antes de buscar los productos |
| Proceso (una transacción) | Las partidas cuyo producto sigue se actualizan (cantidad y precio); las que ya no vienen se eliminan; las nuevas se agregan. **Todas** quedan con el precio vigente del catálogo. Folio y fecha de creación no cambian; se actualiza `FECHA_ACTUALIZACION` |
| Inventario | **No cambia** (un borrador nunca ha descontado) |
| Concurrencia | `@Version` de `Pedido`: si otro usuario lo confirma o edita al mismo tiempo, una de las dos peticiones recibe 409 |
| Detalle técnico | Las partidas se ajustan en su lugar en vez de borrarlas y crearlas de nuevo: Hibernate inserta antes de borrar al hacer flush y reinsertar el mismo producto violaría `UQ_PEDIDOS_DETALLES_PEDIDO_PRODUCTO` |

### CU-07 Confirmar pedido

| | |
|---|---|
| Precondición | Pedido en `BORRADOR` |
| Proceso (una transacción) | 1. Valida que **todas** las partidas tengan existencia suficiente. 2. Solo si todas alcanzan, descuenta cada una. 3. Cambia el estado a `CONFIRMADO` y actualiza `FECHA_ACTUALIZACION` |
| Errores | Pedido inexistente: 404. Estado distinto de `BORRADOR`: 409. Stock insuficiente: 409 con el producto, lo disponible y lo solicitado; rollback, ningún producto cambia y el pedido sigue en `BORRADOR` |

### CU-08 Cancelar pedido

| | |
|---|---|
| Precondición | Pedido en `BORRADOR` o `CONFIRMADO` |
| Proceso (una transacción) | Si estaba `CONFIRMADO`, suma a cada producto la cantidad de su partida. Cambia el estado a `CANCELADO` y registra `FECHA_CANCELACION` y `FECHA_ACTUALIZACION` |
| Errores | Pedido inexistente: 404. Pedido `CANCELADO` o `COMPLETADO`: 409 sin tocar el inventario |

### CU-09 Completar pedido

| | |
|---|---|
| Precondición | Pedido en `CONFIRMADO` |
| Proceso | Cambia el estado a `COMPLETADO` y actualiza `FECHA_ACTUALIZACION`. No mueve inventario |
| Errores | Pedido inexistente: 404. Otro estado: 409 |

## 5. Requerimientos funcionales

| ID | Requerimiento | Dónde se cumple |
|---|---|---|
| RF-01 | Producto con id, nombre, descripción opcional, precio unitario mayor a 0 y existencia no negativa | `ProductoRequest` (validaciones), `Producto.validarDatos`, `CHECK` en `PRODUCTOS` |
| RF-02 | Crear y actualizar productos; catálogo inicial por script | `ProductoController`, `02_data.sql` |
| RF-03 | Consultar productos (todos paginados, activos, por id) | `ProductoController` |
| RF-04 | Crear pedido con una o más partidas | `PedidoServiceImpl.registrar` |
| RF-05 | Subtotal y total calculados en el backend con el precio del producto | `PedidoDetalle.getSubtotal`, `Pedido.calcularTotal` |
| RF-06 | La partida guarda el precio del producto al crear el pedido o al editarlo en `BORRADOR`; después de confirmar ya no cambia | `PEDIDOS_DETALLES.PRECIO_UNITARIO` |
| RF-07 | No se confirma si alguna partida no tiene existencia suficiente, sin cambios parciales | `Pedido.confirmar` + `@Transactional` |
| RF-08 | Confirmar descuenta el inventario | `Pedido.confirmar` |
| RF-09 | Cancelar un pedido confirmado repone el inventario exactamente una vez | `Pedido.cancelar`, `EstadoPedido`, `@Version` |
| RF-10 | Transiciones de estado inválidas se rechazan con 409 | `EstadoPedido.puedeCambiarA`, `Pedido.validarCambioEstadoPedido` |
| RF-11 | Listado con folio, fecha, estado y total; filtros por estado y folio | `PedidoServiceImpl.listar` (`Specification`) |
| RF-12 | Detalle del pedido con sus partidas | `GET /api/pedidos/{id}` |
| RF-13 | Fecha de creación, actualización y cancelación | Columnas `FECHA_*` de `PEDIDOS` |
| RF-14 | Folio único y generado automáticamente | `SEQ_PEDIDOS_FOLIO` + `UQ_PEDIDOS_FOLIO` |
| RF-15 | Errores de negocio claros | `GlobalExceptionHandler` (sección 7) |
| RF-16 | API documentada | Swagger UI en `/swagger-ui.html` |
| RF-17 | Baja lógica de productos para no afectar pedidos existentes | Columna `ACTIVO`, endpoints activar/desactivar |
| RF-18 | Editar las partidas de un pedido mientras está en `BORRADOR`, con precios vigentes | `Pedido.actualizarPartidas`, `PUT /api/pedidos/{id}` |

## 6. Requerimientos no funcionales

| ID | Requerimiento | Cómo se cumple |
|---|---|---|
| RNF-01 | Operaciones críticas atómicas | `@Transactional` en los servicios |
| RNF-02 | Peticiones simultáneas no dejan inventario inconsistente | `@Version` en `Producto` y `Pedido` |
| RNF-03 | Integridad en la base de datos | PK, FK, `UNIQUE`, `NOT NULL` y `CHECK` (documento 03) |
| RNF-04 | Persistencia real | Oracle; los datos se conservan al reiniciar la aplicación |
| RNF-05 | Modelo normalizado | 3FN (documento 03) |
| RNF-06 | Sin secretos en el repositorio | Variables de entorno; `.env` ignorado por Git |
| RNF-07 | Sin trazas en las respuestas | `GlobalExceptionHandler` responde mensajes controlados; el detalle técnico solo va al log |
| RNF-08 | Validación en el servidor | Bean Validation en DTO + validaciones en las entidades |
| RNF-09 | Reproducible | README y scripts probados en el orden documentado |
| RNF-10 | Pruebas automáticas de los escenarios críticos | Documento 05 |

## 7. Errores y códigos HTTP

Formato `ProblemDetail` (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Stock insuficiente para el producto 'Cafe 250 g': disponible 40, solicitado 41",
  "instance": "/api/pedidos/5/confirmar"
}
```

En errores de validación se agrega `"errores": ["productos[0].cantidad: La cantidad debe ser mayor a 0"]`.

| Situación | HTTP | Ejemplo de `detail` |
|---|---|---|
| Cuerpo inválido (lista vacía, cantidad ≤ 0, precio ≤ 0, nombre vacío) | 400 | `Los datos enviados no son válidos.` + `errores` |
| Producto repetido en el pedido | 400 | `El producto con id 5 esta repetido en el pedido` |
| JSON mal formado | 400 | `El cuerpo de la peticion no es un JSON valido.` |
| Parámetro fuera de rango (id negativo, `pagina` negativa, `tamano` mayor a 100) | 400 | `Uno o más parámetros no son válidos.` + `errores` |
| Parámetro con tipo incorrecto (`estado=INVENTADO`) | 400 | `El valor 'INVENTADO' no es valido para el parametro estado` |
| Producto inexistente o inactivo al crear pedido | 404 | `No se encontro un producto activo con id 99` |
| Pedido inexistente | 404 | `Pedido no encontrado con id: 99` |
| Stock insuficiente | 409 | `Stock insuficiente para el producto 'X': disponible N, solicitado M` |
| Editar un pedido que no está en `BORRADOR` | 409 | `El pedido no se puede modificar porque esta en estado Confirmado` |
| Transición no permitida | 409 | `El pedido no puede cambiar a Confirmado porque esta en estado Cancelado` |
| Nombre de producto duplicado | 409 | `La operacion viola una restriccion de datos (por ejemplo, un registro duplicado).` |
| Otra petición modificó el mismo registro | 409 | `El registro fue modificado por otro usuario. Recarga la informacion e intenta de nuevo.` |
| Error no previsto | 500 | `Error interno del servidor. Contacte al administrador.` |

## 8. Supuestos

| # | Supuesto | Motivo |
|---|---|---|
| S-01 | El inventario se descuenta al **confirmar**, no al crear | La regla 3 del enunciado habla de no *confirmar* sin existencia y la regla 5 de reponer al cancelar un *confirmado* |
| S-02 | Una partida por producto en cada pedido | Evita sumar dos veces el mismo producto; `UQ_PEDIDOS_DETALLES_PEDIDO_PRODUCTO` |
| S-03 | Cantidades enteras | Los productos se venden por pieza |
| S-04 | Moneda única (MXN), sin impuestos ni descuentos. Total = suma de subtotales | El enunciado no los pide |
| S-05 | Un pedido en `BORRADOR` se puede editar (reemplazar partidas); en cualquier otro estado no | Corregir un pedido antes de confirmarlo sin cancelar y perder el folio. Las partidas toman el precio vigente porque el borrador todavía no es un compromiso de venta |
| S-06 | Los productos no se borran, se desactivan | Un producto puede estar en pedidos existentes (FK) |
| S-07 | El nombre del producto es único | Evita productos duplicados en el catálogo |
| S-08 | Subtotal y total no se guardan en la base de datos | Son datos derivados (3FN); los calcula el backend al responder |
| S-09 | La existencia se guarda como saldo actual en `PRODUCTOS` | Un historial de movimientos está fuera del alcance |

## 9. Escenarios de aceptación

Datos de `02_data.sql`. Los ids de producto son los que asigna Oracle en una base limpia; si cambian, consúltalos con `GET /api/productos`.

| ID | Escenario | Pasos | Resultado esperado |
|---|---|---|---|
| A | Pedido con dos o más productos | Crear pedido con Leche 1 L (id 5) × 2 y Aceite 1 L (id 3) × 1 | 201; subtotales 54.00 y 45.00; total 99.00; estado `BORRADOR`; inventario sin cambios |
| B | Stock insuficiente | Crear pedido con Café 250 g (id 6) × 41 (hay 40) y confirmarlo | 409 de stock insuficiente; Café sigue en 40; el pedido sigue en `BORRADOR` |
| C | Confirmar pedido válido | Confirmar el pedido de A | `CONFIRMADO`; Leche 118 y Aceite 59 |
| D | Cancelar pedido confirmado | Cancelar el pedido de C y volver a cancelarlo | Primera vez `CANCELADO`, Leche 120 y Aceite 60. Segunda vez 409 y existencias sin cambio |
| E | Transición no permitida | Confirmar `PED-000003` (cancelado) | 409 con mensaje claro |
| F | Persistencia | Detener backend y Oracle, volver a iniciarlos y consultar | Pedidos, estados e inventario iguales |

Pasos detallados en [05-pruebas.md](05-pruebas.md).

## 10. Trazabilidad con el enunciado

| Regla del enunciado | Requerimientos |
|---|---|
| 1. Catálogo de productos | RF-01, RF-02, RF-03, RF-17 |
| 2. Creación de pedido | RF-04, RF-05, RF-06 |
| 3. Validación de inventario | RF-07, RF-08, RNF-01, RNF-02 |
| 4. Ciclo de vida | RF-10, sección 3 |
| 5. Cancelación | RF-09 |
| 6. Consulta y seguimiento | RF-11, RF-12 |
| 7. Trazabilidad mínima | RF-13, RF-14 |
| 8. Errores de negocio | RF-15, sección 7 |
