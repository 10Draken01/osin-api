package com.lacomer.osinapi.entities;

import com.lacomer.osinapi.enums.EstadoPedido;
import com.lacomer.osinapi.exceptions.ConflictoException;
import com.lacomer.osinapi.exceptions.DatoInvalidoException;
import com.lacomer.osinapi.utils.StringCustomUtils;
import com.lacomer.osinapi.utils.ValoresNumericosUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "PEDIDOS")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "FOLIO", nullable = false, updatable = false)
    private String folio;

    @Builder.Default
    @Column(name = "ESTADO", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoPedido = EstadoPedido.BORRADOR;

    @Builder.Default
    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Builder.Default
    @Column(name = "FECHA_ACTUALIZACION", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @Column(name = "FECHA_CANCELACION")
    private LocalDateTime fechaCancelacion;

    // Bloqueo optimista: Hibernate compara la version al guardar y si otra peticion
    // ya cambio el pedido lanza ObjectOptimisticLockingFailureException (409)
    @Version
    @Column(name = "VERSION_LOCK", nullable = false)
    private Long version;

    @Builder.Default
    @OneToMany(
            mappedBy = "pedido",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<PedidoDetalle> detalles = new ArrayList<>();

    private void validarCambioEstadoPedido(EstadoPedido nuevoEstado) {
        if (!this.estadoPedido.puedeCambiarA(nuevoEstado))
            throw new ConflictoException(
                    "El pedido no puede cambiar a " + nuevoEstado.getDescripcion()
                            + " porque esta en estado " + this.estadoPedido.getDescripcion()
            );
    }

    // Los cambios de estado solo se hacen desde confirmar, cancelar y completar
    // (actualizarPartidas cambia el contenido de un BORRADOR, no su estado)
    private void cambiarEstadoPedido(EstadoPedido nuevoEstado) {
        if (nuevoEstado == null)
            throw new DatoInvalidoException("El estado de pedido no puede ser nulo");
        validarCambioEstadoPedido(nuevoEstado);
        this.estadoPedido = nuevoEstado;
        this.fechaActualizacion = LocalDateTime.now();
    }

    // Primero se valida el stock de todas las partidas y despues se descuenta,
    // asi si una partida no alcanza no se modifica ningun producto
    public void confirmar() {
        validarCambioEstadoPedido(EstadoPedido.CONFIRMADO);
        detalles.forEach(PedidoDetalle::validarExistenciasSuficientes);
        detalles.forEach(PedidoDetalle::descontarExistencias);
        cambiarEstadoPedido(EstadoPedido.CONFIRMADO);
    }

    // Solo se regresa el inventario si el pedido estaba confirmado (un borrador nunca desconto nada).
    // Un pedido cancelado ya no puede cambiar de estado, entonces el inventario se regresa una sola vez
    public void cancelar() {
        validarCambioEstadoPedido(EstadoPedido.CANCELADO);
        if (this.estadoPedido == EstadoPedido.CONFIRMADO)
            detalles.forEach(PedidoDetalle::restituirExistencias);
        cambiarEstadoPedido(EstadoPedido.CANCELADO);
        this.fechaCancelacion = this.fechaActualizacion;
    }

    // Compra finalizada, el inventario ya se desconto al confirmar
    public void completar() {
        cambiarEstadoPedido(EstadoPedido.COMPLETADO);
    }

    // Solo un pedido en BORRADOR se puede modificar (EstadoPedido.actualizable).
    // Se llama antes de buscar los productos para responder 409 aunque la peticion traiga productos invalidos
    public void validarQueSeaActualizable() {
        if (!this.estadoPedido.isActualizable())
            throw new ConflictoException(
                    "El pedido no se puede modificar porque esta en estado " + this.estadoPedido.getDescripcion());
    }

    // Reemplaza las partidas de un BORRADOR. No mueve inventario (todavia no se descuenta).
    // Las partidas se ajustan en su lugar en vez de borrar todas y crearlas de nuevo:
    // al hacer flush Hibernate inserta antes de borrar, y volver a insertar el mismo producto
    // violaria UQ_PEDIDOS_DETALLES_PEDIDO_PRODUCTO.
    // Todas las partidas quedan con el precio vigente del catalogo (las nuevas lo traen de PedidoDetalle.crear)
    public void actualizarPartidas(List<PedidoDetalle> nuevas) {
        validarQueSeaActualizable();
        if (nuevas == null || nuevas.isEmpty())
            throw new DatoInvalidoException("El pedido debe contener al menos un producto");

        // 1. Quitar las partidas cuyo producto ya no viene (orphanRemoval las borra)
        detalles.removeIf(actual -> nuevas.stream().noneMatch(actual::esDelMismoProducto));

        // 2. Actualizar las que siguen y agregar las nuevas
        for (PedidoDetalle nueva : nuevas) {
            detalles.stream()
                    .filter(actual -> actual.esDelMismoProducto(nueva))
                    .findFirst()
                    .ifPresentOrElse(
                            actual -> actual.actualizarCantidadYPrecio(nueva),
                            () -> agregarPedidoDetalle(nueva));
        }
        this.fechaActualizacion = LocalDateTime.now();
    }

    public BigDecimal calcularTotal() {
        return detalles.stream()
                .map(PedidoDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void agregarPedidoDetalle(PedidoDetalle pedidoDetalle) {
        if (pedidoDetalle == null)
            throw new DatoInvalidoException("El pedido detalle es requerido");
        this.detalles.add(pedidoDetalle);
        pedidoDetalle.asignarPedido(this);
    }

    public void asignarFolio(String folio) {
        StringCustomUtils.validarNoVacioNoNull(folio, "El folio es requerido");
        this.folio = folio;
    }

    public static String formatearFolio(Long secuencia) {
        ValoresNumericosUtils.validarPositivo(secuencia, "La secuencia del folio no es valida");
        return String.format("PED-%06d", secuencia);
    }

    public static Pedido crear() {
        return Pedido.builder().build();
    }
}
