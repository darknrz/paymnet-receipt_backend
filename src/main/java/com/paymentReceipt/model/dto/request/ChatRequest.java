package com.paymentReceipt.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        String mensaje
) {}
