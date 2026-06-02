package com.paymentReceipt.model.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record InsightResponse(
        long totalComprobantes,
        long comprobantesValidos,
        long comprobantesInvalidos,
        long comprobantesPendientes,
        BigDecimal totalFacturado,
        BigDecimal promedioTotal,
        Map<String, Long> porTipo,      // { "FACTURA": 5, "BOLETA": 3 }
        Map<String, Long> porMoneda     // { "PEN": 7, "USD": 1 }
) {}
