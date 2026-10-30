package com.lacomer.osinapi.repositories;

import com.lacomer.osinapi.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

    // Secuencia creada en 01_schema.sql, se usa para armar el folio PED-000001
    @Query(value = "SELECT SEQ_PEDIDOS_FOLIO.NEXTVAL FROM DUAL", nativeQuery = true)
    Long obtenerSiguienteFolio();
}
