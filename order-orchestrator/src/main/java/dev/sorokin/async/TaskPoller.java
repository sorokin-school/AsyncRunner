package dev.sorokin.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class TaskPoller {
    private final TaskEntityRepository taskRepository;
    private final TransactionTemplate txTemplate;
    private final TaskDispatcher taskDispatcher;

    @Scheduled(fixedDelayString = "5s") //потом вынести
    public void poll() {
        log.info("Starting polling tasks");
        List<AsyncTaskEntity> tasksBatch = pickTasksForProcessing();

        var taskIds = tasksBatch
                .stream()
                .map(AsyncTaskEntity::getId)
                .toList();

        if (tasksBatch.isEmpty()) {
            return;
        }
        log.info("Successfully picked and marked tasks: count = {}, ids = {}", taskIds.size(), taskIds);

        for (AsyncTaskEntity task : tasksBatch) {
            taskDispatcher.dispatch(task);
        }
    }

    private List<AsyncTaskEntity> pickTasksForProcessing() {
        OffsetDateTime now = OffsetDateTime.now();
        log.info("Attempting to peek tasks with now = {}", now);
        return txTemplate.execute(status -> {
            List<AsyncTaskEntity> tasks = taskRepository.peekBatchForProcessing(
                    TaskStatus.NEW.name(),
                    TaskStatus.FAILED_RETRYABLE.name(),
                    TaskStatus.IN_PROGRESS.name(),
                    now,  //единый момент времени для лога и запроса
                    5 //потом вынести
            );
            log.info("Peeked tasks count: {}", tasks.size());
            var nextProcessedTime = OffsetDateTime.now().plus(Duration.ofSeconds(60)); //потом вынести
            for(AsyncTaskEntity task : tasks){
                //увеличиваем счётчик только для реальных новых попыток
                //если задача зависла в IN_PROGRESS и подбирается повторно — это recovery, не новая попытка
                if (task.getTaskStatus() != TaskStatus.IN_PROGRESS) {
                    var attempts = task.getAttempts() == null
                            ? 1 : task.getAttempts() + 1;
                    task.setAttempts(attempts);
                }
                task.setTaskStatus(TaskStatus.IN_PROGRESS);
                task.setNextAttemptAt(nextProcessedTime);
            }
            taskRepository.saveAll(tasks);
            return tasks;
        });
    }
}