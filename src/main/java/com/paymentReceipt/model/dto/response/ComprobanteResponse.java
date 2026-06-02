package com.paymentReceipt.model.dto.response;

import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ComprobanteResponse(
        String id,
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
        String archivoNombre,
        String observaciones,
        List<ItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ItemResponse(
            String id,
            String descripcion,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}
}
