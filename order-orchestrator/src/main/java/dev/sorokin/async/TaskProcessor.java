package dev.sorokin.async;

import dev.sorokin.api.payment.*;
import dev.sorokin.api.warehouse.CalculatePricingRequestDto;
import dev.sorokin.client.StubHttpClient;
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.OrderJpaRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class TaskProcessor {
    private final StubHttpClient stubHttpClient;
    private final OrderJpaRepository orderRepository;
    private final TransactionTemplate txTemplate;

    public TaskExecutionStatus processTask(AsyncTaskEntity task) {
        UUID orderId = task.getOrderId();

        OrderEntity orderEntity = txTemplate.execute(status -> orderRepository.findById(orderId).orElse(null));

        if (orderEntity == null) {
            log.error("Not found order with id = {}", orderId);
            task.setTaskStatus(TaskStatus.FAILED_NON_RETRYABLE);
            return TaskExecutionStatus.NON_RETRYABLE_ERROR;
        }

        try {
            switch (task.getStep()) {

                case AUTH -> {
                    log.info("Executing AUTH step for taskId={}, orderId={}", task.getId(), orderId);

                    AuthorizePaymentRequestDto request = new AuthorizePaymentRequestDto(
                            orderEntity.getId(),
                            orderEntity.getClientEstimate()
                    );
                    AuthorizePaymentResponseDto authResponse = stubHttpClient.authorize(request);

                    if (authResponse.status() != AuthorizationStatus.AUTHORIZED) {
                        String errorMsg = authResponse.message() != null ? authResponse.message() : "Authorization declined";
                        log.warn("Authorization failed for orderId={}: {}", orderId, errorMsg);

                        saveOrderFailure(orderEntity, PaymentStatus.AUTHORIZATION_FAILED, errorMsg);

                        task.setTaskStatus(TaskStatus.FAILED_NON_RETRYABLE);
                        return TaskExecutionStatus.NON_RETRYABLE_ERROR;
                    }

                    saveOrderAuthorizationSuccess(orderEntity, authResponse.authorizedAmount());
                }

                case REPRICE -> {
                    log.info("Executing REPRICE step for taskId={}, orderId={}", task.getId(), orderId);

                    CalculatePricingRequestDto request = new CalculatePricingRequestDto(orderId);
                    var warehouseResponse = stubHttpClient.calculatePricing(request);

                    if (warehouseResponse.finalAmount().compareTo(orderEntity.getAuthorizedAmount()) > 0) {
                        String errorMsg = String.format("Price has changed from %s to %s",
                                orderEntity.getAuthorizedAmount(), warehouseResponse.finalAmount());
                        log.warn("Price increased for orderId={}: {}", orderId, errorMsg);

                        saveOrderPriceChangeFailed(orderEntity, warehouseResponse.finalAmount(), errorMsg);

                        task.setTaskStatus(TaskStatus.FAILED_NON_RETRYABLE);
                        return TaskExecutionStatus.NON_RETRYABLE_ERROR;
                    }

                    saveOrderPriceSuccess(orderEntity, warehouseResponse.finalAmount());
                }

                case CAPTURE -> {
                    log.info("Executing CAPTURE step for taskId={}, orderId={}", task.getId(), orderId);

                    CapturePaymentRequestDto request = new CapturePaymentRequestDto(
                            orderEntity.getFinalAmount(),
                            orderEntity.getId()
                    );

                    var captureResponse = stubHttpClient.capture(request);

                    if (captureResponse.status() == CaptureStatus.CAPTURED) {
                        saveOrderCaptureSuccess(orderEntity, orderEntity.getFinalAmount());
                        return TaskExecutionStatus.SUCCESS;
                    } else {
                        String errorMsg = "Capture rejected by payment gateway";
                        saveOrderFailure(orderEntity, PaymentStatus.CAPTURE_FAILED, errorMsg);

                        task.setTaskStatus(TaskStatus.FAILED_RETRYABLE);
                        return TaskExecutionStatus.RETRYABLE_ERROR;
                    }
                }
            }

        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("Temporary network/timeout error on step {} for taskId={}: {}", task.getStep(), task.getId(), e.getMessage());
            task.setTaskStatus(TaskStatus.FAILED_RETRYABLE);
            return TaskExecutionStatus.RETRYABLE_ERROR;

        } catch (Exception e) {
            log.error("Unexpected error on step {} for taskId={}", task.getStep(), task.getId(), e);
            task.setTaskStatus(TaskStatus.FAILED_NON_RETRYABLE);
            markOrderSystematicFailure(task, "Unexpected system error: " + e.getMessage());
            return TaskExecutionStatus.NON_RETRYABLE_ERROR;
        }

        return TaskExecutionStatus.SUCCESS;
    }

    public void markOrderSystematicFailure(AsyncTaskEntity task, String failureReason) {
        OrderEntity order = txTemplate.execute(status -> orderRepository.findById(task.getOrderId()).orElse(null));
        if (order != null) {
            PaymentStatus paymentStatus = switch (task.getStep()) {
                case AUTH -> PaymentStatus.AUTHORIZATION_FAILED;
                case REPRICE -> PaymentStatus.PRICE_CHANGE_FAILED;
                case CAPTURE -> PaymentStatus.CAPTURE_FAILED;
            };
            saveOrderFailure(order, paymentStatus, failureReason);
        }
    }

    private void saveOrderFailure(OrderEntity order, PaymentStatus status, String failureReason) {
        var orderToUpdate = order.toBuilder()
                .paymentStatus(status)
                .failureReason(failureReason)
                .build();
        txTemplate.executeWithoutResult(tx -> orderRepository.save(orderToUpdate));
    }

    private void saveOrderAuthorizationSuccess(OrderEntity order, BigDecimal authorizedAmount) {
        var orderToUpdate = order.toBuilder()
                .paymentStatus(PaymentStatus.AUTHORIZED)
                .authorizedAmount(authorizedAmount)
                .build();
        txTemplate.executeWithoutResult(tx -> orderRepository.save(orderToUpdate));
    }

    private void saveOrderPriceChangeFailed(OrderEntity order, BigDecimal finalAmount, String failureReason) {
        var orderToUpdate = order.toBuilder()
                .paymentStatus(PaymentStatus.PRICE_CHANGE_FAILED)
                .finalAmount(finalAmount)
                .failureReason(failureReason)
                .build();
        txTemplate.executeWithoutResult(tx -> orderRepository.save(orderToUpdate));
    }

    private void saveOrderPriceSuccess(OrderEntity order, BigDecimal finalAmount) {
        var orderToUpdate = order.toBuilder()
                .finalAmount(finalAmount)
                .build();
        txTemplate.executeWithoutResult(tx -> orderRepository.save(orderToUpdate));
    }

    private void saveOrderCaptureSuccess(OrderEntity order, BigDecimal amount) {
        var orderToUpdate = order.toBuilder()
                .paymentStatus(PaymentStatus.SUCCEED_PAID)
                .capturedAmount(amount)
                .build();
        txTemplate.executeWithoutResult(tx -> orderRepository.save(orderToUpdate));
    }
}