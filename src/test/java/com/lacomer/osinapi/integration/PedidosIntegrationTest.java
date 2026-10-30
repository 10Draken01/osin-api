package com.lacomer.osinapi.integration;

import com.lacomer.osinapi.dto.datos.PedidoDetallesDatos;
import com.lacomer.osinapi.dto.datos.ProductoDatos;
import com.lacomer.osinapi.dto.pedidos.PedidoRequest;
import com.lacomer.osinapi.dto.pedidos.PedidoResponse;
import com.lacomer.osinapi.entities.Producto;
import com.lacomer.osinapi.enums.EstadoPedido;
import com.lacomer.osinapi.exceptions.ConflictoException;
import com.lacomer.osinapi.exceptions.DatoInvalidoException;
import com.lacomer.osinapi.exceptions.RecursoNoEncontradoException;
import com.lacomer.osinapi.repositories.PedidoRepository;
import com.lacomer.osinapi.repositories.ProductoRepository;
import com.lacomer.osinapi.services.pedidos.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Escenarios A-E contra base de datos (H2 en modo Oracle).
// La clase no es @Transactional para que cada llamada al servicio haga su propio commit o rollback,
// asi se comprueba que cuando algo falla la base de datos queda igual.
// El escenario F (reiniciar la aplicacion) es manual, ver docs/05-pruebas.md

@SpringBootTest
@ActiveProfiles("test")
class PedidosIntegrationTest {

    @Autowired private PedidoService pedidoService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ProductoRepository productoRepository;

    private Producto leche;   // 24.50, stock 100
    private Producto pan;     // 38.90, stock 60
    private Producto atun;    // 21.90, stock 5

    @BeforeEach
    void preparar() {
        pedidoRepository.deleteAll();
        productoRepository.deleteAll();
        leche = productoRepository.save(Producto.crear("Leche entera 1 L", "Leche UHT", new BigDecimal("24.50"), 100));
        pan = productoRepository.save(Producto.crear("Pan de caja", null, new BigDecimal("38.90"), 60));
        atun = productoRepository.save(Producto.crear("Atun en agua", null, new BigDecimal("21.90"), 5));
    }

    private PedidoRequest request(Object... productoYCantidad) {
        List<ProductoDatos> items = new java.util.ArrayList<>();
        for (int i = 0; i < productoYCantidad.length; i += 2)
            items.add(new ProductoDatos(((Producto) productoYCantidad[i]).getId(), (Integer) productoYCantidad[i + 1]));
        return new PedidoRequest(items);
    }

    private int existencia(Producto p) {
        return productoRepository.findById(p.getId()).orElseThrow().getExistencias();
    }

    private void assertImporte(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    //  A: pedido con dos o mas productos

    @Test
    void escenarioA_creaPedidoConDosProductosYCalculaImportes() {
        PedidoResponse r = pedidoService.registrar(request(leche, 2, pan, 1));

        assertEquals("BORRADOR", r.estado());
        assertTrue(r.folio().matches("PED-\\d{6}"));
        assertEquals(2, r.productos().size());
        assertImporte("87.90", r.total());

        PedidoDetallesDatos dLeche = r.productos().stream()
                .filter(d -> d.idProducto().equals(leche.getId())).findFirst().orElseThrow();
        assertImporte("49.00", dLeche.subtotal());

        // crear un borrador no toca el inventario
        assertEquals(100, existencia(leche));
        assertEquals(60, existencia(pan));
    }

    @Test
    void elPedidoGuardadoSeConsultaConSusPartidasYTotal() {
        PedidoResponse creado = pedidoService.registrar(request(leche, 2, pan, 1));

        PedidoResponse consultado = pedidoService.obtenerPorId(creado.id());

        assertEquals(creado.folio(), consultado.folio());
        assertEquals(2, consultado.productos().size());
        assertImporte("87.90", consultado.total());
    }

    @Test
    void cadaPedidoRecibeUnFolioUnico() {
        PedidoResponse a = pedidoService.registrar(request(leche, 1));
        PedidoResponse b = pedidoService.registrar(request(leche, 1));

        assertNotEquals(a.folio(), b.folio());
    }

    @Test
    void productoInexistenteSeRechazaYNoDejaPedidoRegistrado() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> pedidoService.registrar(new PedidoRequest(List.of(new ProductoDatos(999999L, 1)))));

