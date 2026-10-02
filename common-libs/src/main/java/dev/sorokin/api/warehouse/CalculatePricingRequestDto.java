package dev.sorokin.api.warehouse;

import java.util.UUID;

public record CalculatePricingRequestDto (
        UUID orderId
) { }
