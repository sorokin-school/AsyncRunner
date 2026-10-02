package dev.sorokin.api;

import dev.sorokin.api.payment.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderDto(
        UUID id,
        String address,
        String description,
        //оценочная стоимость, отправленная фронтендом
        BigDecimal clientEstimate,
        //фактически авторизованная сумма
        BigDecimal authorizedAmount,
        //фактически списанная сумма
        BigDecimal capturedAmount,
        //финальная сумма после пересчёта склада
        BigDecimal finalAmount,
        PaymentStatus paymentStatus,
        String failureReason
) { }
