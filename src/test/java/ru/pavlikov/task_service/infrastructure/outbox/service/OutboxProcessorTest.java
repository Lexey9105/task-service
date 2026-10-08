package ru.pavlikov.task_service.infrastructure.outbox.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.pavlikov.task_service.infrastructure.outbox.entity.OutboxEntity;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxWriterService outboxWriterService;

    @Mock
    private KafkaSenderService kafkaSenderService;

    @InjectMocks
    private OutboxProcessor processor;

    private OutboxEntity event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "batchSize", 100);
        ReflectionTestUtils.setField(processor, "sendTimeoutSec", 2);
        ReflectionTestUtils.setField(processor, "maxRetries", 3);
        event = OutboxEntity.builder()
                .id(UUID.randomUUID())
                .retryCount(0)
                .topicName("task-events")
                .payload("{}")
                .build();
    }

    @Test
    void shouldDoNothingWhenNoEvents() {
        when(outboxWriterService.getUnprocessedEvents(100)).thenReturn(List.of());

        processor.processOutbox();

        verifyNoInteractions(kafkaSenderService);
    }

    @Test
    void shouldMarkEventProcessedAfterSuccessfulSend() {
        when(outboxWriterService.getUnprocessedEvents(100)).thenReturn(List.of(event));
        when(kafkaSenderService.send(event, 2)).thenReturn(true);

        processor.processOutbox();

        verify(outboxWriterService).markAsProcessed(event.getId());
        verify(outboxWriterService, never()).incrementRetryCount(any(), anyString());
        verify(outboxWriterService, never()).markAsFailed(any(), anyString());
    }

    @Test
    void shouldIncrementRetryCountAfterFailedSendBeforeLimit() {
        when(outboxWriterService.getUnprocessedEvents(100)).thenReturn(List.of(event));
        when(kafkaSenderService.send(event, 2)).thenReturn(false);

        processor.processOutbox();

        verify(outboxWriterService).incrementRetryCount(event.getId(), "Ошибка отправки в Kafka");
        verify(outboxWriterService, never()).markAsProcessed(any());
        verify(outboxWriterService, never()).markAsFailed(any(), anyString());
    }

    @Test
    void shouldMarkFailedWhenRetryLimitReached() {
        event.setRetryCount(3);
        when(outboxWriterService.getUnprocessedEvents(100)).thenReturn(List.of(event));
        when(kafkaSenderService.send(event, 2)).thenReturn(false);

        processor.processOutbox();

        verify(outboxWriterService).markAsFailed(event.getId(), "Ошибка отправки в Kafka");
        verify(outboxWriterService, never()).incrementRetryCount(any(), anyString());
    }

    @Test
    void shouldHandleUnexpectedExceptionAndRetry() {
        when(outboxWriterService.getUnprocessedEvents(100)).thenReturn(List.of(event));
        when(kafkaSenderService.send(event, 2)).thenThrow(new IllegalStateException("broker error"));

        assertThatCode(processor::processOutbox).doesNotThrowAnyException();

        verify(outboxWriterService).incrementRetryCount(event.getId(), "broker error");
    }
}
