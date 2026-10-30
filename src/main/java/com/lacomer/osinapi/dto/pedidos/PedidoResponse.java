package com.lacomer.osinapi.dto.pedidos;

import com.lacomer.osinapi.dto.datos.PedidoDetallesDatos;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Datos de respuesta de un pedido")
public record PedidoResponse(
        @Schema(description = "Identificador unico del pedido", example = "1")
        Long id,

        @Schema(description = "Folio del pedido", example = "PED-000001")
        String folio,

        @Schema(description = "Partidas del pedido")
        List<PedidoDetallesDatos> productos,

        @Schema(description = "Estado del pedido", example = "BORRADOR",
                allowableValues = {"BORRADOR", "CONFIRMADO", "COMPLETADO", "CANCELADO"})
        String estado,

        @Schema(description = "Total del pedido calculado por el servidor", example = "87.90")
        BigDecimal total,

        @Schema(description = "Fecha de creacion del pedido")
        LocalDateTime fechaCreacion,

        @Schema(description = "Fecha de ultima actualizacion del pedido")
        LocalDateTime fechaActualizacion,

        @Schema(description = "Fecha de cancelacion del pedido (si aplica)")
        LocalDateTime fechaCancelacion
) { }
