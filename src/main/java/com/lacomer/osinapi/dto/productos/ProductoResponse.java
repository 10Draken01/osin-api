package com.lacomer.osinapi.dto.productos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Datos de un producto")
public record ProductoResponse(
        @Schema(description = "ID del producto")
        Long id,

        @Schema(description = "Nombre del producto")
        String nombre,

        @Schema(description = "Descripcion del producto")
        String descripcion,

        @Schema(description = "Precio unitario del producto")
        BigDecimal precioUnitario,

        @Schema(description = "Existencia del producto")
        Integer existencia,

        @Schema(description = "Indica si el producto esta activo")
        Boolean activo
) { }
