package com.paymentReceipt.service;

import com.paymentReceipt.agent.ComprobanteAgentService;
import com.paymentReceipt.model.dto.response.ChatResponse;
import com.paymentReceipt.model.entity.MensajeChat;
import com.paymentReceipt.model.enums.RolMensaje;
import com.paymentReceipt.repository.MensajeChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatService {

    private final MensajeChatRepository mensajeChatRepository;
    private final ComprobanteAgentService agentService;
    private final ComprobanteService comprobanteService;

    /**
     * Procesa un mensaje de texto del usuario en el chat.
     */
    @Transactional
    public ChatResponse procesarMensaje(String mensajeUsuario) {
        log.info("Procesando mensaje del chat: {}", mensajeUsuario);

        // 1. Persistir mensaje del usuario
        persistirMensaje(mensajeUsuario, RolMensaje.USER, null);

        // 2. Construir contexto desde la BD según la pregunta
        String contexto = comprobanteService.construirContextoParaChat(mensajeUsuario);

        // 3. Obtener respuesta del LLM
        String respuesta = agentService.responderConsulta(mensajeUsuario, contexto);

        // 4. Persistir respuesta del assistant
        persistirMensaje(respuesta, RolMensaje.ASSISTANT, null);

        return ChatResponse.of(respuesta);
    }

    /**
     * Obtiene el historial de mensajes del chat.
     */
    public List<MensajeChat> obtenerHistorial() {
        return mensajeChatRepository.findTop20ByOrderByCreatedAtDesc()
                .reversed();
    }

    /**
     * Elimina el historial persistido del chat.
     */
    @Transactional
    public void limpiarHistorial() {
        mensajeChatRepository.deleteAllInBatch();
    }

    @Transactional
    protected void persistirMensaje(String contenido, RolMensaje rol, String comprobanteId) {
        MensajeChat mensaje = MensajeChat.builder()
                .contenido(contenido)
                .rol(rol)
                .build();
        mensajeChatRepository.save(mensaje);
    }
}
