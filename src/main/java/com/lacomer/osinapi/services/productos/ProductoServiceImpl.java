package com.lacomer.osinapi.services.productos;

import com.lacomer.osinapi.dto.productos.ProductoRequest;
import com.lacomer.osinapi.dto.productos.ProductoResponse;
import com.lacomer.osinapi.entities.Producto;
import com.lacomer.osinapi.exceptions.RecursoNoEncontradoException;
import com.lacomer.osinapi.mapper.ProductoMapper;
import com.lacomer.osinapi.repositories.ProductoRepository;
import com.lacomer.osinapi.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductoServiceImpl implements ProductoService{
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResponse> listar(Pageable pageable) {
        log.info("Listando productos");
        return productoRepository.findAll(pageable)
                .map(productoMapper::entidadAResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos() {
        log.info("Listando productos activos");
        return productoRepository.findAllByActivo(true).stream()
                .map(productoMapper::entidadAResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        return productoMapper.entidadAResponse(obtenerProducto(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoDesactivado(Long id) {
        log.info("Buscando producto desactivado con id {}", id);
        return productoMapper.entidadAResponse(obtenerProductoDesactivo(id));
    }

    @Override
    public ProductoResponse registrar(ProductoRequest request) {
        log.info("Registrando producto");
        Producto producto = productoMapper.requestAEntidad(request);
        productoRepository.saveAndFlush(producto);
        log.info("Producto registrado con id {}", producto.getId());
        return productoMapper.entidadAResponse(producto);
    }

    @Override
    public ProductoResponse actualizar(ProductoRequest request, Long id) {
        log.info("Actualizando producto con id {}", id);
        Producto producto = obtenerProducto(id);
        producto.actualizar(
                request.nombre(),
                request.descripcion(),
                request.precioUnitario(),
                request.existencia()
        );
        productoRepository.saveAndFlush(producto);
        log.info("Producto actualizado con id {}", producto.getId());
        return productoMapper.entidadAResponse(producto);
    }

    @Override
    public void eliminar(Long id) {
        // Vacio ya que no se especifica la eliminacion de productos, solo se puede desactivar
    }

    @Override
    public void desactivarProducto(Long id) {
        log.info("Desactivando producto con id {}", id);
        Producto producto = obtenerProductoActivo(id);
        producto.desactivar();
        productoRepository.saveAndFlush(producto);
        log.info("Producto desactivado con id {}", producto.getId());
    }

    @Override
    public void activarProducto(Long id) {
        log.info("Activando producto con id {}", id);
        Producto producto = obtenerProductoDesactivo(id);
        producto.activar();
        productoRepository.saveAndFlush(producto);
        log.info("Producto activado con id {}", producto.getId());
    }

    private Producto obtenerProductoActivo(Long id){
        log.info("Buscando producto activo con id {}", id);
        return productoRepository.findByIdAndActivo(id, true)
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("Producto no encontrado con id: " + id)
                );
    }

    private Producto obtenerProductoDesactivo(Long id){
        log.info("Buscando producto desactivo con id {}", id);
        return productoRepository.findByIdAndActivo(id, false)
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("Producto no encontrado con id: " + id)
                );
    }

    private Producto obtenerProducto(Long id){
        return ServiceUtils.obtenerEntidadOExcepcion(
                productoRepository,
                id,
                Producto.class
        );
    }
}
