package com.lacomer.osinapi.enums;

import org.junit.jupiter.api.Test;

import static com.lacomer.osinapi.enums.EstadoPedido.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoPedidoTest {

    @Test
    void borradorPuedePasarAConfirmadoOCancelado() {
        assertTrue(BORRADOR.puedeCambiarA(CONFIRMADO));
        assertTrue(BORRADOR.puedeCambiarA(CANCELADO));
        assertFalse(BORRADOR.puedeCambiarA(COMPLETADO));
        assertFalse(BORRADOR.puedeCambiarA(BORRADOR));
    }

    @Test
    void confirmadoPuedePasarACanceladoOCompletado() {
        assertTrue(CONFIRMADO.puedeCambiarA(CANCELADO));
        assertTrue(CONFIRMADO.puedeCambiarA(COMPLETADO));
        assertFalse(CONFIRMADO.puedeCambiarA(BORRADOR));
        assertFalse(CONFIRMADO.puedeCambiarA(CONFIRMADO));
    }

    @Test
    void canceladoYCompletadoSonEstadosFinales() {
        for (EstadoPedido destino : EstadoPedido.values()) {
            assertFalse(CANCELADO.puedeCambiarA(destino), "CANCELADO -> " + destino);
            assertFalse(COMPLETADO.puedeCambiarA(destino), "COMPLETADO -> " + destino);
        }
    }
}
