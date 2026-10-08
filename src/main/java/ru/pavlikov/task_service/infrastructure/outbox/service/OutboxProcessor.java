package ru.pavlikov.task_service.infrastructure.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.pavlikov.task_service.infrastructure.outbox.entity.OutboxEntity;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxWriterService outboxWriterService;
    private final KafkaSenderService kafkaSenderService;

    @Value("${outbox.processor.batch-size:100}")
    private int batchSize;

    @Value("${outbox.processor.send-timeout-sec:10}")
    private int sendTimeoutSec;

    @Value("${outbox.processor.max-retries:5}")
    private int maxRetries;


    @Scheduled(fixedDelayString = "${outbox.processor.delay:5000}")
    public void processOutbox() {
        List<OutboxEntity> events = outboxWriterService.getUnprocessedEvents(batchSize);

        if (events.isEmpty()) {
            return;
        }

        log.info("Начата обработка {} событий из outbox", events.size());

        for (OutboxEntity event : events) {
            processEvent(event);
        }

        log.info("Завершена обработка событий из outbox");
    }

    private void processEvent(OutboxEntity event) {
        try {
            boolean success = kafkaSenderService.send(event, sendTimeoutSec);

            if (success) {
                outboxWriterService.markAsProcessed(event.getId());
            } else {
                handleFailure(event, "Ошибка отправки в Kafka");
            }

        } catch (Exception e) {
            log.error("Неожиданная ошибка при обработке события {}", event.getId(), e);
            handleFailure(event, e.getMessage());
        }
    }

    private void handleFailure(OutboxEntity event, String errorMessage) {
        if (event.getRetryCount() >= maxRetries) {
            outboxWriterService.markAsFailed(event.getId(), errorMessage);
        } else {
            outboxWriterService.incrementRetryCount(event.getId(), errorMessage);
        }
    }
}