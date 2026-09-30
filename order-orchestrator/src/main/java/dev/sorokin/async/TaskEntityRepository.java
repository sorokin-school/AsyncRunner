package dev.sorokin.async;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface TaskEntityRepository extends JpaRepository<AsyncTaskEntity, Long> {

    @Query(value = """ 
                        select * from tasks 
                        where task_status = :newStatus 
                        or (task_status = :retryStatus and next_attempt_at <= :now)
                        or (task_status = :processingStatus and next_attempt_at <= :now)
                        order by id 
                        limit :batchSize
                        for update skip locked
                                                """, nativeQuery = true)

    List<AsyncTaskEntity> peekBatchForProcessing(
            @Param("newStatus") String newStatus,
            @Param("retryStatus") String retryStatus,
            @Param("processingStatus") String processingStatus,
            @Param("now") OffsetDateTime now,
            @Param("batchSize") int batchSize

    );


}
