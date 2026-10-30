package com.lacomer.osinapi.controller;

import com.lacomer.osinapi.dto.productos.ProductoResponse;
import com.lacomer.osinapi.exceptions.ConflictoException;
import com.lacomer.osinapi.exceptions.GlobalExceptionHandler;
import com.lacomer.osinapi.exceptions.RecursoNoEncontradoException;
import com.lacomer.osinapi.services.productos.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Codigos HTTP y validaciones del API de productos sin levantar Spring completo:
// controlador y GlobalExceptionHandler reales con el servicio simulado
@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock private ProductoService service;
    private MockMvc mvc;

    private static final String PRODUCTO_VALIDO =
            "{\"nombre\":\"Leche 1 L\",\"descripcion\":\"Leche entera\",\"precioUnitario\":27.00,\"existencia\":120}";

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ProductoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ProductoResponse leche() {
        return new ProductoResponse(5L, "Leche 1 L", "Leche entera", new BigDecimal("27.00"), 120, true);
    }

    //  consulta

    @Test
    void listarSinParametrosUsaPagina0Tamano20OrdenadoPorId() throws Exception {
        PageRequest esperado = PageRequest.of(0, 20, Sort.by("id"));
        when(service.listar(esperado)).thenReturn(new PageImpl<>(List.of(leche()), esperado, 1));

        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Leche 1 L"));

        verify(service).listar(esperado);
    }

    @Test
    void listarPasaPaginaYTamanoAlServicio() throws Exception {
        PageRequest esperado = PageRequest.of(2, 5, Sort.by("id"));
        when(service.listar(esperado)).thenReturn(new PageImpl<>(List.of(), esperado, 0));

        mvc.perform(get("/api/productos").param("pagina", "2").param("tamano", "5"))
                .andExpect(status().isOk());

        verify(service).listar(esperado);
    }

    @Test
    void listarActivosResponde200() throws Exception {
        when(service.listarActivos()).thenReturn(List.of(leche()));

        mvc.perform(get("/api/productos/activos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].existencia").value(120));
    }

    @Test
    void obtenerPorIdResponde200() throws Exception {
        when(service.obtenerPorId(5L)).thenReturn(leche());

        mvc.perform(get("/api/productos/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioUnitario").value(27.00))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void obtenerPorIdInexistenteResponde404() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("Producto no encontrado con id: 99"));

        mvc.perform(get("/api/productos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Producto no encontrado con id: 99"));
    }

    //  registrar

    @Test
    void registrarProductoValidoResponde201() throws Exception {
        when(service.registrar(any())).thenReturn(leche());

        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void registrarSinDescripcionEsValido() throws Exception {
        when(service.registrar(any())).thenReturn(leche());

        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Arroz\",\"precioUnitario\":24.90,\"existencia\":0}"))
                .andExpect(status().isCreated());
    }

    @Test
    void registrarConPrecioCeroResponde400() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Arroz\",\"precioUnitario\":0,\"existencia\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("precioUnitario: El precio unitario debe ser mayor a 0"));

        verify(service, never()).registrar(any());
    }

    @Test
    void registrarConExistenciaNegativaResponde400() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Arroz\",\"precioUnitario\":24.90,\"existencia\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("existencia: La existencia del producto no puede ser negativa"));
    }

    @Test
    void registrarSinNombreResponde400() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  \",\"precioUnitario\":24.90,\"existencia\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0]").value("nombre: El nombre del producto es requerido"));
    }

    @Test
    void registrarNombreDuplicadoResponde409SinDetallesTecnicos() throws Exception {
        when(service.registrar(any())).thenThrow(new DataIntegrityViolationException("ORA-00001: unique constraint (OSIN.UQ_PRODUCTOS_NOMBRE) violated"));

        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La operacion viola una restriccion de datos (por ejemplo, un registro duplicado)."));
    }

    //  actualizar

    @Test
    void actualizarProductoResponde200() throws Exception {
        when(service.actualizar(any(), eq(5L))).thenReturn(leche());

        mvc.perform(put("/api/productos/5").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Leche 1 L"));
    }

    @Test
    void actualizarProductoInexistenteResponde404() throws Exception {
        when(service.actualizar(any(), eq(99L))).thenThrow(new RecursoNoEncontradoException("Producto no encontrado con id: 99"));

        mvc.perform(put("/api/productos/99").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO_VALIDO))
                .andExpect(status().isNotFound());
    }

    //  activar / desactivar

    @Test
    void desactivarResponde204() throws Exception {
        mvc.perform(patch("/api/productos/5/desactivar"))
                .andExpect(status().isNoContent());

        verify(service).desactivarProducto(5L);
    }

    @Test
    void desactivarProductoYaDesactivadoResponde404() throws Exception {
        doThrow(new RecursoNoEncontradoException("Producto no encontrado con id: 5"))
                .when(service).desactivarProducto(5L);

        mvc.perform(patch("/api/productos/5/desactivar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void activarResponde204() throws Exception {
        mvc.perform(patch("/api/productos/5/activar"))
                .andExpect(status().isNoContent());

        verify(service).activarProducto(5L);
    }

    @Test
    void activarConConflictoResponde409() throws Exception {
        doThrow(new ConflictoException("El producto ya se encuentra activado"))
                .when(service).activarProducto(5L);

        mvc.perform(patch("/api/productos/5/activar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El producto ya se encuentra activado"));
    }
}