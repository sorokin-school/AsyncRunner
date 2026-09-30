package dev.sorokin.async;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@AllArgsConstructor
public class TaskDispatcher {
    private final ExecutorService executorService = Executors.newFixedThreadPool(10); //потом вынести
    private final TaskEntityRepository taskRepository;
    private final TransactionTemplate txTemplate;

    public void dispatch(AsyncTaskEntity task) {
        CompletableFuture
                .supplyAsync(()-> processTask(task), executorService)
                .thenAccept(result -> handleTaskExecuted(task, result))
                .exceptionally(ex -> handleTaskExceptionInTaskHappened(task, ex));

    }

    private Void handleTaskExceptionInTaskHappened(
            AsyncTaskEntity task,
            Throwable ex
    ) {
        return null;
    }

    private void handleTaskExecuted(
            AsyncTaskEntity task,
            TaskExecutionStatus taskExecutionStatus
    ) {
        log.info("Task executed: taskId={}, status={}", task.getId(), taskExecutionStatus);
        switch(taskExecutionStatus){
            case SUCCESS -> handleTaskSucceded(task);
            case RETRYABLE_ERROR -> sheduleTaskRetry(task);
            case NON_RETRYABLE_ERROR -> handleTaskFailed(task);
        }


    }

    private TaskExecutionStatus processTask(AsyncTaskEntity task){
        return TaskExecutionStatus.SUCCESS;
    }
}
