package com.paymentReceipt.model.dto.response;

import java.time.LocalDateTime;

public record ChatResponse(
        String mensaje,
        String comprobanteId,   // null si no se analizó comprobante
        LocalDateTime timestamp
) {
    public static ChatResponse of(String mensaje) {
        return new ChatResponse(mensaje, null, LocalDateTime.now());
    }

    public static ChatResponse of(String mensaje, String comprobanteId) {
        return new ChatResponse(mensaje, comprobanteId, LocalDateTime.now());
    }
}
