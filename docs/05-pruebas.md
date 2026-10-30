# 05. Pruebas

## 1. Pruebas automáticas

Desde la raíz del proyecto:

```bash
./mvnw test          # Linux, macOS o WSL
mvnw.cmd test        # Windows (CMD o PowerShell)
```

No necesitan Oracle ni variables de entorno. Usan el perfil `test` (`src/test/resources/application-test.properties`):

- H2 en memoria en modo Oracle (`jdbc:h2:mem:pedidostest;MODE=Oracle`).
- Hibernate crea las tablas a partir de las entidades (`ddl-auto=create-drop`).
- `src/test/resources/data.sql` crea la secuencia `SEQ_PEDIDOS_FOLIO`.

Al terminar, Maven muestra `Tests run: N, Failures: 0, Errors: 0` y `BUILD SUCCESS`. Los reportes quedan en `target/surefire-reports`.

### 1.1 Qué cubre cada clase

| Clase | Tipo | Qué prueba |
|---|---|---|
| `enums/EstadoPedidoTest` | Unitaria | La tabla de transiciones de estado |
| `entities/ProductoTest` | Unitaria | Validaciones de producto (nombre, precio > 0, existencia ≥ 0), descripción opcional, descontar y reponer existencias, activar/desactivar |
| `entities/PedidoTest` | Unitaria | Subtotales y total, confirmar, stock insuficiente sin cambios parciales, cancelar una sola vez, cancelar borrador, completar, transiciones inválidas |
| `services/PedidoServiceImplTest` | Unitaria con Mockito (repositorios simulados) | Crear no descuenta, folio, 404, productos repetidos, no se guarda nada cuando falla, confirmar, cancelar, completar |
| `controller/PedidoControllerTest` | Web con MockMvc (controlador y `GlobalExceptionHandler` reales, servicio simulado) | Códigos 201, 200, 400, 404, 409 y 500; mensajes de error sin detalles técnicos; filtros y paginación |
| `integration/PedidosIntegrationTest` | Integración (Spring completo + H2) | Escenarios A a E con commit y rollback reales, folio único, filtros, `@Version` |
| `OsinapiApplicationTests` | Integración | La aplicación arranca y el mapeo JPA es válido |

### 1.2 Escenarios del enunciado

| Escenario | Prueba automática |
|---|---|
| A. Pedido con 2 o más productos e importes | `PedidoTest.calculaSubtotalesYTotal`, `PedidosIntegrationTest.escenarioA_creaPedidoConDosProductosYCalculaImportes` |
| B. Stock insuficiente sin cambios parciales | `PedidoTest.stockInsuficienteRechazaTodoElPedidoSinModificarNingunProducto`, `PedidosIntegrationTest.escenarioB_*` |
| C. Confirmar descuenta inventario | `PedidosIntegrationTest.escenarioC_confirmarDescuentaInventarioYCambiaEstado` |
| D. Cancelar repone una sola vez | `PedidosIntegrationTest.escenarioD_cancelarPedidoConfirmadoReponeInventarioUnaSolaVez`, `PedidoTest.cancelarDosVecesNoDuplicaElInventario` |
| E. Transición no permitida | `PedidosIntegrationTest.escenarioE_*`, `PedidoControllerTest.transicionNoPermitidaResponde409` |
| F. Persistencia tras reiniciar | Manual (sección 2) |

`PedidosIntegrationTest` no es `@Transactional`: cada llamada al servicio hace su propio commit o rollback, igual que en producción. Así se comprueba que cuando una operación falla, la base de datos queda sin cambios.
### 1.3 Pruebas de los controladores

Las dos clases usan `MockMvcBuilders.standaloneSetup`: se levanta solo el controlador real con el `GlobalExceptionHandler` real, y el servicio se simula con Mockito. Así se prueba lo que le corresponde al controlador (rutas, validación de la entrada, código HTTP y formato del error) sin base de datos.

**`controller/PedidoControllerTest`**

