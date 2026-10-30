package com.lacomer.osinapi.services.pedidos;

import com.lacomer.osinapi.dto.pedidos.PedidoRequest;
import com.lacomer.osinapi.dto.pedidos.PedidoResponse;
import com.lacomer.osinapi.enums.EstadoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PedidoService {

    Page<PedidoResponse> listar(EstadoPedido estado, String folio, Pageable pageable);

    PedidoResponse obtenerPorId(Long id);

    PedidoResponse registrar(PedidoRequest request);

    PedidoResponse actualizar(Long id, PedidoRequest request);

    PedidoResponse confirmar(Long id);

    PedidoResponse cancelar(Long id);

    PedidoResponse completar(Long id);
}
