package com.lacomer.osinapi.controller;

import com.lacomer.osinapi.docs.ProblemaDoc;
import com.lacomer.osinapi.services.CrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


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

    // Se reciben pagina y tamano en lugar de Pageable porque Swagger mostraba "sort": ["string"]
    // y al ejecutarlo Spring intentaba ordenar por una propiedad "string" que no existe (error 500).
    // El orden queda fijo por id
    @GetMapping
    @Operation(
            summary = "Listar registros",
            description = "Obtiene los registros paginados y ordenados por id."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de registros obtenida correctamente."
    )
    public ResponseEntity<Page<RS>> listar(
            @Parameter(description = "Numero de pagina, empieza en 0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "La pagina no puede ser negativa")
            int pagina,
            @Parameter(description = "Registros por pagina (1 a 100)")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamano minimo es 1")
            @Max(value = 100, message = "El tamano maximo es 100")
            int tamano
    ) {
        return ResponseEntity.ok(service.listar(PageRequest.of(pagina, tamano, Sort.by("id"))));
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
    @ApiResponse(
            responseCode = "409",
            description = "Hubo un conflicto al registrar un registro"
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
    @ApiResponse(
            responseCode = "409",
            description = "Hubo un conflicto al actualizar un registro"
    )
    public ResponseEntity<RS> actualizar(
            @PathVariable
            @Positive(message = "El id debe ser positivo")
            Long id,
            @Valid @RequestBody RQ request
    ) {
        return ResponseEntity.ok(service.actualizar(request, id));
    }

//    Comentado listo para implementacion futura
//    @DeleteMapping("/{id}")
//    @Operation(
//            summary = "Eliminar registro",
//            description = "Elimina un registro mediante su identificador único."
//    )
//    @ApiResponse(
//            responseCode = "204",
//            description = "Registro eliminado correctamente."
//    )
//    @ApiResponse(
//            responseCode = "404",
//            description = "No se encontró el registro que se desea eliminar."
//    )
//    public ResponseEntity<Void> eliminar(
//            @PathVariable
//            @Positive(message = "El id debe ser positivo")
//            Long id
//    ) {
//        service.eliminar(id);
//        return ResponseEntity.noContent().build();
//    }
}
