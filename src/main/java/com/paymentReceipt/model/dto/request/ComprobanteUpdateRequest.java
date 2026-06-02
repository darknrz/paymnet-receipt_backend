package com.paymentReceipt.model.dto.request;

import com.paymentReceipt.model.enums.EstadoComprobante;
import com.paymentReceipt.model.enums.TipoComprobante;

import java.math.BigDecimal;
import java.time.LocalDate;

// Todos los campos son opcionales: solo se actualizan los que vienen non-null
public record ComprobanteUpdateRequest(
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
        String observaciones
) {}
