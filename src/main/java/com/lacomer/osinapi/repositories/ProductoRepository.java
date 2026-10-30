package com.lacomer.osinapi.repositories;

import com.lacomer.osinapi.entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // Se pasa el parametro "activo" en lugar de usar ...True/...False para que se aplique el converter Boolean <-> NUMBER(1)
    Optional<Producto> findByIdAndActivo(Long id, Boolean activo);

    List<Producto> findAllByActivo(Boolean activo);
}
