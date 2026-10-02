package dev.sorokin.async;

import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class TaskDispatcher {
    private final ExecutorService executorService = Executors.newFixedThreadPool(10); //вынести
    private final TaskEntityRepository taskRepository;
    private final TransactionTemplate txTemplate;
    private final TaskProcessor taskProcessor;

    public void dispatch(AsyncTaskEntity task) {
        CompletableFuture
                .supplyAsync(() -> taskProcessor.processTask(task), executorService)
                .thenAccept(result -> handleTaskExecuted(task, result))
                .exceptionally(ex -> handleTaskException(task, ex));
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TaskDispatcher executor, waiting for running tasks to finish");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private Void handleTaskException(AsyncTaskEntity task, Throwable ex) {
        log.error("Task failed with unexpected exception, taskId = {}", task.getId(), ex);
        scheduleTaskRetry(task);
        return null;
    }

    private void handleTaskExecuted(AsyncTaskEntity task, TaskExecutionStatus taskExecutionStatus) {
        log.info("Task executed: taskId={}, status={}", task.getId(), taskExecutionStatus);
        switch (taskExecutionStatus) {
            case SUCCESS -> handleTaskSucceeded(task);
            case RETRYABLE_ERROR -> scheduleTaskRetry(task);
            case NON_RETRYABLE_ERROR -> handleTaskFailed(task);
        }
    }

    private void handleTaskFailed(AsyncTaskEntity task) {
        txTemplate.executeWithoutResult(status -> {
            taskRepository.save(task.toBuilder()
                    .taskStatus(TaskStatus.FAILED_NON_RETRYABLE)
                    .nextAttemptAt(null)
                    .build());
        });
    }

    private void scheduleTaskRetry(AsyncTaskEntity task) {
        log.info("Scheduling retry for taskId={}", task.getId());

        int currentAttempts = task.getAttempts() == null ? 1 : task.getAttempts();

        txTemplate.executeWithoutResult(status -> {
            if (currentAttempts >= 10) {
                log.error("Maximum number of retries reached for taskId: {}", task.getId());
                taskRepository.save(task.toBuilder()
                        .attempts(currentAttempts)
                        .taskStatus(TaskStatus.FAILED_NON_RETRYABLE)
                        .nextAttemptAt(null)
                        .build());
                taskProcessor.markOrderSystematicFailure(task, "Maximum retries reached (10)");
                return;
            }

            var nextAttemptAt = OffsetDateTime.now().plus(Duration.ofSeconds(5));

            taskRepository.save(task.toBuilder()
                    .attempts(currentAttempts)
                    .taskStatus(TaskStatus.FAILED_RETRYABLE)
                    .nextAttemptAt(nextAttemptAt)
                    .build());
        });
    }

    private void handleTaskSucceeded(AsyncTaskEntity task) {
        txTemplate.executeWithoutResult(status -> {
            switch (task.getStep()) {
                case AUTH -> taskRepository.save(task.toBuilder()
                        .taskStatus(TaskStatus.NEW)
                        .step(TaskStep.REPRICE)
                        .attempts(0)  //сбрасываем счётчик - у REPRICE свои 10 попыток
                        .nextAttemptAt(OffsetDateTime.now())
                        .build());
                case REPRICE -> taskRepository.save(task.toBuilder()
                        .taskStatus(TaskStatus.NEW)
                        .step(TaskStep.CAPTURE)
                        .attempts(0)  //сбрасываем счётчик - у CAPTURE свои 10 попыток
                        .nextAttemptAt(OffsetDateTime.now())
                        .build());
                case CAPTURE -> taskRepository.save(task.toBuilder()
                        .taskStatus(TaskStatus.SUCCEEDED)
                        .nextAttemptAt(null)
                        .build());
            }
        });
    }
}