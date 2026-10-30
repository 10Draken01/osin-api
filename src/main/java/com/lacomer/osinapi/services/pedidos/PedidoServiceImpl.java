package com.lacomer.osinapi.services.pedidos;

import com.lacomer.osinapi.dto.datos.ProductoDatos;
import com.lacomer.osinapi.dto.pedidos.PedidoRequest;
import com.lacomer.osinapi.dto.pedidos.PedidoResponse;
import com.lacomer.osinapi.entities.Pedido;
import com.lacomer.osinapi.entities.PedidoDetalle;
import com.lacomer.osinapi.entities.Producto;
import com.lacomer.osinapi.enums.EstadoPedido;
import com.lacomer.osinapi.exceptions.DatoInvalidoException;
import com.lacomer.osinapi.exceptions.RecursoNoEncontradoException;
import com.lacomer.osinapi.mapper.PedidoMapper;
import com.lacomer.osinapi.repositories.PedidoRepository;
import com.lacomer.osinapi.repositories.ProductoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PedidoServiceImpl implements PedidoService {
    private final PedidoRepository pedidoRepository;
    private final PedidoMapper pedidoMapper;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PedidoResponse> listar(EstadoPedido estado, String folio, Pageable pageable) {
        log.info("Listando pedidos (estado={}, folio={})", estado, folio);
        return pedidoRepository.findAll(filtros(estado, folio), pageable)
                .map(pedidoMapper::entidadAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtenerPorId(Long id) {
        return pedidoMapper.entidadAResponse(obtenerPedido(id));
    }

    // Crea el pedido en BORRADOR, aqui no se descuenta inventario (se descuenta al confirmar).
    // El cliente solo manda id y cantidad, el precio se toma del producto y los importes se calculan aqui
    @Override
    public PedidoResponse registrar(PedidoRequest request) {
        validarProductosDelPedido(request);

        String folio = Pedido.formatearFolio(pedidoRepository.obtenerSiguienteFolio());
        log.info("Registrando pedido con folio {}", folio);

        Pedido pedido = pedidoMapper.requestAEntidad(request, folio);
        for (ProductoDatos p : request.productos()) {
            Producto producto = obtenerProductoActivo(p.id());
            pedido.agregarPedidoDetalle(pedidoMapper.productoDatosAPedidoDetalle(producto, p.cantidad()));
        }
        pedidoRepository.saveAndFlush(pedido);

        return pedidoMapper.entidadAResponse(pedido);
    }

    // Solo BORRADOR (409 en otro estado). Mismas validaciones que registrar; las partidas
    // se reemplazan y todas toman el precio vigente del catalogo. No mueve inventario
    @Override
    public PedidoResponse actualizar(Long id, PedidoRequest request) {
        log.info("Actualizando pedido con id {}", id);
        validarProductosDelPedido(request);

        Pedido pedido = obtenerPedido(id);
        pedido.validarQueSeaActualizable();

        List<PedidoDetalle> partidas = new ArrayList<>();
        for (ProductoDatos p : request.productos()) {
            Producto producto = obtenerProductoActivo(p.id());
            partidas.add(pedidoMapper.productoDatosAPedidoDetalle(producto, p.cantidad()));
        }
        pedido.actualizarPartidas(partidas);
        pedidoRepository.saveAndFlush(pedido);

        return pedidoMapper.entidadAResponse(pedido);
    }

    // Todo el metodo corre en una transaccion (@Transactional de la clase).
    // Si alguna partida no tiene stock se lanza ConflictoException, se hace rollback
    // y el pedido sigue en BORRADOR sin que se modifique ningun producto
    @Override
    public PedidoResponse confirmar(Long id) {
        log.info("Confirmando pedido con id {}", id);
        Pedido pedido = obtenerPedido(id);
        pedido.confirmar();
        pedidoRepository.saveAndFlush(pedido);
        return pedidoMapper.entidadAResponse(pedido);
    }

    @Override
    public PedidoResponse cancelar(Long id) {
        log.info("Cancelando pedido con id {}", id);
        Pedido pedido = obtenerPedido(id);
        pedido.cancelar();
        pedidoRepository.saveAndFlush(pedido);
        return pedidoMapper.entidadAResponse(pedido);
    }

    @Override
    public PedidoResponse completar(Long id) {
        log.info("Completando pedido con id {}", id);
        Pedido pedido = obtenerPedido(id);
        pedido.completar();
        pedidoRepository.saveAndFlush(pedido);
        return pedidoMapper.entidadAResponse(pedido);
    }

    private Specification<Pedido> filtros(EstadoPedido estado, String folio) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (estado != null)
                condiciones.add(cb.equal(root.get("estadoPedido"), estado));
            // Se indica el caracter de escape '\' porque si no se pone, Hibernate en H2 agrega ESCAPE ''
            // y en modo Oracle '' es NULL, entonces el LIKE nunca encuentra nada
            if (folio != null && !folio.isBlank())
                condiciones.add(cb.like(cb.upper(root.<String>get("folio")), "%" + folio.trim().toUpperCase() + "%", '\\'));
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    // Una partida por producto (UNIQUE PEDIDO_ID, PRODUCTO_ID en la BD)
    private void validarProductosDelPedido(PedidoRequest request) {
        if (request == null || request.productos() == null || request.productos().isEmpty())
            throw new DatoInvalidoException("El pedido debe contener al menos un producto");

        Set<Long> ids = new HashSet<>();
        for (ProductoDatos p : request.productos()) {
            if (p == null || p.id() == null)
                throw new DatoInvalidoException("El id del producto es requerido");
            if (!ids.add(p.id()))
                throw new DatoInvalidoException("El producto con id " + p.id() + " esta repetido en el pedido");
        }
    }

    private Pedido obtenerPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado con id: " + id));
    }

    private Producto obtenerProductoActivo(Long id) {
        log.info("Buscando producto activo con id {}", id);
        return productoRepository.findByIdAndActivo(id, true)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontro un producto activo con id " + id));
    }
}