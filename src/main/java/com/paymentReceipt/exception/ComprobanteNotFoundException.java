package com.paymentReceipt.exception;

public class ComprobanteNotFoundException extends RuntimeException {
    public ComprobanteNotFoundException(String id) {
        super("Comprobante no encontrado con id: " + id);
    }
}
