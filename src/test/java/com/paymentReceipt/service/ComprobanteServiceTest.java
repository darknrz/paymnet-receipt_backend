package com.paymentReceipt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentReceipt.agent.ArchivoExtractionService;
import com.paymentReceipt.agent.ComprobanteAgentService;
import com.paymentReceipt.agent.ComprobanteAgentService.AgentResult;
import com.paymentReceipt.agent.ComprobanteAgentService.AgentResult.ItemResult;
import com.paymentReceipt.exception.ComprobanteNotFoundException;
import com.paymentReceipt.model.dto.request.ComprobanteUpdateRequest;
import com.paymentReceipt.model.dto.response.ComprobanteResponse;
import com.paymentReceipt.model.dto.response.InsightResponse;
import com.paymentReceipt.model.entity.Comprobante;
import com.paymentReceipt.model.entity.ItemComprobante;
import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;
import com.paymentReceipt.repository.ComprobanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprobanteServiceTest {

    @Mock
    private ComprobanteRepository repository;

    @Mock
    private ComprobanteAgentService agentService;

    @Mock
    private ArchivoExtractionService extractionService;

    private final ComprobanteMapper mapper = new ComprobanteMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private ComprobanteService service;

    @BeforeEach
    void setUp() {
        service = new ComprobanteService(repository, agentService, extractionService, mapper, objectMapper);
    }

    @Test
    void analizarYPersistirDebeExtraerAnalizarYGuardar() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "comprobante.pdf",
                "application/pdf",
                "contenido".getBytes()
        );

        AgentResult resultado = crearResultadoAgent();
        when(extractionService.extraerTexto(archivo)).thenReturn("texto extraido");
        when(agentService.analizar("texto extraido")).thenReturn(resultado);
        when(repository.save(any(Comprobante.class))).thenAnswer(invocation -> {
            Comprobante comprobante = invocation.getArgument(0);
            comprobante.setId("cmp-1");
            return comprobante;
        });

        ComprobanteResponse response = service.analizarYPersistir(archivo);

        assertThat(response.id()).isEqualTo("cmp-1");
        assertThat(response.tipo()).isEqualTo(TipoComprobante.FACTURA);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).descripcion()).isEqualTo("Producto 1");
        verify(extractionService).extraerTexto(archivo);
        verify(agentService).analizar("texto extraido");
        verify(repository).save(any(Comprobante.class));
    }

    @Test
    void analizarYPersistirDebeUsarAnalisisMultimodalParaImagen() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "comprobante.png",
                "image/png",
                "imagen".getBytes()
        );

        AgentResult resultado = crearResultadoAgent();
        when(agentService.analizarImagen(archivo)).thenReturn(resultado);
        when(repository.save(any(Comprobante.class))).thenAnswer(invocation -> {
            Comprobante comprobante = invocation.getArgument(0);
            comprobante.setId("cmp-img");
            return comprobante;
        });

        ComprobanteResponse response = service.analizarYPersistir(archivo);

        assertThat(response.id()).isEqualTo("cmp-img");
        verify(agentService).analizarImagen(archivo);
        verifyNoInteractions(extractionService);
    }

    @Test
    void analizarTextoDebeGuardarYRetornarRespuesta() throws Exception {
        AgentResult resultado = crearResultadoAgent();
        when(agentService.analizar("texto libre")).thenReturn(resultado);
        when(repository.save(any(Comprobante.class))).thenAnswer(invocation -> {
            Comprobante comprobante = invocation.getArgument(0);
            comprobante.setId("cmp-2");
            return comprobante;
        });

        ComprobanteResponse response = service.analizarTexto("texto libre");

        assertThat(response.id()).isEqualTo("cmp-2");
        assertThat(response.estado()).isEqualTo(EstadoComprobante.VALIDO);
        verify(agentService).analizar("texto libre");
        verify(repository).save(any(Comprobante.class));
    }

    @Test
    void obtenerPorIdDebeLanzarExcepcionSiNoExiste() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId("missing"))
                .isInstanceOf(ComprobanteNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void actualizarDebeModificarSoloCamposPresentes() {
        Comprobante existente = Comprobante.builder()
                .id("cmp-3")
                .tipo(TipoComprobante.BOLETA)
                .serie("B001")
                .numero("0001")
                .estado(EstadoComprobante.PENDIENTE)
                .items(new ArrayList<>())
                .build();

        when(repository.findById("cmp-3")).thenReturn(Optional.of(existente));
        when(repository.save(any(Comprobante.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
                "USD",
                EstadoComprobante.VALIDO,
                "Observaciones"
        );

        ComprobanteResponse response = service.actualizar("cmp-3", request);

        assertThat(response.tipo()).isEqualTo(TipoComprobante.FACTURA);
        assertThat(response.numero()).isEqualTo("0009");
        assertThat(response.emisorRazonSocial()).isEqualTo("Emisor actualizado");
        assertThat(response.moneda()).isEqualTo("USD");
        assertThat(response.estado()).isEqualTo(EstadoComprobante.VALIDO);
        verify(repository).save(existente);
    }

    @Test
    void eliminarDebeBorrarCuandoExiste() {
        when(repository.existsById("cmp-4")).thenReturn(true);

        service.eliminar("cmp-4");

        verify(repository).deleteById("cmp-4");
    }

    @Test
    void obtenerInsightsDebeConstruirResumenConConteos() {
        when(repository.count()).thenReturn(3L);
        when(repository.findByEstado(EstadoComprobante.VALIDO)).thenReturn(List.of(new Comprobante(), new Comprobante()));
        when(repository.findByEstado(EstadoComprobante.INVALIDO)).thenReturn(List.of(new Comprobante()));
        when(repository.findByEstado(EstadoComprobante.PENDIENTE)).thenReturn(List.of());
        when(repository.sumTotalByEstado(EstadoComprobante.VALIDO)).thenReturn(new BigDecimal("250.00"));
        when(repository.avgTotal()).thenReturn(new BigDecimal("83.33"));
        when(repository.countByTipo()).thenReturn(List.of(new Object[]{TipoComprobante.FACTURA, 2L}, new Object[]{TipoComprobante.BOLETA, 1L}));
        when(repository.countByMoneda()).thenReturn(List.of(new Object[]{"PEN", 2L}, new Object[]{null, 1L}));

        InsightResponse insights = service.obtenerInsights();

        assertThat(insights.totalComprobantes()).isEqualTo(3L);
        assertThat(insights.comprobantesValidos()).isEqualTo(2L);
        assertThat(insights.porTipo()).containsEntry("FACTURA", 2L).containsEntry("BOLETA", 1L);
        assertThat(insights.porMoneda()).containsEntry("PEN", 2L).containsEntry("SIN_MONEDA", 1L);
    }

    @Test
    void construirContextoParaChatDebeUsarComprobantesRelacionados() {
        Comprobante comprobante = Comprobante.builder()
                .tipo(TipoComprobante.FACTURA)
                .serie("F001")
                .numero("000123")
                .emisorRazonSocial("Empresa S.A.")
                .moneda("PEN")
                .total(new BigDecimal("118.00"))
                .estado(EstadoComprobante.VALIDO)
                .items(new ArrayList<>())
                .build();

        when(repository.buscarTextoLibre("empresa")).thenReturn(List.of(comprobante));

        String contexto = service.construirContextoParaChat("empresa");

        assertThat(contexto).contains("Comprobantes encontrados");
        assertThat(contexto).contains("Empresa S.A.");
        verify(repository).buscarTextoLibre("empresa");
        verifyNoInteractions(agentService, extractionService);
    }

    private AgentResult crearResultadoAgent() {
        List<ItemResult> items = List.of(
                new ItemResult("Producto 1", new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"))
        );

        return new AgentResult(
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
                "Sin observaciones",
                items
        );
    }
}
