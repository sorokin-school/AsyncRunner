package dev.sorokin.api.warehouse;

import java.math.BigDecimal;
import java.util.UUID;

public record CalculatePricingResponseDto(
        UUID orderId,
        BigDecimal finalAmount,
        String reason
) { }
