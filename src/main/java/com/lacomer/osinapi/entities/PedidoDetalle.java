package com.lacomer.osinapi.entities;

import com.lacomer.osinapi.exceptions.DatoInvalidoException;
import com.lacomer.osinapi.utils.ValoresNumericosUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Table(name = "PEDIDOS_DETALLES")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class PedidoDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PEDIDO_ID", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PRODUCTO_ID", nullable = false)
    private Producto producto;

    @Column(name = "CANTIDAD", nullable = false)
    private Integer cantidad;

    // Precio del producto al crear el pedido (o al editarlo mientras es BORRADOR).
    // Una vez confirmado ya no cambia aunque se actualice el catalogo
    @Column(name = "PRECIO_UNITARIO", nullable = false)
    private BigDecimal precioUnitario;

    // El subtotal no se guarda en la BD (es un dato derivado), se calcula con cantidad * precio
    public BigDecimal getSubtotal() {
        if (cantidad == null || precioUnitario == null)
            return BigDecimal.ZERO;
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    private static void validarDatos(Producto producto, Integer cantidad) {
        if (producto == null)
            throw new DatoInvalidoException("El producto es requerido");

        ValoresNumericosUtils.validarPositivo(
                cantidad, "La cantidad debe ser mayor a 0"
        );
    }

    void asignarPedido(Pedido pedido) {
        if (pedido == null)
            throw new DatoInvalidoException("El pedido es requerido");
        this.pedido = pedido;
    }

    public void validarExistenciasSuficientes() {
        producto.validarExistenciasSuficientes(cantidad);
    }

    public void descontarExistencias() {
        producto.decrementarExistencias(cantidad);
    }

    public void restituirExistencias() {
        producto.aumentarExistencias(cantidad);
    }

    // Misma partida = mismo producto (por id; por referencia si el producto aun no se guarda)
    boolean esDelMismoProducto(PedidoDetalle otra) {
        if (otra == null || otra.producto == null || this.producto == null)
            return false;
        if (this.producto == otra.producto)
            return true;
        return this.producto.getId() != null && this.producto.getId().equals(otra.producto.getId());
    }

    // Al editar un BORRADOR la partida toma la cantidad nueva y el precio vigente del producto
    void actualizarCantidadYPrecio(PedidoDetalle nueva) {
        validarDatos(nueva.producto, nueva.cantidad);
        this.cantidad = nueva.cantidad;
        this.precioUnitario = nueva.precioUnitario;
    }

    public static PedidoDetalle crear(Producto producto, Integer cantidad) {
        validarDatos(producto, cantidad);
        return PedidoDetalle.builder()
                .producto(producto)
                .cantidad(cantidad)
                .precioUnitario(producto.getPrecioUnitario())
                .build();
    }
}
