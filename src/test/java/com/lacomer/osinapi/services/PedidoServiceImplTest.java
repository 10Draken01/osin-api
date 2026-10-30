package com.lacomer.osinapi.services;

import com.lacomer.osinapi.dto.datos.ProductoDatos;
import com.lacomer.osinapi.dto.pedidos.PedidoRequest;
import com.lacomer.osinapi.dto.pedidos.PedidoResponse;
import com.lacomer.osinapi.entities.Pedido;
import com.lacomer.osinapi.entities.PedidoDetalle;
import com.lacomer.osinapi.entities.Producto;
import com.lacomer.osinapi.exceptions.ConflictoException;
import com.lacomer.osinapi.exceptions.DatoInvalidoException;
import com.lacomer.osinapi.exceptions.RecursoNoEncontradoException;
import com.lacomer.osinapi.mapper.PedidoMapper;
import com.lacomer.osinapi.repositories.PedidoRepository;
import com.lacomer.osinapi.repositories.ProductoRepository;
import com.lacomer.osinapi.services.pedidos.PedidoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Servicio de pedidos con repositorios simulados (sin base de datos)
@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private ProductoRepository productoRepository;
    @Spy private PedidoMapper pedidoMapper = new PedidoMapper();
    @InjectMocks private PedidoServiceImpl service;

    private Producto leche;
    private Producto pan;

    @BeforeEach
    void preparar() {
        leche = Producto.builder().id(1L).nombre("Leche").precioUnitario(new BigDecimal("24.50")).existencias(100).build();
        pan = Producto.builder().id(2L).nombre("Pan").precioUnitario(new BigDecimal("38.90")).existencias(60).build();
    }

    private Pedido pedidoBorrador(Producto p1, int c1, Producto p2, int c2) {
        Pedido pedido = Pedido.crear();
        pedido.asignarFolio("PED-000001");
        pedido.agregarPedidoDetalle(PedidoDetalle.crear(p1, c1));
        pedido.agregarPedidoDetalle(PedidoDetalle.crear(p2, c2));
        return pedido;
    }

    //  registrar

    @Test
    void registrarCreaBorradorCalculaTotalYNoDescuentaInventario() {
        when(pedidoRepository.obtenerSiguienteFolio()).thenReturn(4L);
        when(productoRepository.findByIdAndActivo(1L, true)).thenReturn(Optional.of(leche));
        when(productoRepository.findByIdAndActivo(2L, true)).thenReturn(Optional.of(pan));
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        PedidoResponse r = service.registrar(new PedidoRequest(
                List.of(new ProductoDatos(1L, 2), new ProductoDatos(2L, 1))));

        assertEquals("PED-000004", r.folio());
        assertEquals("BORRADOR", r.estado());
        assertEquals(0, new BigDecimal("87.90").compareTo(r.total()));
        assertEquals(2, r.productos().size());
        assertEquals(100, leche.getExistencias());
        assertEquals(60, pan.getExistencias());
    }

    @Test
    void registrarConProductoInexistenteLanza404YNoGuarda() {
        when(pedidoRepository.obtenerSiguienteFolio()).thenReturn(4L);
        when(productoRepository.findByIdAndActivo(99L, true)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.registrar(new PedidoRequest(List.of(new ProductoDatos(99L, 1)))));

        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarConProductosRepetidosSeRechaza() {
        assertThrows(DatoInvalidoException.class,
                () -> service.registrar(new PedidoRequest(
                        List.of(new ProductoDatos(1L, 1), new ProductoDatos(1L, 2)))));

        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    @Test
    void registrarSinProductosSeRechaza() {
        assertThrows(DatoInvalidoException.class, () -> service.registrar(new PedidoRequest(List.of())));
        assertThrows(DatoInvalidoException.class, () -> service.registrar(new PedidoRequest(null)));
        assertThrows(DatoInvalidoException.class, () -> service.registrar(null));
    }

    @Test
    void registrarConCantidadInvalidaSeRechaza() {
        when(pedidoRepository.obtenerSiguienteFolio()).thenReturn(4L);
        when(productoRepository.findByIdAndActivo(1L, true)).thenReturn(Optional.of(leche));

        assertThrows(DatoInvalidoException.class,
                () -> service.registrar(new PedidoRequest(List.of(new ProductoDatos(1L, 0)))));

        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    //  confirmar

    @Test
    void confirmarPedidoInexistenteLanza404() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.confirmar(99L));
    }

    @Test
    void confirmarDescuentaInventarioYCambiaEstado() {
        Pedido pedido = pedidoBorrador(leche, 2, pan, 1);
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        PedidoResponse r = service.confirmar(10L);

        assertEquals("CONFIRMADO", r.estado());
        assertEquals(98, leche.getExistencias());
        assertEquals(59, pan.getExistencias());
    }

    @Test
    void confirmarConStockInsuficienteLanzaConflictoSinCambiosNiGuardado() {
        Pedido pedido = pedidoBorrador(leche, 2, pan, 61);   // pan: solo hay 60
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertThrows(ConflictoException.class, () -> service.confirmar(10L));

        assertEquals(100, leche.getExistencias());
        assertEquals(60, pan.getExistencias());
        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    //  cancelar

    @Test
    void cancelarPedidoConfirmadoReponeInventario() {
        Pedido pedido = pedidoBorrador(leche, 2, pan, 1);
        pedido.confirmar();
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        PedidoResponse r = service.cancelar(10L);

        assertEquals("CANCELADO", r.estado());
        assertNotNull(r.fechaCancelacion());
        assertEquals(100, leche.getExistencias());
        assertEquals(60, pan.getExistencias());
    }

    @Test
    void cancelarPedidoYaCanceladoLanzaConflictoYNoRepone() {
        Pedido pedido = pedidoBorrador(leche, 2, pan, 1);
        pedido.confirmar();
        pedido.cancelar();
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertThrows(ConflictoException.class, () -> service.cancelar(10L));

        assertEquals(100, leche.getExistencias());
        assertEquals(60, pan.getExistencias());
        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelarPedidoInexistenteLanza404() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.cancelar(99L));
    }

    //  completar

    @Test
    void completarPedidoConfirmadoNoMueveInventario() {
        Pedido pedido = pedidoBorrador(leche, 2, pan, 1);
        pedido.confirmar();
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        PedidoResponse r = service.completar(10L);

        assertEquals("COMPLETADO", r.estado());
        assertEquals(98, leche.getExistencias());
        assertEquals(59, pan.getExistencias());
    }

    @Test
    void completarUnBorradorLanzaConflicto() {
        Pedido pedido = pedidoBorrador(leche, 2, pan, 1);
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertThrows(ConflictoException.class, () -> service.completar(10L));

        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    //  consulta

    @Test
    void obtenerPorIdInexistenteLanza404() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }

    //  actualizar

    @Test
    void actualizarBorradorReemplazaPartidasConPrecioVigente() {
        Pedido pedido = pedidoBorrador(leche, 2, pan, 1);
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(productoRepository.findByIdAndActivo(1L, true)).thenReturn(Optional.of(leche));
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        PedidoResponse r = service.actualizar(10L, new PedidoRequest(List.of(new ProductoDatos(1L, 3))));

        assertEquals("BORRADOR", r.estado());
        assertEquals(1, r.productos().size());
        assertEquals(3, r.productos().get(0).cantidad());
        assertEquals(0, new BigDecimal("73.50").compareTo(r.total()));
        assertEquals(100, leche.getExistencias());
    }

    @Test
    void actualizarUnConfirmadoLanzaConflictoSinBuscarProductos() {
        Pedido pedido = pedidoBorrador(leche, 1, pan, 1);
        pedido.confirmar();
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));

        assertThrows(ConflictoException.class,
                () -> service.actualizar(10L, new PedidoRequest(List.of(new ProductoDatos(1L, 3)))));
        verify(productoRepository, never()).findByIdAndActivo(any(), any());
        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarConProductosRepetidosLanzaDatoInvalido() {
        assertThrows(DatoInvalidoException.class, () -> service.actualizar(10L, new PedidoRequest(
                List.of(new ProductoDatos(1L, 1), new ProductoDatos(1L, 2)))));
        verify(pedidoRepository, never()).findById(any());
    }

    @Test
    void actualizarPedidoInexistenteLanza404() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(99L, new PedidoRequest(List.of(new ProductoDatos(1L, 1)))));
    }
}
