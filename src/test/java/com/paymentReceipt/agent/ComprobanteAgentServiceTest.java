package com.paymentReceipt.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentReceipt.agent.ComprobanteAgentService.AgentResult;
import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComprobanteAgentServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private ChatClient.PromptUserSpec userSpec;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private ComprobanteAgentService service;

    @BeforeEach
    void setUp() {
        service = new ComprobanteAgentService(chatClient, objectMapper);
    }

    @Test
    void analizarImagenDebeAdjuntarMediaAlPrompt() throws Exception {
        AgentResult expected = new AgentResult(
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
                List.of()
        );

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        ArgumentCaptor<Consumer<ChatClient.PromptUserSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(requestSpec.user(captor.capture())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(objectMapper.writeValueAsString(expected));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo",
                "comprobante.png",
                "image/png",
                "imagen".getBytes()
        );

        AgentResult result = service.analizarImagen(archivo);

        assertThat(result.tipo()).isEqualTo(TipoComprobante.FACTURA);
        captor.getValue().accept(userSpec);
        verify(userSpec).text(org.mockito.ArgumentMatchers.contains("imagen"));
        ArgumentCaptor<MimeType> mimeCaptor = ArgumentCaptor.forClass(MimeType.class);
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(userSpec).media(mimeCaptor.capture(), resourceCaptor.capture());
        assertThat(mimeCaptor.getValue().toString()).isEqualTo("image/png");
        assertThat(resourceCaptor.getValue().getFilename()).isEqualTo("comprobante.png");
    }
}
