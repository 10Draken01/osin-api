package com.lacomer.osinapi.dto.pedidos;

import com.lacomer.osinapi.dto.datos.ProductoDatos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Datos para registrar un pedido")
public record PedidoRequest(
        @Schema(description = "Lista de productos del pedido (al menos uno, sin repetir productos)")
        @NotEmpty(message = "El pedido debe contener al menos un producto")
        List<@Valid ProductoDatos> productos
) { }
