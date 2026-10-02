package dev.sorokin.domain;

import dev.sorokin.api.OrderCreateRequestDto;
import dev.sorokin.api.payment.PaymentStatus;
import dev.sorokin.async.AsyncTaskEntity;
import dev.sorokin.async.TaskEntityRepository;
import dev.sorokin.async.TaskStatus;
import dev.sorokin.async.TaskStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderJpaRepository orderRepository;
    private final TaskEntityRepository taskRepository;

    @Transactional //нельзя разрывать создание таски и создание заказа
    public OrderEntity createOrder(
            OrderCreateRequestDto requestDto
    ) {
        log.info("Creating order with address {}, description {}",
                requestDto.address(), requestDto.description());
        var entity = OrderEntity.builder()
                .address(requestDto.address())
                .description(requestDto.description())
                .clientEstimate(requestDto.clientEstimate())
                .paymentStatus(PaymentStatus.NEW)
                .build();
        var createdOrder = orderRepository.save(entity);
        log.info("Created order id = {}", createdOrder.getId());

        var task = AsyncTaskEntity.builder()
                .orderId(createdOrder.getId())
                .taskStatus(TaskStatus.NEW)
                .step(TaskStep.AUTH)
                .build();

        var createdTask = taskRepository.save(task);
        log.info("Created task for order creation: taskId = {}, orderId = {}",
                createdTask.getId(), createdOrder.getId());

        return createdOrder;
    }

    @Transactional(readOnly = true)
    public Optional<OrderEntity> findOrder(UUID id) {
        return orderRepository.findById(id);
    }
}
