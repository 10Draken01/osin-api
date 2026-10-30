package com.lacomer.osinapi.dto.datos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "ProductoDatosRequest", description = "Datos del producto para el pedido")
public record ProductoDatos(
        @Schema(description = "Identificador del producto", example = "1")
        @NotNull(message = "El id del producto es requerido")
        @Positive(message = "El id del producto debe ser positivo")
        Long id,

        @Schema(description = "Cantidad del producto", example = "2")
        @NotNull(message = "La cantidad es requerida")
        @Positive(message = "La cantidad debe ser mayor a 0")
        Integer cantidad
) { }
