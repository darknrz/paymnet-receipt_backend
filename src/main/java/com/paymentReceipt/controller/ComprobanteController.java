package com.paymentReceipt.controller;

import com.paymentReceipt.model.dto.request.ComprobanteUpdateRequest;
import com.paymentReceipt.model.dto.response.ApiResponse;
import com.paymentReceipt.model.dto.response.ComprobanteResponse;
import com.paymentReceipt.model.dto.response.InsightResponse;
import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;
import com.paymentReceipt.service.ComprobanteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService service;

    /**
     * Listar todos los comprobantes.
     * GET /api/comprobantes
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ComprobanteResponse>>> listarTodos() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarTodos()));
    }

    /**
     * Obtener comprobante por ID.
     * GET /api/comprobantes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComprobanteResponse>> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(service.obtenerPorId(id)));
    }

    /**
     * Buscar comprobantes con texto libre.
     * GET /api/comprobantes/buscar?q=...
     */
    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<List<ComprobanteResponse>>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscar(q)));
    }

    /**
     * Filtrar por tipo.
     * GET /api/comprobantes/tipo/{tipo}
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<ApiResponse<List<ComprobanteResponse>>> porTipo(@PathVariable TipoComprobante tipo) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorTipo(tipo)));
    }

    /**
     * Filtrar por estado.
     * GET /api/comprobantes/estado/{estado}
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<ApiResponse<List<ComprobanteResponse>>> porEstado(@PathVariable EstadoComprobante estado) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorEstado(estado)));
    }

    /**
     * Actualizar campos manualmente (bonus: corrección desde UI).
     * PATCH /api/comprobantes/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ComprobanteResponse>> actualizar(
            @PathVariable String id,
            @RequestBody ComprobanteUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Comprobante actualizado", service.actualizar(id, request)));
    }

    /**
     * Eliminar comprobante.
     * DELETE /api/comprobantes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Comprobante eliminado", null));
    }

    /**
     * Obtener insights estadísticos.
     * GET /api/comprobantes/insights
     */
    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<InsightResponse>> insights() {
        return ResponseEntity.ok(ApiResponse.ok(service.obtenerInsights()));
    }
}
