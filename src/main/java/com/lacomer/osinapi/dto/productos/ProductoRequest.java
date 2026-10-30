package com.lacomer.osinapi.dto.productos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Datos para registrar o actualizar un producto")
public record ProductoRequest(
        @Schema(description = "Nombre del producto", example = "Leche entera 1 L")
        @NotBlank(message = "El nombre del producto es requerido")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String nombre,

        @Schema(description = "Descripcion del producto (opcional)")
        @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
        String descripcion,

        @Schema(description = "Precio unitario del producto", example = "24.50")
        @NotNull(message = "El precio unitario es requerido")
        @Positive(message = "El precio unitario debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales")
        BigDecimal precioUnitario,

        @Schema(description = "Existencia del producto", example = "100")
        @NotNull(message = "La existencia del producto es requerida")
        @PositiveOrZero(message = "La existencia del producto no puede ser negativa")
        Integer existencia
) { }
