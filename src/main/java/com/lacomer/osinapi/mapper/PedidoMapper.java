package com.lacomer.osinapi.mapper;

import com.lacomer.osinapi.dto.datos.PedidoDetallesDatos;
import com.lacomer.osinapi.dto.pedidos.PedidoRequest;
import com.lacomer.osinapi.dto.pedidos.PedidoResponse;
import com.lacomer.osinapi.entities.Pedido;
import com.lacomer.osinapi.entities.PedidoDetalle;
import com.lacomer.osinapi.entities.Producto;
import org.springframework.stereotype.Component;

@Component
public class PedidoMapper implements CommonMapper<PedidoRequest, PedidoResponse, Pedido> {
    @Override
    public Pedido requestAEntidad(PedidoRequest request) {
        return request == null ? null : Pedido.crear();
    }

    public Pedido requestAEntidad(PedidoRequest request, String folio) {
        Pedido pedido = requestAEntidad(request);
        pedido.asignarFolio(folio);
        return pedido;
    }

    @Override
    public PedidoResponse entidadAResponse(Pedido pedido) {
        return pedido == null ? null
                : new PedidoResponse(
                pedido.getId(),
                pedido.getFolio(),
                pedido.getDetalles().stream().map(this::pedidoDetalleAPedidoDetallesDatos).toList(),
                pedido.getEstadoPedido().name(),
                pedido.calcularTotal(),
                pedido.getFechaCreacion(),
                pedido.getFechaActualizacion(),
                pedido.getFechaCancelacion()
        );
    }

    public PedidoDetalle productoDatosAPedidoDetalle(Producto producto, Integer cantidad) {
        return PedidoDetalle.crear(producto, cantidad);
    }

    private PedidoDetallesDatos pedidoDetalleAPedidoDetallesDatos(PedidoDetalle pedidoDetalle) {
        return pedidoDetalle == null ? null
                : new PedidoDetallesDatos(
                pedidoDetalle.getProducto().getId(),
                pedidoDetalle.getProducto().getNombre(),
                pedidoDetalle.getCantidad(),
                pedidoDetalle.getPrecioUnitario(),
                pedidoDetalle.getSubtotal()
        );
    }
}
