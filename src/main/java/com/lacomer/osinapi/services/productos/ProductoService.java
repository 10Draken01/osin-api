package com.lacomer.osinapi.services.productos;

import com.lacomer.osinapi.dto.productos.ProductoRequest;
import com.lacomer.osinapi.dto.productos.ProductoResponse;
import com.lacomer.osinapi.services.CrudService;

import java.util.List;

public interface ProductoService extends CrudService<ProductoRequest, ProductoResponse> {

    List<ProductoResponse> listarActivos();

    ProductoResponse obtenerProductoDesactivado(Long id);

    void desactivarProducto(Long id);
    void activarProducto(Long id);
}
