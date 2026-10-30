package com.lacomer.osinapi.utils;

import com.lacomer.osinapi.exceptions.RecursoNoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

@Slf4j
public class ServiceUtils {
    public static<E, ID> E obtenerEntidadOExcepcion(
            JpaRepository<E, ID> repository,
            ID id,
            Class<E> eClass
    ){
        String nombreEntidad =eClass.getSimpleName();

        log.info("Buscando {} con id: {}", nombreEntidad, id);

        return repository.findById(id)
                .orElseThrow(() ->
                    new RecursoNoEncontradoException(
                            nombreEntidad +
                            " no encontrado con id: " +
                            id
                    )
                );
    }
}
