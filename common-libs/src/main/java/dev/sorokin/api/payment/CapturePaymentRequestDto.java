package dev.sorokin.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record CapturePaymentRequestDto(
        BigDecimal capturedAmount,
        UUID customerId
) {}
