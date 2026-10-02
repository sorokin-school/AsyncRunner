package dev.sorokin.api;

import java.math.BigDecimal;

public record OrderCreateRequestDto(
        String address,
        String description,
        //оценочная стоимость, отправленная фронтендом
        BigDecimal clientEstimate
) { }
