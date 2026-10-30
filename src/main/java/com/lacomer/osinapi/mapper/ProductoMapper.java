package com.lacomer.osinapi.mapper;

import com.lacomer.osinapi.dto.productos.ProductoRequest;
import com.lacomer.osinapi.dto.productos.ProductoResponse;
import com.lacomer.osinapi.entities.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper implements CommonMapper<ProductoRequest, ProductoResponse, Producto>{
    @Override
    public Producto requestAEntidad(ProductoRequest request) {
        return request == null ? null
                : Producto.crear(
                        request.nombre(),
                        request.descripcion(),
                        request.precioUnitario(),
                        request.existencia()
        );
    }

    @Override
    public ProductoResponse entidadAResponse(Producto producto) {
        return producto == null ? null
                : new ProductoResponse(
                        producto.getId(),
                        producto.getNombre(),
                        producto.getDescripcion(),
                        producto.getPrecioUnitario(),
                        producto.getExistencias(),
                        producto.getActivo()
        );
    }
}
