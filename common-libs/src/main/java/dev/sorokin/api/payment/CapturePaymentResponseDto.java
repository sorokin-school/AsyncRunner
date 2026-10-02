package dev.sorokin.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record CapturePaymentResponseDto(
        UUID captureId,
        BigDecimal capturedAmount,
        CaptureStatus status,
        String message
) {}
