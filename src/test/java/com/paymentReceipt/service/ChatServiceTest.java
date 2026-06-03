package com.paymentReceipt.service;

import com.paymentReceipt.agent.ComprobanteAgentService;
import com.paymentReceipt.model.dto.response.ChatResponse;
import com.paymentReceipt.model.entity.MensajeChat;
import com.paymentReceipt.model.enums.RolMensaje;
import com.paymentReceipt.repository.MensajeChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MensajeChatRepository mensajeChatRepository;

    @Mock
    private ComprobanteAgentService agentService;

    @Mock
    private ComprobanteService comprobanteService;

    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(mensajeChatRepository, agentService, comprobanteService);
    }

    @Test
    void procesarMensajeDebePersistirUsuarioYRespuesta() {
        when(comprobanteService.construirContextoParaChat("hola")).thenReturn("contexto");
        when(agentService.responderConsulta("hola", "contexto")).thenReturn("respuesta");
        when(mensajeChatRepository.save(any(MensajeChat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.procesarMensaje("hola");

        assertThat(response.mensaje()).isEqualTo("respuesta");

        ArgumentCaptor<MensajeChat> captor = ArgumentCaptor.forClass(MensajeChat.class);
        verify(mensajeChatRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(MensajeChat::getRol)
                .containsExactly(RolMensaje.USER, RolMensaje.ASSISTANT);
        assertThat(captor.getAllValues()).extracting(MensajeChat::getContenido)
                .containsExactly("hola", "respuesta");
    }

    @Test
    void obtenerHistorialDebeInvertirElOrdenRecibido() {
        MensajeChat primero = MensajeChat.builder().id("1").contenido("primero").rol(RolMensaje.USER).build();
        MensajeChat segundo = MensajeChat.builder().id("2").contenido("segundo").rol(RolMensaje.ASSISTANT).build();
        when(mensajeChatRepository.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of(primero, segundo));

        List<MensajeChat> historial = service.obtenerHistorial();

        assertThat(historial).containsExactly(segundo, primero);
    }

    @Test
    void limpiarHistorialDebeEliminarTodo() {
        service.limpiarHistorial();

        verify(mensajeChatRepository).deleteAllInBatch();
    }
}
