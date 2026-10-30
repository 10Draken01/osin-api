package com.lacomer.osinapi.entities;

import com.lacomer.osinapi.enums.EstadoPedido;
import com.lacomer.osinapi.exceptions.ConflictoException;
import com.lacomer.osinapi.exceptions.DatoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// Reglas de negocio del pedido sin Spring ni base de datos.
class PedidoTest {

    private Producto leche;   // 24.50, stock 100
    private Producto pan;     // 38.90, stock 60
    private Producto atun;    // 21.90, stock 5

    @BeforeEach
    void preparar() {
        leche = Producto.crear("Leche entera 1 L", null, new BigDecimal("24.50"), 100);
        pan = Producto.crear("Pan de caja", null, new BigDecimal("38.90"), 60);
        atun = Producto.crear("Atun en agua", null, new BigDecimal("21.90"), 5);
    }

    private Pedido pedidoCon(Object... productoYCantidad) {
        Pedido pedido = Pedido.crear();
        pedido.asignarFolio("PED-000001");
        for (int i = 0; i < productoYCantidad.length; i += 2) {
            pedido.agregarPedidoDetalle(
                    PedidoDetalle.crear((Producto) productoYCantidad[i], (Integer) productoYCantidad[i + 1]));
        }
        return pedido;
    }

    // Escenario A: importes

    @Test
    void calculaSubtotalesYTotal() {
        Pedido pedido = pedidoCon(leche, 2, pan, 1);

        assertEquals(new BigDecimal("49.00"), pedido.getDetalles().get(0).getSubtotal());
        assertEquals(new BigDecimal("38.90"), pedido.getDetalles().get(1).getSubtotal());
        assertEquals(new BigDecimal("87.90"), pedido.calcularTotal());
    }

    @Test
    void nuevoPedidoNaceEnBorradorYNoTocaElInventario() {
        Pedido pedido = pedidoCon(leche, 2, pan, 1);

        assertEquals(EstadoPedido.BORRADOR, pedido.getEstadoPedido());
        assertEquals(100, leche.getExistencias());
        assertEquals(60, pan.getExistencias());
    }

    @Test
    void laPartidaConservaElPrecioVigenteAlCrearla() {
        Pedido pedido = pedidoCon(leche, 1);
        leche.actualizar("Leche entera 1 L", null, new BigDecimal("99.00"), 100);

        assertEquals(new BigDecimal("24.50"), pedido.getDetalles().get(0).getPrecioUnitario());
    }

    @Test
    void partidaConCantidadInvalidaSeRechaza() {
        assertThrows(DatoInvalidoException.class, () -> PedidoDetalle.crear(leche, 0));
        assertThrows(DatoInvalidoException.class, () -> PedidoDetalle.crear(leche, -2));
        assertThrows(DatoInvalidoException.class, () -> PedidoDetalle.crear(leche, null));
        assertThrows(DatoInvalidoException.class, () -> PedidoDetalle.crear(null, 1));
    }

    @Test
    void formatoDeFolio() {
        assertEquals("PED-000004", Pedido.formatearFolio(4L));
        assertEquals("PED-123456", Pedido.formatearFolio(123456L));
        assertThrows(DatoInvalidoException.class, () -> Pedido.formatearFolio(null));
    }

    // Escenario C: confirmar

    @Test
    void confirmarDescuentaInventarioYCambiaEstado() {
        Pedido pedido = pedidoCon(leche, 2, pan, 1);

        pedido.confirmar();

        assertEquals(EstadoPedido.CONFIRMADO, pedido.getEstadoPedido());
        assertEquals(98, leche.getExistencias());
        assertEquals(59, pan.getExistencias());
    }

    // Escenario B: stock insuficiente sin cambios parciales

    @Test
    void stockInsuficienteRechazaTodoElPedidoSinModificarNingunProducto() {
        // la primera partida si alcanza y la segunda no, no se debe descontar nada
        Pedido pedido = pedidoCon(leche, 2, atun, 10);

        assertThrows(ConflictoException.class, pedido::confirmar);

        assertEquals(EstadoPedido.BORRADOR, pedido.getEstadoPedido());
        assertEquals(100, leche.getExistencias());
        assertEquals(5, atun.getExistencias());
    }

    @Test
    void sePuedeConfirmarPidiendoExactamenteLaExistencia() {
        Pedido pedido = pedidoCon(atun, 5);
        pedido.confirmar();
        assertEquals(0, atun.getExistencias());
    }

    // Escenario D: cancelar repone una sola vez

    @Test
    void cancelarPedidoConfirmadoReponeInventarioYRegistraFechas() {
        Pedido pedido = pedidoCon(leche, 2, pan, 1);
        pedido.confirmar();

        pedido.cancelar();

        assertEquals(EstadoPedido.CANCELADO, pedido.getEstadoPedido());
        assertEquals(100, leche.getExistencias());
        assertEquals(60, pan.getExistencias());
        assertNotNull(pedido.getFechaCancelacion());
    }

