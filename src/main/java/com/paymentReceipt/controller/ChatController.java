package com.paymentReceipt.controller;

import com.paymentReceipt.model.dto.request.ChatRequest;
import com.paymentReceipt.model.dto.response.ApiResponse;
import com.paymentReceipt.model.dto.response.ChatResponse;
import com.paymentReceipt.model.dto.response.ComprobanteResponse;
import com.paymentReceipt.model.entity.MensajeChat;
import com.paymentReceipt.service.ChatService;
import com.paymentReceipt.service.ComprobanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ComprobanteService comprobanteService;
    private final ChatClient chatClient;

    /**
     * Enviar mensaje de texto al chat.
     * POST /api/chat/mensaje
     */
    @PostMapping("/mensaje")
    public ResponseEntity<ApiResponse<ChatResponse>> enviarMensaje(
            @Valid @RequestBody ChatRequest request) {

        ChatResponse respuesta = chatService.procesarMensaje(request.mensaje());
        return ResponseEntity.ok(ApiResponse.ok(respuesta));
    }

    /**
     * Enviar mensaje con streaming SSE (bonus).
     * GET /api/chat/stream?mensaje=...
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMensaje(@RequestParam String mensaje) {
        log.info("Iniciando stream para: {}", mensaje);

        String contexto = comprobanteService.construirContextoParaChat(mensaje);
        String promptCompleto = contexto.isBlank()
                ? mensaje
                : "Contexto:\n" + contexto + "\n\nPregunta: " + mensaje;

        return chatClient.prompt()
                .system("Eres un asistente especializado en comprobantes de pago peruanos. Responde en español de forma clara y concisa.")
                .user(promptCompleto)
                .stream()
                .content();
    }

    /**
     * Subir archivo de comprobante para análisis.
     * POST /api/chat/analizar
     */
    @PostMapping(value = "/analizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ComprobanteResponse>> analizarArchivo(
            @RequestParam("archivo") MultipartFile archivo) throws IOException {

        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El archivo está vacío"));
        }

        log.info("Analizando archivo: {}", archivo.getOriginalFilename());
        ComprobanteResponse resultado = comprobanteService.analizarYPersistir(archivo);
        return ResponseEntity.ok(ApiResponse.ok("Comprobante analizado exitosamente", resultado));
    }

    /**
     * Analizar texto plano de comprobante.
     * POST /api/chat/analizar-texto
     */
    @PostMapping("/analizar-texto")
    public ResponseEntity<ApiResponse<ComprobanteResponse>> analizarTexto(
            @RequestBody String texto) {

        if (texto == null || texto.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El texto no puede estar vacío"));
        }

        ComprobanteResponse resultado = comprobanteService.analizarTexto(texto);
        return ResponseEntity.ok(ApiResponse.ok("Comprobante analizado exitosamente", resultado));
    }

    /**
     * Obtener historial del chat.
     * GET /api/chat/historial
     */
    @GetMapping("/historial")
    public ResponseEntity<ApiResponse<List<MensajeChat>>> obtenerHistorial() {
        List<MensajeChat> historial = chatService.obtenerHistorial();
        return ResponseEntity.ok(ApiResponse.ok(historial));
    }

    /**
     * Limpiar historial del chat.
     * DELETE /api/chat/historial
     */
    @DeleteMapping("/historial")
    public ResponseEntity<ApiResponse<Void>> limpiarHistorial() {
        chatService.limpiarHistorial();
        return ResponseEntity.ok(ApiResponse.ok("Historial del chat eliminado", null));
    }
}
