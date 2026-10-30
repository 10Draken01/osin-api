package com.lacomer.osinapi.controller;

import com.lacomer.osinapi.dto.pedidos.PedidoResponse;
import com.lacomer.osinapi.enums.EstadoPedido;
import com.lacomer.osinapi.exceptions.ConflictoException;
import com.lacomer.osinapi.exceptions.GlobalExceptionHandler;
import com.lacomer.osinapi.exceptions.RecursoNoEncontradoException;
import com.lacomer.osinapi.services.pedidos.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


// Codigos HTTP y formato de error del API sin levantar Spring completo:
// controlador y GlobalExceptionHandler reales con el servicio simulado

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock private PedidoService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new PedidoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PedidoResponse respuesta(String estado) {
        return new PedidoResponse(1L, "PED-000001", List.of(), estado,
                new BigDecimal("87.90"), LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void crearPedidoValidoResponde201ConFolioYTotal() throws Exception {
        when(service.registrar(any())).thenReturn(respuesta("BORRADOR"));

        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[{\"id\":1,\"cantidad\":2},{\"id\":2,\"cantidad\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio").value("PED-000001"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.total").value(87.90));
    }

    @Test
    void crearPedidoSinProductosResponde400() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void crearPedidoConCantidadCeroResponde400() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[{\"id\":1,\"cantidad\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearPedidoConProductoInexistenteResponde404() throws Exception {
        when(service.registrar(any())).thenThrow(new RecursoNoEncontradoException("No se encontro un producto activo con id 99"));

        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[{\"id\":99,\"cantidad\":1}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No se encontro un producto activo con id 99"));
    }

    @Test
    void pedidoNoEncontradoResponde404() throws Exception {
        when(service.obtenerPorId(99L)).thenThrow(new RecursoNoEncontradoException("Pedido no encontrado con id: 99"));

        mvc.perform(get("/api/pedidos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmarValidoResponde200() throws Exception {
        when(service.confirmar(1L)).thenReturn(respuesta("CONFIRMADO"));

        mvc.perform(post("/api/pedidos/1/confirmar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    @Test
    void confirmarConStockInsuficienteResponde409ConMensajeClaro() throws Exception {
        when(service.confirmar(1L)).thenThrow(new ConflictoException("Stock insuficiente para el producto 'Atun': disponible 5, solicitado 10"));

        mvc.perform(post("/api/pedidos/1/confirmar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Stock insuficiente para el producto 'Atun': disponible 5, solicitado 10"));
    }

    @Test
    void transicionNoPermitidaResponde409() throws Exception {
        when(service.cancelar(1L)).thenThrow(new ConflictoException("El pedido no puede cambiar a Cancelado por que esta en estado Cancelado"));

        mvc.perform(post("/api/pedidos/1/cancelar"))
                .andExpect(status().isConflict());
    }

    @Test
    void errorInesperadoResponde500SinDetallesTecnicos() throws Exception {
        when(service.confirmar(1L)).thenThrow(new IllegalStateException("detalle tecnico interno"));

        mvc.perform(post("/api/pedidos/1/confirmar"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Error interno del servidor. Contacte al administrador."));
    }

    @Test
    void listarPasaLosFiltrosYLaPaginaAlServicio() throws Exception {
        PageRequest esperado = PageRequest.of(1, 5, Sort.by("id").descending());
        when(service.listar(EstadoPedido.CONFIRMADO, "PED-0000", esperado))
                .thenReturn(new PageImpl<>(List.of(respuesta("CONFIRMADO")), esperado, 6));

        mvc.perform(get("/api/pedidos").param("estado", "CONFIRMADO").param("folio", "PED-0000")
                        .param("pagina", "1").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].folio").value("PED-000001"));

        verify(service).listar(EstadoPedido.CONFIRMADO, "PED-0000", esperado);
    }

    @Test
    void listarSinParametrosUsaPagina0Tamano20() throws Exception {
        PageRequest esperado = PageRequest.of(0, 20, Sort.by("id").descending());
        when(service.listar(null, null, esperado)).thenReturn(new PageImpl<>(List.of(), esperado, 0));

        mvc.perform(get("/api/pedidos"))
                .andExpect(status().isOk());

        verify(service).listar(null, null, esperado);
    }

    @Test
    void completarConfirmadoResponde200() throws Exception {
        when(service.completar(1L)).thenReturn(respuesta("COMPLETADO"));

        mvc.perform(post("/api/pedidos/1/completar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
    }

    @Test
    void jsonMalFormadoResponde400() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{productos:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El cuerpo de la peticion no es un JSON valido."));
    }

    @Test
    void listarConEstadoInexistenteResponde400() throws Exception {
        mvc.perform(get("/api/pedidos").param("estado", "INVENTADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El valor 'INVENTADO' no es valido para el parametro estado"));
    }

    @Test
    void actualizarBorradorResponde200() throws Exception {
        when(service.actualizar(any(), any())).thenReturn(respuesta("BORRADOR"));

        mvc.perform(put("/api/pedidos/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[{\"id\":1,\"cantidad\":3}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void actualizarPedidoNoBorradorResponde409() throws Exception {
        when(service.actualizar(any(), any())).thenThrow(
                new ConflictoException("El pedido no se puede modificar porque esta en estado Confirmado"));

        mvc.perform(put("/api/pedidos/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[{\"id\":1,\"cantidad\":3}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El pedido no se puede modificar porque esta en estado Confirmado"));
    }

    @Test
    void actualizarConListaVaciaResponde400() throws Exception {
        mvc.perform(put("/api/pedidos/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
    }
}
