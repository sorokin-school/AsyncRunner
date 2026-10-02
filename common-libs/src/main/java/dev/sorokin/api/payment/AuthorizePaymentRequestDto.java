package dev.sorokin.api.payment;

import java.math.BigDecimal;

public record AuthorizePaymentRequestDto(
        java.util.UUID customerId,
        BigDecimal amount
) { }
