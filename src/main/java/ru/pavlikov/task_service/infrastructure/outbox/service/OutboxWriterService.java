package ru.pavlikov.task_service.infrastructure.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pavlikov.task_service.infrastructure.outbox.dto.OutboxCommand;
import ru.pavlikov.task_service.infrastructure.outbox.entity.OutboxEntity;
import ru.pavlikov.task_service.infrastructure.outbox.repository.OutboxRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxWriterService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public void saveEvent(OutboxCommand command) {
        OutboxEntity entity = OutboxEntity.builder()
                .topicName(command.getTopicName())
                .messageKey(command.getMessageKey())
                .eventType(command.getEventType())
                .sourceService(command.getSourceService())
                .payload(command.getPayload())
                .headers(command.getHeaders())
                .processed(false)
                .failed(false)
                .retryCount(0)
                .build();

        outboxRepository.save(entity);
        log.debug("Событие сохранено в outbox: type={}, topic={}",
                command.getEventType(), command.getTopicName());
    }

    @Transactional(readOnly = true)
    public List<OutboxEntity> getUnprocessedEvents(int batchSize) {
        return outboxRepository.findUnprocessedEvents(
                org.springframework.data.domain.PageRequest.of(0, batchSize)
        );
    }

    @Transactional
    public void markAsProcessed(UUID eventId) {
        outboxRepository.markAsProcessed(eventId);
        log.debug("Событие {} помечено как обработанное", eventId);
    }

    @Transactional
    public void incrementRetryCount(UUID eventId, String errorMessage) {
        outboxRepository.incrementRetryCount(eventId, errorMessage);
        log.warn("Событие {}: увеличен счетчик попыток. Ошибка: {}", eventId, errorMessage);
    }

    @Transactional
    public void markAsFailed(UUID eventId, String errorMessage) {
        outboxRepository.markAsFailed(eventId, errorMessage);
        log.error("Событие {} помечено как FAILED: {}", eventId, errorMessage);
    }
}