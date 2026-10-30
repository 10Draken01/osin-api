package com.lacomer.osinapi.dto.datos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "PedidoDetallesDatos", description = "Información de un pedido detallado")
public record PedidoDetallesDatos(
        @Schema(description = "Identificador del producto", example = "1")
        Long idProducto,

        @Schema(description = "Nombre del producto", example = "Producto A")
        String nombre,

        @Schema(description = "Cantidad del producto", example = "2")
        Integer cantidad,

        @Schema(description = "Precio unitario del producto", example = "100.50")
        BigDecimal precioUnitario,

        @Schema(description = "Subtotal del producto en el pedido", example = "150.00")
        BigDecimal subtotal
) { }