    @Test
    void cancelarDosVecesNoDuplicaElInventario() {
        Pedido pedido = pedidoCon(leche, 2);
        pedido.confirmar();
        pedido.cancelar();

        assertThrows(ConflictoException.class, pedido::cancelar);

        assertEquals(100, leche.getExistencias());   // no 102
    }

    // Escenario E: transiciones no permitidas

    @Test
    void noSePuedeConfirmarDosVeces() {
        Pedido pedido = pedidoCon(leche, 2);
        pedido.confirmar();

        assertThrows(ConflictoException.class, pedido::confirmar);

        assertEquals(98, leche.getExistencias());    // no 96
    }

    @Test
    void noSePuedeConfirmarUnPedidoCancelado() {
        Pedido pedido = pedidoCon(leche, 2);
        pedido.confirmar();
        pedido.cancelar();

        assertThrows(ConflictoException.class, pedido::confirmar);

        assertEquals(100, leche.getExistencias());
    }

    @Test
    void cancelarUnBorradorNoMueveElInventario() {
        Pedido pedido = pedidoCon(leche, 2);

        pedido.cancelar();

        assertEquals(EstadoPedido.CANCELADO, pedido.getEstadoPedido());
        assertNotNull(pedido.getFechaCancelacion());
        assertEquals(100, leche.getExistencias());   // no 102
    }

    // Completar

    @Test
    void completarUnPedidoConfirmadoNoMueveElInventario() {
        Pedido pedido = pedidoCon(leche, 2);
        pedido.confirmar();

        pedido.completar();

        assertEquals(EstadoPedido.COMPLETADO, pedido.getEstadoPedido());
        assertEquals(98, leche.getExistencias());
    }

    @Test
    void soloSeCompletaUnPedidoConfirmado() {
        Pedido borrador = pedidoCon(leche, 2);
        assertThrows(ConflictoException.class, borrador::completar);

        Pedido cancelado = pedidoCon(pan, 1);
        cancelado.cancelar();
        assertThrows(ConflictoException.class, cancelado::completar);
    }

    @Test
    void unPedidoCompletadoNoSePuedeCancelar() {
        Pedido pedido = pedidoCon(leche, 2);
        pedido.confirmar();
        pedido.completar();

        assertThrows(ConflictoException.class, pedido::cancelar);

        assertEquals(98, leche.getExistencias());
    }

    // Actualizar un BORRADOR

    @Test
    void actualizarBorradorAjustaCantidadesQuitaYAgregaPartidas() {
        Pedido pedido = pedidoCon(leche, 2, pan, 1);

        pedido.actualizarPartidas(java.util.List.of(
                PedidoDetalle.crear(leche, 5),
                PedidoDetalle.crear(atun, 1)));

        assertEquals(2, pedido.getDetalles().size());
        assertSame(leche, pedido.getDetalles().get(0).getProducto());
        assertEquals(5, pedido.getDetalles().get(0).getCantidad());
        assertSame(atun, pedido.getDetalles().get(1).getProducto());
        assertEquals(new BigDecimal("144.40"), pedido.calcularTotal());
        assertEquals(EstadoPedido.BORRADOR, pedido.getEstadoPedido());
    }

    @Test
    void actualizarBorradorTomaElPrecioVigenteDelCatalogo() {
        Pedido pedido = pedidoCon(leche, 1);
        leche.actualizar("Leche entera 1 L", null, new BigDecimal("26.00"), 100);

        pedido.actualizarPartidas(java.util.List.of(PedidoDetalle.crear(leche, 1)));

        assertEquals(new BigDecimal("26.00"), pedido.getDetalles().get(0).getPrecioUnitario());
    }

    @Test
    void actualizarBorradorNoMueveElInventario() {
        Pedido pedido = pedidoCon(leche, 2);

        pedido.actualizarPartidas(java.util.List.of(PedidoDetalle.crear(leche, 50)));

        assertEquals(100, leche.getExistencias());
    }

    @Test
    void soloUnBorradorSePuedeActualizar() {
        Pedido confirmado = pedidoCon(leche, 1);
        confirmado.confirmar();
        Pedido cancelado = pedidoCon(leche, 1);
        cancelado.cancelar();

        assertThrows(ConflictoException.class,
                () -> confirmado.actualizarPartidas(java.util.List.of(PedidoDetalle.crear(leche, 2))));
        assertThrows(ConflictoException.class,
                () -> cancelado.actualizarPartidas(java.util.List.of(PedidoDetalle.crear(leche, 2))));
        assertEquals(1, confirmado.getDetalles().get(0).getCantidad());
    }

    @Test
    void actualizarSinPartidasSeRechaza() {
        Pedido pedido = pedidoCon(leche, 1);

        assertThrows(DatoInvalidoException.class, () -> pedido.actualizarPartidas(java.util.List.of()));
        assertEquals(1, pedido.getDetalles().size());
    }
}
