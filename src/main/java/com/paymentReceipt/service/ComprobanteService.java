package com.paymentReceipt.service;

import com.paymentReceipt.agent.ArchivoExtractionService;
import com.paymentReceipt.agent.ComprobanteAgentService;
import com.paymentReceipt.agent.ComprobanteAgentService.AgentResult;
import com.paymentReceipt.exception.ComprobanteNotFoundException;
import com.paymentReceipt.model.dto.request.ComprobanteUpdateRequest;
import com.paymentReceipt.model.dto.response.ComprobanteResponse;
import com.paymentReceipt.model.dto.response.InsightResponse;
import com.paymentReceipt.model.entity.Comprobante;
import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;
import com.paymentReceipt.repository.ComprobanteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ComprobanteService {

    private final ComprobanteRepository repository;
    private final ComprobanteAgentService agentService;
    private final ArchivoExtractionService extractionService;
    private final ComprobanteMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * Analiza un archivo de comprobante usando el Agent y lo persiste.
     */
    @Transactional
    public ComprobanteResponse analizarYPersistir(MultipartFile archivo) throws IOException {
        log.info("Procesando comprobante: {}", archivo.getOriginalFilename());

        AgentResult resultado;
        if (esImagen(archivo)) {
            resultado = agentService.analizarImagen(archivo);
        } else {
            String texto = extractionService.extraerTexto(archivo);
            if (texto.isBlank() || texto.strip().length() < 30) {
                log.warn("Texto extraído insuficiente ({}), renderizando PDF como imagen...", texto.strip().length());
                resultado = agentService.analizarPdfComoImagen(archivo);
            } else {
                if (texto.length() > 4000) {
                    log.warn("Texto demasiado largo ({}), truncando a 4000 chars", texto.length());
                    texto = texto.substring(0, 4000);
                }
                resultado = agentService.analizar(texto);
            }
        }

        String rawJson = objectMapper.writeValueAsString(resultado);

        Comprobante comprobante = mapper.fromAgentResult(resultado, archivo.getOriginalFilename(), rawJson);
        Comprobante guardado = repository.save(comprobante);

        log.info("Comprobante persistido con id: {}", guardado.getId());
        return mapper.toResponse(guardado);
    }

    /**
     * Analiza texto plano de un comprobante (copiado manualmente).
     */
    @Transactional
    public ComprobanteResponse analizarTexto(String texto) {
        log.info("Analizando texto de comprobante...");

        AgentResult resultado = agentService.analizar(texto);

        try {
            String rawJson = objectMapper.writeValueAsString(resultado);
            Comprobante comprobante = mapper.fromAgentResult(resultado, null, rawJson);
            Comprobante guardado = repository.save(comprobante);
            return mapper.toResponse(guardado);
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar comprobante", e);
        }
    }

    /**
     * Obtener todos los comprobantes.
     */
    public List<ComprobanteResponse> listarTodos() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Obtener comprobante por ID.
     */
    public ComprobanteResponse obtenerPorId(String id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ComprobanteNotFoundException(id));
    }

    /**
     * Buscar comprobantes con texto libre.
     */
    public List<ComprobanteResponse> buscar(String texto) {
        return repository.buscarTextoLibre(texto).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Buscar por tipo.
     */
    public List<ComprobanteResponse> buscarPorTipo(TipoComprobante tipo) {
        return repository.findByTipo(tipo).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Buscar por estado.
     */
    public List<ComprobanteResponse> buscarPorEstado(EstadoComprobante estado) {
        return repository.findByEstado(estado).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Actualización manual de campos (bonus: corrección desde UI).
     */
    @Transactional
    public ComprobanteResponse actualizar(String id, ComprobanteUpdateRequest request) {
        Comprobante comprobante = repository.findById(id)
                .orElseThrow(() -> new ComprobanteNotFoundException(id));

        if (request.tipo() != null) comprobante.setTipo(request.tipo());
        if (request.serie() != null) comprobante.setSerie(request.serie());
        if (request.numero() != null) comprobante.setNumero(request.numero());
        if (request.fechaEmision() != null) comprobante.setFechaEmision(request.fechaEmision());
        if (request.emisorRuc() != null) comprobante.setEmisorRuc(request.emisorRuc());
        if (request.emisorRazonSocial() != null) comprobante.setEmisorRazonSocial(request.emisorRazonSocial());
        if (request.emisorDireccion() != null) comprobante.setEmisorDireccion(request.emisorDireccion());
        if (request.receptorRucDni() != null) comprobante.setReceptorRucDni(request.receptorRucDni());
        if (request.receptorNombre() != null) comprobante.setReceptorNombre(request.receptorNombre());
        if (request.subtotal() != null) comprobante.setSubtotal(request.subtotal());
        if (request.igv() != null) comprobante.setIgv(request.igv());
        if (request.total() != null) comprobante.setTotal(request.total());
        if (request.moneda() != null) comprobante.setMoneda(request.moneda());
        if (request.estado() != null) comprobante.setEstado(request.estado());
        if (request.observaciones() != null) comprobante.setObservaciones(request.observaciones());

        return mapper.toResponse(repository.save(comprobante));
    }

    /**
     * Eliminar comprobante.
     */
    @Transactional
    public void eliminar(String id) {
        if (!repository.existsById(id)) {
            throw new ComprobanteNotFoundException(id);
        }
        repository.deleteById(id);
    }

    /**
     * Generar insights estadísticos.
     */
    public InsightResponse obtenerInsights() {
        long total = repository.count();
        long validos = repository.findByEstado(EstadoComprobante.VALIDO).size();
        long invalidos = repository.findByEstado(EstadoComprobante.INVALIDO).size();
        long pendientes = repository.findByEstado(EstadoComprobante.PENDIENTE).size();
        BigDecimal totalFacturado = repository.sumTotalByEstado(EstadoComprobante.VALIDO);
        BigDecimal promedio = repository.avgTotal();

        Map<String, Long> porTipo = new HashMap<>();
        repository.countByTipo().forEach(row -> porTipo.put(row[0].toString(), (Long) row[1]));

        Map<String, Long> porMoneda = new HashMap<>();
        repository.countByMoneda().forEach(row -> porMoneda.put(
                row[0] != null ? row[0].toString() : "SIN_MONEDA",
                (Long) row[1]
        ));

        return new InsightResponse(total, validos, invalidos, pendientes, totalFacturado, promedio, porTipo, porMoneda);
    }

    /**
     * Construye un contexto de texto para que el LLM pueda responder consultas del chat.
     */
    public String construirContextoParaChat(String pregunta) {
        // Buscar comprobantes relacionados con la pregunta
        List<Comprobante> relacionados = repository.buscarTextoLibre(pregunta);

        if (relacionados.isEmpty()) {
            InsightResponse insights = obtenerInsights();
            return String.format("""
                    Resumen de la base de datos:
                    - Total comprobantes: %d
                    - Válidos: %d | Inválidos: %d | Pendientes: %d
                    - Total facturado (válidos): S/ %.2f
                    - Distribución por tipo: %s
                    """,
                    insights.totalComprobantes(),
                    insights.comprobantesValidos(),
                    insights.comprobantesInvalidos(),
                    insights.comprobantesPendientes(),
                    insights.totalFacturado(),
                    insights.porTipo()
            );
        }

        StringBuilder sb = new StringBuilder("Comprobantes encontrados:\n");
        relacionados.stream().limit(5).forEach(c -> {
            sb.append(String.format("- [%s] %s %s | Emisor: %s | Total: %s %s | Estado: %s\n",
                    c.getTipo(), c.getSerie(), c.getNumero(),
                    c.getEmisorRazonSocial(),
                    c.getMoneda(), c.getTotal(),
                    c.getEstado()
            ));
        });

        return sb.toString();
    }

    private boolean esImagen(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }

        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo == null) {
            return false;
        }

        String normalizado = nombreArchivo.toLowerCase();
        return normalizado.endsWith(".png")
                || normalizado.endsWith(".jpg")
                || normalizado.endsWith(".jpeg")
                || normalizado.endsWith(".webp")
                || normalizado.endsWith(".gif")
                || normalizado.endsWith(".bmp")
                || normalizado.endsWith(".tif")
                || normalizado.endsWith(".tiff");
    }
}
