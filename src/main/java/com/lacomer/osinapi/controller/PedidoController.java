package com.lacomer.osinapi.controller;

import com.lacomer.osinapi.dto.pedidos.PedidoRequest;
import com.lacomer.osinapi.dto.pedidos.PedidoResponse;
import com.lacomer.osinapi.enums.EstadoPedido;
import com.lacomer.osinapi.services.pedidos.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pedidos", description = "Creacion, consulta, edicion de borradores y cambios de estado de pedidos")
@ApiResponse(responseCode = "500", description = "Error interno del servidor")
public class PedidoController {

    private final PedidoService service;

    @GetMapping
    @Operation(summary = "Listar pedidos",
            description = "Listado paginado con folio, fecha, estado y total, del mas reciente al mas antiguo. Filtros opcionales por estado y por folio.")
    @ApiResponse(responseCode = "200", description = "Pedidos obtenidos correctamente.")
    @ApiResponse(responseCode = "400", description = "Filtro o paginacion invalidos (por ejemplo, estado inexistente).")
    public ResponseEntity<Page<PedidoResponse>> listar(
            @Parameter(description = "BORRADOR, CONFIRMADO, COMPLETADO o CANCELADO")
            @RequestParam(required = false) EstadoPedido estado,
            @Parameter(description = "Folio completo o parte del folio, por ejemplo PED-000001")
            @RequestParam(required = false) String folio,
            @Parameter(description = "Numero de pagina, empieza en 0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "La pagina no puede ser negativa")
            int pagina,
            @Parameter(description = "Registros por pagina (1 a 100)")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamano minimo es 1")
            @Max(value = 100, message = "El tamano maximo es 100")
            int tamano
    ) {
        // Los pedidos mas recientes primero
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("id").descending());
        return ResponseEntity.ok(service.listar(estado, folio, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de pedido", description = "Devuelve el pedido con todas sus partidas.")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado.")
    @ApiResponse(responseCode = "404", description = "Pedido no encontrado.")
    public ResponseEntity<PedidoResponse> obtenerPorId(
            @PathVariable @Positive(message = "El id debe ser positivo") Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Crear pedido",
            description = "Crea un pedido en estado BORRADOR. Los importes los calcula el servidor. No descuenta inventario.")
    @ApiResponse(responseCode = "201", description = "Pedido creado.")
    @ApiResponse(responseCode = "400", description = "Datos invalidos (lista vacia, cantidad invalida, productos repetidos).")
    @ApiResponse(responseCode = "404", description = "Producto inexistente o inactivo.")
    public ResponseEntity<PedidoResponse> registrar(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(201).body(service.registrar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar pedido",
            description = "Reemplaza las partidas de un pedido en BORRADOR. Todas las partidas toman el precio vigente del catalogo. No mueve inventario.")
    @ApiResponse(responseCode = "200", description = "Pedido actualizado.")
    @ApiResponse(responseCode = "400", description = "Datos invalidos (lista vacia, cantidad invalida, productos repetidos).")
    @ApiResponse(responseCode = "404", description = "Pedido inexistente, o producto inexistente o inactivo.")
    @ApiResponse(responseCode = "409", description = "El pedido no esta en BORRADOR o fue modificado por otro usuario.")
    public ResponseEntity<PedidoResponse> actualizar(
            @PathVariable @Positive(message = "El id debe ser positivo") Long id,
            @Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar pedido",
            description = "Valida existencia de todas las partidas y descuenta el inventario de forma atomica.")
    @ApiResponse(responseCode = "200", description = "Pedido confirmado.")
    @ApiResponse(responseCode = "404", description = "Pedido no encontrado.")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente o transicion de estado no permitida.")
    public ResponseEntity<PedidoResponse> confirmar(
            @PathVariable @Positive(message = "El id debe ser positivo") Long id) {
        return ResponseEntity.ok(service.confirmar(id));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar pedido",
            description = "Cancela un pedido en BORRADOR o CONFIRMADO. Si estaba confirmado repone el inventario una sola vez.")
    @ApiResponse(responseCode = "200", description = "Pedido cancelado.")
    @ApiResponse(responseCode = "404", description = "Pedido no encontrado.")
    @ApiResponse(responseCode = "409", description = "Transicion de estado no permitida (por ejemplo, ya esta cancelado).")
    public ResponseEntity<PedidoResponse> cancelar(
            @PathVariable @Positive(message = "El id debe ser positivo") Long id) {
        return ResponseEntity.ok(service.cancelar(id));
    }

    @PostMapping("/{id}/completar")
    @Operation(summary = "Completar pedido",
            description = "Marca la compra como finalizada. Solo desde CONFIRMADO, no mueve inventario.")
    @ApiResponse(responseCode = "200", description = "Pedido completado.")
    @ApiResponse(responseCode = "404", description = "Pedido no encontrado.")
    @ApiResponse(responseCode = "409", description = "Transicion de estado no permitida.")
    public ResponseEntity<PedidoResponse> completar(
            @PathVariable @Positive(message = "El id debe ser positivo") Long id) {
        return ResponseEntity.ok(service.completar(id));
    }
}
