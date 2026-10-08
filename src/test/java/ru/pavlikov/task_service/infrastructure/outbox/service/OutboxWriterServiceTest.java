package ru.pavlikov.task_service.infrastructure.outbox.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import ru.pavlikov.task_service.infrastructure.outbox.dto.OutboxCommand;
import ru.pavlikov.task_service.infrastructure.outbox.entity.OutboxEntity;
import ru.pavlikov.task_service.infrastructure.outbox.repository.OutboxRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxWriterServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxWriterService service;

    @Test
    void shouldSaveCommandAsNewOutboxEntity() {
        OutboxCommand command = OutboxCommand.builder()
                .topicName("task-events")
                .messageKey("task-1")
                .eventType("TASK_CREATED")
                .sourceService("task-service")
                .payload("{}")
                .headers(Map.of("traceId", "abc"))
                .build();

        service.saveEvent(command);

        ArgumentCaptor<OutboxEntity> captor = ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEntity entity = captor.getValue();
        assertThat(entity.getTopicName()).isEqualTo("task-events");
        assertThat(entity.getMessageKey()).isEqualTo("task-1");
        assertThat(entity.getEventType()).isEqualTo("TASK_CREATED");
        assertThat(entity.getSourceService()).isEqualTo("task-service");
        assertThat(entity.getPayload()).isEqualTo("{}");
        assertThat(entity.getHeaders()).containsEntry("traceId", "abc");
        assertThat(entity.getProcessed()).isFalse();
        assertThat(entity.getFailed()).isFalse();
        assertThat(entity.getRetryCount()).isZero();
    }

    @Test
    void shouldGetUnprocessedEventsWithRequestedBatchSize() {
        List<OutboxEntity> events = List.of(OutboxEntity.builder().id(UUID.randomUUID()).build());
        when(outboxRepository.findUnprocessedEvents(PageRequest.of(0, 10))).thenReturn(events);

        List<OutboxEntity> result = service.getUnprocessedEvents(10);

        assertThat(result).isSameAs(events);
        verify(outboxRepository).findUnprocessedEvents(PageRequest.of(0, 10));
    }

    @Test
    void shouldMarkEventProcessed() {
        UUID id = UUID.randomUUID();

        service.markAsProcessed(id);

        verify(outboxRepository).markAsProcessed(id);
    }

    @Test
    void shouldIncrementRetryCount() {
        UUID id = UUID.randomUUID();

        service.incrementRetryCount(id, "Kafka timeout");

        verify(outboxRepository).incrementRetryCount(id, "Kafka timeout");
    }

    @Test
    void shouldMarkEventFailed() {
        UUID id = UUID.randomUUID();

        service.markAsFailed(id, "Max retries exceeded");

        verify(outboxRepository).markAsFailed(id, "Max retries exceeded");
    }
}
