package com.paymentReceipt.agent;

import com.paymentReceipt.exception.AgentException;
import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComprobanteAgentService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    // =========================================================
    // CONTRATO: JSON que devuelve el Agent
    // =========================================================
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AgentResult(
            TipoComprobante tipo,
            String serie,
            String numero,
            LocalDate fechaEmision,
            String emisorRuc,
            String emisorRazonSocial,
            String emisorDireccion,
            String receptorRucDni,
            String receptorNombre,
            BigDecimal subtotal,
            BigDecimal igv,
            BigDecimal total,
            String moneda,
            EstadoComprobante estado,
            String observaciones,
            List<ItemResult> items
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ItemResult(
                String descripcion,
                BigDecimal cantidad,
                BigDecimal precioUnitario,
                BigDecimal subtotal
        ) {}
    }

    private static final String SYSTEM_PROMPT = """
            Eres un agente especializado en análisis de comprobantes de pago peruanos (facturas, boletas, tickets).
            
            Tu tarea es extraer información estructurada de un comprobante y devolverla ÚNICAMENTE en formato JSON.
            NO incluyas ningún texto adicional, solo el JSON puro.
            
            El JSON debe seguir exactamente esta estructura:
            {
              "tipo": "FACTURA|BOLETA|TICKET|NOTA_CREDITO|NOTA_DEBITO|OTRO",
              "serie": "string o null",
              "numero": "string o null",
              "fechaEmision": "YYYY-MM-DD o null",
              "emisorRuc": "string o null",
              "emisorRazonSocial": "string o null",
              "emisorDireccion": "string o null",
              "receptorRucDni": "string o null",
              "receptorNombre": "string o null",
              "subtotal": número o null,
              "igv": número o null,
              "total": número o null,
              "moneda": "PEN|USD",
              "estado": "VALIDO|INVALIDO|PENDIENTE",
              "observaciones": "descripción de problemas encontrados o null",
              "items": [
                {
                  "descripcion": "string",
                  "cantidad": número,
                  "precioUnitario": número,
                  "subtotal": número
                }
              ]
            }
            
            Reglas de validación:
            - El estado es VALIDO si tiene RUC emisor válido (11 dígitos), fecha, serie, número y total coherente
            - El estado es INVALIDO si faltan datos críticos o los montos no cuadran
            - El estado es PENDIENTE si hay datos pero no puedes confirmar su validez
            - El IGV en Perú es 18% del subtotal (tolerancia de ±0.10 soles)
            - Si el total no coincide con subtotal + IGV, marca como INVALIDO y anota en observaciones
            """;

    /**
     * Analiza el texto extraído de un comprobante y retorna los datos estructurados.
     */
    public AgentResult analizar(String textoComprobante) {
        log.info("Iniciando análisis de comprobante...");
        return analizarConPrompt(
                "Analiza el siguiente comprobante de pago y extrae la información:\n\n" + textoComprobante,
                user -> {
                }
        );
    }

    /**
     * Analiza una imagen de comprobante usando visión multimodal.
     */
    public AgentResult analizarImagen(MultipartFile archivo) {
        log.info("Iniciando análisis de imagen de comprobante...");

        try {
            byte[] bytes = comprimirImagen(archivo.getBytes());
            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return archivo.getOriginalFilename();
                }
            };

            return analizarConPrompt(
                    "Analiza la imagen del comprobante de pago y extrae la información estructurada.",
                    user -> user.media(MimeTypeUtils.IMAGE_JPEG, resource)
            );
        } catch (IOException e) {
            throw new AgentException("No se pudo leer la imagen del comprobante: " + e.getMessage(), e);
        }
    }

    private byte[] comprimirImagen(byte[] original) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(original));
        if (img == null) return original;
        return comprimirImagen(img);
    }

    private byte[] comprimirImagen(BufferedImage img) throws IOException {
        int maxDim = 1024;
        int w = img.getWidth();
        int h = img.getHeight();

        BufferedImage destino = img;
        if (w > maxDim || h > maxDim) {
            double scale = Math.min((double) maxDim / w, (double) maxDim / h);
            int nw = (int) (w * scale);
            int nh = (int) (h * scale);
            destino = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = destino.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            log.info("Imagen redimensionada de {}x{} a {}x{}", w, h, nw, nh);
        } else if (destino.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.drawImage(destino, 0, 0, null);
            g.dispose();
            destino = rgb;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(destino, "jpg", baos);
        return baos.toByteArray();
    }

    /**
     * Renderiza la primera página del PDF como imagen y la analiza con visión multimodal.
     * Usar cuando PDFBox no extrae texto (PDF escaneado o basado en imagen).
     */
    public AgentResult analizarPdfComoImagen(MultipartFile archivo) {
        log.info("Convirtiendo PDF escaneado a imagen para análisis visual...");
        try {
            byte[] imagenBytes = renderizarPdfAImagen(archivo.getBytes());
            ByteArrayResource resource = new ByteArrayResource(imagenBytes) {
                @Override
                public String getFilename() {
                    return "pagina.jpg";
                }
            };
            return analizarConPrompt(
                    "Analiza la imagen del comprobante de pago y extrae la información estructurada.",
                    user -> user.media(MimeTypeUtils.IMAGE_JPEG, resource)
            );
        } catch (IOException e) {
            throw new AgentException("No se pudo convertir el PDF a imagen: " + e.getMessage(), e);
        }
    }

    private byte[] renderizarPdfAImagen(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage imagen = renderer.renderImageWithDPI(0, 150);
            byte[] comprimida = comprimirImagen(imagen);
            log.info("PDF renderizado a imagen ({} bytes)", comprimida.length);
            return comprimida;
        }
    }

    /**
     * Responde consultas sobre comprobantes desde el chat.
     */
    public String responderConsulta(String pregunta, String contexto) {
        log.info("Respondiendo consulta del chat...");

        String systemChat = """
                Eres un asistente especializado en comprobantes de pago.
                Tienes acceso a la base de datos de comprobantes del usuario.
                Responde de forma clara, concisa y en español.
                Si el contexto contiene datos relevantes, úsalos en tu respuesta.
                """;

        String promptCompleto = contexto.isBlank()
                ? pregunta
                : "Contexto de la base de datos:\n" + contexto + "\n\nPregunta del usuario: " + pregunta;

        return chatClient.prompt()
                .system(systemChat)
                .user(promptCompleto)
                .call()
                .content();
    }

    private String limpiarJson(String raw) {
        if (raw == null) return "{}";
        return raw.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();
    }

    private AgentResult analizarConPrompt(String prompt, Consumer<org.springframework.ai.chat.client.ChatClient.PromptUserSpec> customizadorUsuario) {
        try {
            String respuestaJson = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user -> {
                        user.text(prompt);
                        customizadorUsuario.accept(user);
                    })
                    .call()
                    .content();

            log.debug("Respuesta del agent: {}", respuestaJson);

            String jsonLimpio = limpiarJson(respuestaJson);
            return objectMapper.readValue(jsonLimpio, AgentResult.class);
        } catch (Exception e) {
            log.error("Error al analizar comprobante", e);
            throw new AgentException("No se pudo analizar el comprobante: " + e.getMessage(), e);
        }
    }

    private MimeType resolverMimeType(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return MimeTypeUtils.parseMimeType(contentType);
        }

        return MediaTypeFactory.getMediaType(archivo.getOriginalFilename())
                .map(mediaType -> MimeTypeUtils.parseMimeType(mediaType.toString()))
                .orElse(MimeTypeUtils.parseMimeType(MediaType.IMAGE_JPEG_VALUE));
    }
}
