package ru.pavlikov.task_service.infrastructure.outbox.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.pavlikov.task_service.infrastructure.outbox.entity.OutboxEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    @Query("SELECT o FROM OutboxEntity o WHERE o.processed = false AND o.failed = false ORDER BY o.createdAt ASC")
    List<OutboxEntity> findUnprocessedEvents(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEntity o SET o.processed = true WHERE o.id = :id")
    void markAsProcessed(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE OutboxEntity o SET o.retryCount = o.retryCount + 1, o.lastError = :error WHERE o.id = :id")
    void incrementRetryCount(@Param("id") UUID id, @Param("error") String error);

    @Modifying
    @Query("UPDATE OutboxEntity o SET o.failed = true, o.lastError = :error WHERE o.id = :id")
    void markAsFailed(@Param("id") UUID id, @Param("error") String error);
}