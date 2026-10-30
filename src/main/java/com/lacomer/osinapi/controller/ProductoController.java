package com.lacomer.osinapi.controller;

import com.lacomer.osinapi.dto.productos.ProductoRequest;
import com.lacomer.osinapi.dto.productos.ProductoResponse;
import com.lacomer.osinapi.services.productos.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Hereda de CrudController: GET /api/productos, GET /{id}, POST y PUT /{id}
@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Catalogo e inventario de productos")
public class ProductoController extends CrudController<ProductoRequest, ProductoResponse, ProductoService> {

    public ProductoController(ProductoService service) {
        super(service);
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar productos activos", description = "Catalogo disponible para crear pedidos.")
    @ApiResponse(responseCode = "200", description = "Lista de productos activos.")
    public ResponseEntity<List<ProductoResponse>> listarActivos() {
        return ResponseEntity.ok(service.listarActivos());
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar producto", description = "Baja logica: el producto ya no puede agregarse a pedidos nuevos.")
    @ApiResponse(responseCode = "204", description = "Producto desactivado.")
    @ApiResponse(responseCode = "404", description = "Producto activo no encontrado.")
    public ResponseEntity<Void> desactivar(
            @PathVariable @Positive(message = "El id debe ser positivo") Long id) {
        service.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Activar producto", description = "Reactiva un producto dado de baja.")
    @ApiResponse(responseCode = "204", description = "Producto activado.")
    @ApiResponse(responseCode = "404", description = "Producto desactivado no encontrado.")
    public ResponseEntity<Void> activar(
            @PathVariable @Positive(message = "El id debe ser positivo") Long id) {
        service.activarProducto(id);
        return ResponseEntity.noContent().build();
    }
}
