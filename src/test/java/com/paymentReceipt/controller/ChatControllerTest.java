package com.paymentReceipt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentReceipt.exception.GlobalExceptionHandler;
import com.paymentReceipt.model.dto.request.ChatRequest;
import com.paymentReceipt.model.dto.response.ChatResponse;
import com.paymentReceipt.model.dto.response.ComprobanteResponse;
import com.paymentReceipt.model.entity.MensajeChat;
import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.RolMensaje;
import com.paymentReceipt.model.enums.TipoComprobante;
import com.paymentReceipt.service.ChatService;
import com.paymentReceipt.service.ComprobanteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ComprobanteService comprobanteService;

    @Mock
    private ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService, comprobanteService, chatClient))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void enviarMensajeDebeRetornarRespuestaDelServicio() throws Exception {
        when(chatService.procesarMensaje("Hola")).thenReturn(ChatResponse.of("respuesta"));

        mockMvc.perform(post("/api/chat/mensaje")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("Hola"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mensaje").value("respuesta"));
    }

    @Test
    void enviarMensajeConTextoVacioDebeFallarValidacion() throws Exception {
        mockMvc.perform(post("/api/chat/mensaje")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Errores de validación"));
    }

    @Test
    void analizarTextoDebeRetornarBadRequestSiEstaVacio() throws Exception {
        mockMvc.perform(post("/api/chat/analizar-texto")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El texto no puede estar vacío"));
    }

    @Test
    void analizarArchivoDebeRetornarBadRequestSiElArchivoEstaVacio() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/chat/analizar").file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El archivo está vacío"));
    }

    @Test
    void obtenerHistorialDebeRetornarMensajes() throws Exception {
        MensajeChat mensaje = MensajeChat.builder()
                .id("m-1")
                .rol(RolMensaje.USER)
                .contenido("hola")
                .build();
        when(chatService.obtenerHistorial()).thenReturn(List.of(mensaje));

        mockMvc.perform(get("/api/chat/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].contenido").value("hola"));
    }

    @Test
    void limpiarHistorialDebeResponderOk() throws Exception {
        doNothing().when(chatService).limpiarHistorial();

        mockMvc.perform(delete("/api/chat/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Historial del chat eliminado"));
    }

    @Test
    void analizarArchivoDebeDelegarAlServicio() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "comprobante.pdf",
                "application/pdf",
                "contenido".getBytes()
        );

        ComprobanteResponse response = new ComprobanteResponse(
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
        when(comprobanteService.analizarYPersistir(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/chat/analizar").file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("cmp-1"));
    }
}