        assertEquals(0, pedidoRepository.count());
    }

    @Test
    void productoInactivoNoSePuedeAgregarAUnPedido() {
        Producto inactivo = productoRepository.findById(pan.getId()).orElseThrow();
        inactivo.desactivar();
        productoRepository.save(inactivo);

        assertThrows(RecursoNoEncontradoException.class,
                () -> pedidoService.registrar(request(pan, 1)));
    }

    @Test
    void cantidadInvalidaYProductosRepetidosSeRechazanSinGuardarNada() {
        assertThrows(DatoInvalidoException.class, () -> pedidoService.registrar(request(leche, 0)));
        assertThrows(DatoInvalidoException.class, () -> pedidoService.registrar(request(leche, 1, leche, 2)));

        assertEquals(0, pedidoRepository.count());
    }

    //  B: stock insuficiente sin cambios parciales

    @Test
    void escenarioB_confirmarConStockInsuficienteSeRechazaSinModificarInventario() {
        PedidoResponse pedido = pedidoService.registrar(request(atun, 10));   // hay 5

        assertThrows(ConflictoException.class, () -> pedidoService.confirmar(pedido.id()));

        assertEquals(5, existencia(atun));
        assertEquals("BORRADOR", pedidoService.obtenerPorId(pedido.id()).estado());
    }

    @Test
    void escenarioB_conVariasPartidasNingunProductoQuedaModificado() {
        // la leche si alcanza y el atun no, la leche no se debe descontar
        PedidoResponse pedido = pedidoService.registrar(request(leche, 2, atun, 10));

        assertThrows(ConflictoException.class, () -> pedidoService.confirmar(pedido.id()));

        assertEquals(100, existencia(leche));
        assertEquals(5, existencia(atun));
        assertEquals("BORRADOR", pedidoService.obtenerPorId(pedido.id()).estado());
    }

    //  C: confirmar pedido valido

    @Test
    void escenarioC_confirmarDescuentaInventarioYCambiaEstado() {
        PedidoResponse pedido = pedidoService.registrar(request(leche, 2, pan, 1));

        PedidoResponse confirmado = pedidoService.confirmar(pedido.id());

        assertEquals("CONFIRMADO", confirmado.estado());
        assertEquals(98, existencia(leche));
        assertEquals(59, existencia(pan));
    }

    //  D: cancelar repone una sola vez

    @Test
    void escenarioD_cancelarPedidoConfirmadoReponeInventarioUnaSolaVez() {
        PedidoResponse pedido = pedidoService.registrar(request(leche, 2, pan, 1));
        pedidoService.confirmar(pedido.id());

        PedidoResponse cancelado = pedidoService.cancelar(pedido.id());

        assertEquals("CANCELADO", cancelado.estado());
        assertNotNull(cancelado.fechaCancelacion());
        assertEquals(100, existencia(leche));
        assertEquals(60, existencia(pan));

        // segunda cancelacion: rechazada y sin duplicar inventario
        assertThrows(ConflictoException.class, () -> pedidoService.cancelar(pedido.id()));
        assertEquals(100, existencia(leche));
        assertEquals(60, existencia(pan));
    }

    //  E: transiciones no permitidas

    @Test
    void escenarioE_confirmarUnPedidoCanceladoSeRechaza() {
        PedidoResponse pedido = pedidoService.registrar(request(leche, 2));
        pedidoService.confirmar(pedido.id());
        pedidoService.cancelar(pedido.id());

        assertThrows(ConflictoException.class, () -> pedidoService.confirmar(pedido.id()));

        assertEquals(100, existencia(leche));
    }

    @Test
    void escenarioE_noSePuedeConfirmarDosVeces() {
        PedidoResponse pedido = pedidoService.registrar(request(leche, 2));
        pedidoService.confirmar(pedido.id());

        assertThrows(ConflictoException.class, () -> pedidoService.confirmar(pedido.id()));

        assertEquals(98, existencia(leche));   // descontado una sola vez
    }

    @Test
    void escenarioE_unPedidoCompletadoNoSePuedeCancelar() {
        PedidoResponse pedido = pedidoService.registrar(request(leche, 2));
        pedidoService.confirmar(pedido.id());
        PedidoResponse completado = pedidoService.completar(pedido.id());

        assertEquals("COMPLETADO", completado.estado());
        assertThrows(ConflictoException.class, () -> pedidoService.cancelar(pedido.id()));

        assertEquals(98, existencia(leche));
    }

    @Test
    void cancelarUnBorradorNoMueveElInventario() {
        PedidoResponse pedido = pedidoService.registrar(request(leche, 2));

        PedidoResponse cancelado = pedidoService.cancelar(pedido.id());

        assertEquals("CANCELADO", cancelado.estado());
        assertEquals(100, existencia(leche));
    }

    //  Concurrencia (@Version)

    @Test
    void siOtraTransaccionYaModificoElProductoElGuardadoSeRechaza() {
        // copia leida antes de confirmar, como si otra peticion la hubiera cargado al mismo tiempo
        Producto copiaVieja = productoRepository.findById(leche.getId()).orElseThrow();

        PedidoResponse pedido = pedidoService.registrar(request(leche, 2));
        pedidoService.confirmar(pedido.id());   // cambia la existencia y sube la version

        copiaVieja.aumentarExistencias(10);
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> productoRepository.save(copiaVieja));

        assertEquals(98, existencia(leche));   // no se aplico el cambio de la copia vieja
    }

    @Test
    void confirmarOCancelarUnPedidoInexistenteDa404() {
        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.confirmar(999999L));
        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.cancelar(999999L));
        assertThrows(RecursoNoEncontradoException.class, () -> pedidoService.completar(999999L));
    }

    //  Consulta con filtros

    @Test
    void listarFiltraPorEstadoYPorFolio() {
        PedidoResponse borrador = pedidoService.registrar(request(leche, 1));
        PedidoResponse confirmado = pedidoService.registrar(request(pan, 1));
        pedidoService.confirmar(confirmado.id());

        Page<PedidoResponse> todos = pedidoService.listar(null, null, PageRequest.of(0, 10));
        Page<PedidoResponse> soloConfirmados = pedidoService.listar(EstadoPedido.CONFIRMADO, null, PageRequest.of(0, 10));
        Page<PedidoResponse> porFolio = pedidoService.listar(null, borrador.folio(), PageRequest.of(0, 10));

        assertEquals(2, todos.getTotalElements());
        assertEquals(1, soloConfirmados.getTotalElements());
        assertEquals(confirmado.folio(), soloConfirmados.getContent().get(0).folio());
        assertEquals(1, porFolio.getTotalElements());
        assertEquals(borrador.folio(), porFolio.getContent().get(0).folio());
        assertImporte("38.90", soloConfirmados.getContent().get(0).total());
    }

    //  Actualizar un BORRADOR

    @Test
    void actualizarBorradorConservaElMismoProductoSinViolarElUnique() {
        PedidoResponse pedido = pedidoService.registrar(request(leche, 2, pan, 1));
        leche.actualizar("Leche entera 1 L", "Leche UHT", new BigDecimal("26.00"), 100);
        productoRepository.save(leche);

        PedidoResponse actualizado = pedidoService.actualizar(pedido.id(), request(leche, 5, atun, 1));

        assertEquals(pedido.folio(), actualizado.folio());
        assertEquals("BORRADOR", actualizado.estado());
        PedidoResponse guardado = pedidoService.obtenerPorId(pedido.id());
        assertEquals(2, guardado.productos().size());
        assertImporte("130.00", guardado.productos().stream()
                .filter(p -> p.idProducto().equals(leche.getId())).findFirst().orElseThrow().subtotal());
        assertImporte("151.90", guardado.total());
        assertEquals(100, existencia(leche));
        assertEquals(5, existencia(atun));
    }

    @Test
    void actualizarUnPedidoConfirmadoSeRechazaYNoCambiaNada() {
        PedidoResponse pedido = pedidoService.registrar(request(leche, 2));
        pedidoService.confirmar(pedido.id());

        assertThrows(ConflictoException.class,
                () -> pedidoService.actualizar(pedido.id(), request(leche, 10)));
        assertEquals(2, pedidoService.obtenerPorId(pedido.id()).productos().get(0).cantidad());
        assertEquals(98, existencia(leche));
    }
}
