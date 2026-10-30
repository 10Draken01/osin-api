
package com.lacomer.osinapi.controller;

import com.lacomer.osinapi.docs.ProblemaDoc;
import com.lacomer.osinapi.services.CrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@Validated
@ApiResponse(
        responseCode = "400",
        description = "Solicitud incorrecta",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemaDoc.class)
        )
)
@ApiResponse(
        responseCode = "500",
        description = "Error interno del servidor",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemaDoc.class)
        )
)
public class CrudController<RQ, RS, S extends CrudService<RQ, RS>> {

    protected final S service;

    @GetMapping
    @Operation(
            summary = "Listar registros",
            description = "Obtiene una lista de todos los registros disponibles."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de registros obtenida correctamente."
    )
    public ResponseEntity<List<RS>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener registro por ID",
            description = "Busca un registro mediante su identificador único."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registro encontrado correctamente."
    )
    @ApiResponse(
            responseCode = "404",
            description = "No se encontró el registro solicitado."
    )
    public ResponseEntity<RS> obtenerPorId(
            @PathVariable
            @Positive(message = "El id debe ser positivo")
            Long id
    ) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    @Operation(
            summary = "Registrar registro",
            description = "Crea un nuevo registro con los datos proporcionados."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Registro creado correctamente."
    )
    public ResponseEntity<RS> registrar(
            @Valid @RequestBody RQ request
    ) {
        RS resultado = service.registrar(request);
        return ResponseEntity.status(201).body(resultado);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar registro",
            description = "Actualiza los datos de un registro existente mediante su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registro actualizado correctamente."
    )
    @ApiResponse(
            responseCode = "404",
            description = "No se encontró el registro que se desea actualizar."
    )
    public ResponseEntity<RS> actualizar(
            @PathVariable
            @Positive(message = "El id debe ser positivo")
            Long id,
            @Valid @RequestBody RQ request
    ) {
        return ResponseEntity.ok(service.actualizar(request, id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar registro",
            description = "Elimina un registro mediante su identificador único."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Registro eliminado correctamente."
    )
    @ApiResponse(
            responseCode = "404",
            description = "No se encontró el registro que se desea eliminar."
    )
    public ResponseEntity<Void> eliminar(
            @PathVariable
            @Positive(message = "El id debe ser positivo")
            Long id
    ) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
