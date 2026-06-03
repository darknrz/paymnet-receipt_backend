package com.paymentReceipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentReceipt.exception.ComprobanteNotFoundException;
import com.paymentReceipt.exception.GlobalExceptionHandler;
import com.paymentReceipt.model.dto.request.ComprobanteUpdateRequest;
import com.paymentReceipt.model.dto.response.ComprobanteResponse;
import com.paymentReceipt.model.dto.response.InsightResponse;
import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;
import com.paymentReceipt.service.ComprobanteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ComprobanteControllerTest {

    @Mock
    private ComprobanteService service;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ComprobanteController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listarTodosDebeRetornarOk() throws Exception {
        ComprobanteResponse response = crearResponse();
        when(service.listarTodos()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/comprobantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("cmp-1"))
                .andExpect(jsonPath("$.data[0].tipo").value("FACTURA"));
    }

    @Test
    void actualizarDebeRetornarComprobanteActualizado() throws Exception {
        when(service.actualizar(eq("cmp-1"), any(ComprobanteUpdateRequest.class)))
                .thenReturn(crearResponse());

        ComprobanteUpdateRequest request = new ComprobanteUpdateRequest(
                TipoComprobante.FACTURA,
                null,
                "0009",
                LocalDate.of(2026, 6, 1),
                null,
                "Emisor actualizado",
                null,
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                new BigDecimal("118.00"),
                "PEN",
                EstadoComprobante.VALIDO,
                "Observaciones"
        );

        mockMvc.perform(patch("/api/comprobantes/cmp-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comprobante actualizado"))
                .andExpect(jsonPath("$.data.id").value("cmp-1"));
    }

    @Test
    void eliminarDebeRetornarMensajeExitoso() throws Exception {
        doNothing().when(service).eliminar("cmp-1");

        mockMvc.perform(delete("/api/comprobantes/cmp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comprobante eliminado"));
    }

    @Test
    void obtenerPorIdCuandoNoExisteDebeRetornar404() throws Exception {
        when(service.obtenerPorId("missing")).thenThrow(new ComprobanteNotFoundException("missing"));

        mockMvc.perform(get("/api/comprobantes/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("missing")));
    }

    @Test
    void insightsDebeRetornarResumen() throws Exception {
        when(service.obtenerInsights()).thenReturn(new InsightResponse(
                3L,
                2L,
                1L,
                0L,
                new BigDecimal("250.00"),
                new BigDecimal("83.33"),
                java.util.Map.of("FACTURA", 2L),
                java.util.Map.of("PEN", 3L)
        ));

        mockMvc.perform(get("/api/comprobantes/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalComprobantes").value(3))
                .andExpect(jsonPath("$.data.porTipo.FACTURA").value(2));
    }

    private ComprobanteResponse crearResponse() {
        return new ComprobanteResponse(
                "cmp-1",
                TipoComprobante.FACTURA,
                "F001",
                "000123",
                LocalDate.of(2026, 6, 1),
                "20123456789",
                "Empresa S.A.",
                "Av. Siempre Viva 123",
                "10456789012",
                "Cliente S.A.C.",
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                new BigDecimal("118.00"),
                "PEN",
                EstadoComprobante.VALIDO,
                "archivo.pdf",
                "Sin observaciones",
                List.of(),
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 1, 10, 5)
        );
    }
}
