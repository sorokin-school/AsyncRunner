package dev.sorokin.api.payment;

public enum PaymentStatus {
    NEW,
    AUTHORIZATION_FAILED, //авторизация карты не прошла (банк/шлюз отказал, денег не хватило)
    AUTHORIZED,
    PRICE_CHANGE_FAILED, //финальная сумма после пересчёта склада оказалась больше, чем
    //авторизованная, мы решили не списывать;
    CAPTURE_FAILED, //не удалось списать
    SUCCEED_PAID
}