| Prueba | Petición | Resultado esperado |
|---|---|---|
| `crearPedidoValidoResponde201ConFolioYTotal` | `POST /api/pedidos` con dos productos | 201 con `folio`, `estado: BORRADOR` y `total` |
| `crearPedidoSinProductosResponde400` | `POST /api/pedidos` con `{"productos":[]}` | 400 con la lista `errores` |
| `crearPedidoConCantidadCeroResponde400` | `POST /api/pedidos` con `cantidad: 0` | 400 |
| `crearPedidoConProductoInexistenteResponde404` | `POST /api/pedidos` con un producto que no existe | 404 con el mensaje del servicio |
| `pedidoNoEncontradoResponde404` | `GET /api/pedidos/99` | 404 |
| `confirmarValidoResponde200` | `POST /api/pedidos/1/confirmar` | 200 con `estado: CONFIRMADO` |
| `confirmarConStockInsuficienteResponde409ConMensajeClaro` | `POST /api/pedidos/1/confirmar` sin stock | 409 con el producto, lo disponible y lo solicitado |
| `transicionNoPermitidaResponde409` | `POST /api/pedidos/1/cancelar` de un pedido ya cancelado | 409 |
| `errorInesperadoResponde500SinDetallesTecnicos` | El servicio lanza un error no previsto | 500 con mensaje genérico, sin el error técnico |
| `completarConfirmadoResponde200` | `POST /api/pedidos/1/completar` | 200 con `estado: COMPLETADO` |
| `jsonMalFormadoResponde400` | `POST /api/pedidos` con un JSON incompleto | 400: `El cuerpo de la peticion no es un JSON valido.` |
| `listarPasaLosFiltrosYLaPaginaAlServicio` | `GET /api/pedidos?estado=CONFIRMADO&folio=PED-0000&pagina=1&tamano=5` | 200; el servicio recibe los filtros y la página 1 de 5, del más reciente al más antiguo |
| `listarSinParametrosUsaPagina0Tamano20` | `GET /api/pedidos` | 200; el servicio recibe la página 0 de 20 |
| `listarConEstadoInexistenteResponde400` | `GET /api/pedidos?estado=INVENTADO` | 400: `El valor 'INVENTADO' no es valido para el parametro estado` |

**`controller/ProductoControllerTest`**

| Prueba | Petición | Resultado esperado |
|---|---|---|
| `listarSinParametrosUsaPagina0Tamano20OrdenadoPorId` | `GET /api/productos` | 200; el servicio recibe la página 0 de 20 ordenada por id |
| `listarPasaPaginaYTamanoAlServicio` | `GET /api/productos?pagina=2&tamano=5` | 200; el servicio recibe la página 2 de 5 |
| `listarActivosResponde200` | `GET /api/productos/activos` | 200 con la lista de productos activos |
| `obtenerPorIdResponde200` | `GET /api/productos/5` | 200 con precio y estado activo |
| `obtenerPorIdInexistenteResponde404` | `GET /api/productos/99` | 404: `Producto no encontrado con id: 99` |
| `registrarProductoValidoResponde201` | `POST /api/productos` con todos los datos | 201 |
| `registrarSinDescripcionEsValido` | `POST /api/productos` sin `descripcion` y con existencia 0 | 201 (la descripción es opcional y la existencia puede ser 0) |
| `registrarConPrecioCeroResponde400` | `POST /api/productos` con `precioUnitario: 0` | 400: `precioUnitario: El precio unitario debe ser mayor a 0`; el servicio no se llama |
| `registrarConExistenciaNegativaResponde400` | `POST /api/productos` con `existencia: -1` | 400: `existencia: La existencia del producto no puede ser negativa` |
| `registrarSinNombreResponde400` | `POST /api/productos` con nombre en blanco | 400: `nombre: El nombre del producto es requerido` |
| `registrarNombreDuplicadoResponde409SinDetallesTecnicos` | Oracle rechaza el nombre repetido (`UQ_PRODUCTOS_NOMBRE`) | 409 con mensaje genérico, sin el texto `ORA-00001` |
| `actualizarProductoResponde200` | `PUT /api/productos/5` | 200 |
| `actualizarProductoInexistenteResponde404` | `PUT /api/productos/99` | 404 |
| `desactivarResponde204` | `PATCH /api/productos/5/desactivar` | 204 |
| `desactivarProductoYaDesactivadoResponde404` | Desactivar un producto que ya está inactivo | 404 (el servicio solo busca entre los productos activos) |
| `activarResponde204` | `PATCH /api/productos/5/activar` | 204 |
| `activarConConflictoResponde409` | El producto ya está activo | 409: `El producto ya se encuentra activado` |

## 2. Pruebas manuales contra Oracle (escenarios A–F)

Sirven como evidencia (video o capturas) para la entrega.

**Preparación**

