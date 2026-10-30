package com.lacomer.osinapi.services;

import java.util.List;

public interface CrudService<RQ, RS>{

    List<RS> listar();

    RS obtenerPorId(Long id);

    RS registrar(RQ request);

    RS actualizar(RQ request, Long id);

    void eliminar(Long id);
}
