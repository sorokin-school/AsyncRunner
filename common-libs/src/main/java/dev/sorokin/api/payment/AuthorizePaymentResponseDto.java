package dev.sorokin.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record AuthorizePaymentResponseDto(
        UUID authorizationId,
        BigDecimal authorizedAmount,
        AuthorizationStatus status,
        String message
) { }