1. Oracle en ejecución con `00_crear_usuario.sql`, `01_schema.sql` y `02_data.sql` ejecutados en una base limpia (ver [04-inicializacion-oracle.md](04-inicializacion-oracle.md)).
2. Backend en ejecución: `./mvnw spring-boot:run`.
3. Abrir `http://localhost:8080/swagger-ui.html`.

**Datos iniciales** (`02_data.sql`):

| Id | Producto | Precio | Existencia |
|---|---|---|---|
| 3 | Aceite 1 L | 45.00 | 60 |
| 5 | Leche 1 L | 27.00 | 120 |
| 6 | Cafe 250 g | 68.00 | 40 |

| Folio | Estado |
|---|---|
| PED-000001 | BORRADOR |
| PED-000002 | CONFIRMADO |
| PED-000003 | CANCELADO |

En Swagger, para cada endpoint: **Try it out** → llenar los datos → **Execute**.

| Paso | Endpoint y datos | Resultado esperado |
|---|---|---|
| **A1** | `POST /api/pedidos` con `{"productos":[{"id":5,"cantidad":2},{"id":3,"cantidad":1}]}` | 201. Anota el `id` (lo llamaremos `idA`). `estado: BORRADOR`, subtotales 54.00 y 45.00, `total: 99.00` |
| A2 | `GET /api/productos/5` y `GET /api/productos/3` | `existencia` 120 y 60 (crear no descuenta) |
| **B1** | `POST /api/pedidos` con `{"productos":[{"id":6,"cantidad":41}]}` | 201 (es un borrador). Anota el `id` (`idB`) |
| B2 | `POST /api/pedidos/{idB}/confirmar` | 409, `detail`: `Stock insuficiente para el producto 'Cafe 250 g': disponible 40, solicitado 41` |
| B3 | `GET /api/productos/6` y `GET /api/pedidos/{idB}` | Existencia 40; pedido en `BORRADOR` |
| **C1** | `POST /api/pedidos/{idA}/confirmar` | 200, `estado: CONFIRMADO` |
| C2 | `GET /api/productos/5` y `/3` | Leche 118, Aceite 59 |
| **D1** | `POST /api/pedidos/{idA}/cancelar` | 200, `estado: CANCELADO`, `fechaCancelacion` con valor |
| D2 | `GET /api/productos/5` y `/3` | Leche 120, Aceite 60 |
| D3 | `POST /api/pedidos/{idA}/cancelar` otra vez | 409. Existencias siguen en 120 y 60 |
| **E1** | `GET /api/pedidos?folio=PED-000003` para obtener su `id`, luego `POST /api/pedidos/{id}/confirmar` | 409: `El pedido no puede cambiar a Confirmado porque esta en estado Cancelado` |
| E2 | `POST /api/pedidos/99999/confirmar` | 404: `Pedido no encontrado con id: 99999` |
| E3 | `POST /api/pedidos` con `{"productos":[]}` | 400 con lista `errores` |
| E4 | `GET /api/pedidos?estado=CONFIRMADO` y `GET /api/pedidos?folio=PED-000002` | Solo aparecen los pedidos que coinciden |
| **F1** | Anota el estado de los pedidos y la existencia de los productos. Detén el backend (`Ctrl + C`) y Oracle (`docker stop oracle-local`) | |
| F2 | `docker start oracle-local`, espera `DATABASE IS READY TO USE!` en `docker logs -f oracle-local` y ejecuta otra vez `./mvnw spring-boot:run` | |
| F3 | Repite las consultas de F1 | Mismos pedidos, estados y existencias |

### 2.1 Comprobaciones en Oracle (SQL Developer, conexión `OSIN_LOCAL`)

```sql
SELECT FOLIO, ESTADO, TOTAL, NUMERO_PARTIDAS FROM VW_PEDIDO_RESUMEN ORDER BY ID;
SELECT ID, NOMBRE, EXISTENCIAS FROM PRODUCTOS ORDER BY ID;
```

La base de datos también protege el inventario. Este `UPDATE` debe fallar con `ORA-02290` (`CK_PRODUCTOS_EXISTENCIAS`):

```sql
UPDATE PRODUCTOS SET EXISTENCIAS = -1 WHERE ID = 1;
ROLLBACK;
```

### 2.2 Volver a empezar

Para repetir los escenarios desde los datos iniciales: ejecutar `99_limpiar.sql`, `01_schema.sql` y `02_data.sql` como `OSIN`.
