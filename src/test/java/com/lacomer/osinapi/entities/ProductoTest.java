package com.lacomer.osinapi.entities;

import com.lacomer.osinapi.exceptions.ConflictoException;
import com.lacomer.osinapi.exceptions.DatoInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductoTest {

    private Producto leche() {
        return Producto.crear("Leche entera 1 L", "Leche UHT", new BigDecimal("24.50"), 10);
    }

    @Test
    void crearProductoValidoQuedaActivo() {
        Producto p = leche();
        assertEquals("Leche entera 1 L", p.getNombre());
        assertEquals(10, p.getExistencias());
        assertTrue(p.getActivo());
    }

    @Test
    void crearSinDescripcionEsValido() {
        Producto p = Producto.crear("Arroz", null, new BigDecimal("28.00"), 5);
        assertNull(p.getDescripcion());
    }

    @Test
    void descripcionEnBlancoSeGuardaComoNull() {
        Producto p = Producto.crear("Arroz", "   ", new BigDecimal("28.00"), 5);
        assertNull(p.getDescripcion());
    }

    @Test
    void existenciaCeroEsValida() {
        assertEquals(0, Producto.crear("Arroz", null, new BigDecimal("28.00"), 0).getExistencias());
    }

    @Test
    void nombreVacioOSoloEspaciosSeRechaza() {
        assertThrows(DatoInvalidoException.class,
                () -> Producto.crear("   ", null, new BigDecimal("1.00"), 1));
        assertThrows(DatoInvalidoException.class,
                () -> Producto.crear(null, null, new BigDecimal("1.00"), 1));
    }

    @Test
    void nombreMayorA120CaracteresSeRechaza() {
        assertThrows(DatoInvalidoException.class,
                () -> Producto.crear("x".repeat(121), null, new BigDecimal("1.00"), 1));
    }

    @Test
    void precioCeroONegativoSeRechaza() {
        assertThrows(DatoInvalidoException.class,
                () -> Producto.crear("A", null, BigDecimal.ZERO, 1));
        assertThrows(DatoInvalidoException.class,
                () -> Producto.crear("A", null, new BigDecimal("-1"), 1));
        assertThrows(DatoInvalidoException.class,
                () -> Producto.crear("A", null, null, 1));
    }

    @Test
    void existenciaNegativaSeRechaza() {
        assertThrows(DatoInvalidoException.class,
                () -> Producto.crear("A", null, new BigDecimal("1.00"), -1));
    }

    @Test
    void decrementarDescuentaLaExistencia() {
        Producto p = leche();
        p.decrementarExistencias(4);
        assertEquals(6, p.getExistencias());
    }

    @Test
    void decrementarMasDeLaExistenciaLanzaConflictoYNoCambiaNada() {
        Producto p = leche();
        assertThrows(ConflictoException.class, () -> p.decrementarExistencias(11));
        assertEquals(10, p.getExistencias());
    }

    @Test
    void decrementarExactamenteLaExistenciaEsValido() {
        Producto p = leche();
        p.decrementarExistencias(10);
        assertEquals(0, p.getExistencias());
    }

    @Test
    void aumentarSumaLaExistenciaYRechazaCantidadInvalida() {
        Producto p = leche();
        p.aumentarExistencias(5);
        assertEquals(15, p.getExistencias());
        assertThrows(DatoInvalidoException.class, () -> p.aumentarExistencias(0));
        assertThrows(DatoInvalidoException.class, () -> p.aumentarExistencias(-3));
    }

    @Test
    void desactivarYActivarNoSePuedenRepetir() {
        Producto p = leche();
        p.desactivar();
        assertFalse(p.getActivo());
        assertThrows(ConflictoException.class, p::desactivar);
        p.activar();
        assertTrue(p.getActivo());
        assertThrows(ConflictoException.class, p::activar);
    }

    @Test
    void actualizarCambiaDatosYExistencia() {
        Producto p = leche();
        p.actualizar("Leche deslactosada 1 L", null, new BigDecimal("26.00"), 50);
        assertEquals("Leche deslactosada 1 L", p.getNombre());
        assertNull(p.getDescripcion());
        assertEquals(new BigDecimal("26.00"), p.getPrecioUnitario());
        assertEquals(50, p.getExistencias());
    }
}
