package com.lacomer.osinapi.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CrudService<RQ, RS>{

    Page<RS> listar(Pageable pageable);

    RS obtenerPorId(Long id);

    RS registrar(RQ request);

    RS actualizar(RQ request, Long id);

    void eliminar(Long id);
}
