package com.lacomer.osinapi.enums;

import com.lacomer.osinapi.exceptions.ConflictoException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum EstadoPedido {
    // Estado inicial, todavia no descuenta inventario
    BORRADOR(1L,"Borrador", true) {
        @Override
        public Set<EstadoPedido> puedeCambiar() {
            return Set.of(CONFIRMADO, CANCELADO);
        }
    },
    // Inventario descontado, falta finalizar la compra
    CONFIRMADO(2L, "Confirmado", false) {
        @Override
        public Set<EstadoPedido> puedeCambiar() {
            return Set.of(CANCELADO, COMPLETADO);
        }
    },
    // Estado final de compra
    COMPLETADO(3L, "Completado", false) {
        @Override
        public Set<EstadoPedido> puedeCambiar() {
            return Set.of();
        }
    },
    // Estado final, si estaba confirmado ya se regreso el inventario
    CANCELADO(4L, "Cancelado", false) {
        @Override
        public Set<EstadoPedido> puedeCambiar() {
            return Set.of();
        }
    };

    private final Long codigo;
    private final String descripcion;

    private final boolean actualizable;

    public abstract Set<EstadoPedido> puedeCambiar();

    public boolean puedeCambiarA(EstadoPedido estadoPedido){
        return puedeCambiar().contains(estadoPedido);
    }

    public static EstadoPedido obtenerEstadoPedidoPorCodigo(Long codigo){
        for(EstadoPedido e : values()){
            if(e.codigo.equals(codigo))
                return e;
        }
        throw new ConflictoException("El estado con codigo: " + codigo + " no existe");
    }
}
