package ru.pavlikov.task_service.infrastructure.outbox.service;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.pavlikov.task_service.infrastructure.outbox.entity.OutboxEntity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaSenderServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private SendResult<String, String> sendResult;

    @Mock
    private RecordMetadata metadata;

    @Test
    void shouldReturnTrueWhenKafkaSendSucceeds() {
        KafkaSenderService service = new KafkaSenderService(kafkaTemplate);
        OutboxEntity event = event();
        when(kafkaTemplate.send(event.getTopicName(), event.getMessageKey(), event.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(metadata.topic()).thenReturn("task-events");
        when(metadata.partition()).thenReturn(2);
        when(metadata.offset()).thenReturn(10L);

        boolean result = service.send(event, 1);

        assertThat(result).isTrue();
        verify(kafkaTemplate).send(event.getTopicName(), event.getMessageKey(), event.getPayload());
    }

    @Test
    void shouldReturnFalseWhenFutureFails() {
        KafkaSenderService service = new KafkaSenderService(kafkaTemplate);
        OutboxEntity event = event();
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(event.getTopicName(), event.getMessageKey(), event.getPayload())).thenReturn(failed);

        boolean result = service.send(event, 1);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenSendThrowsImmediately() {
        KafkaSenderService service = new KafkaSenderService(kafkaTemplate);
        OutboxEntity event = event();
        when(kafkaTemplate.send(event.getTopicName(), event.getMessageKey(), event.getPayload()))
                .thenThrow(new IllegalStateException("producer not available"));

        boolean result = service.send(event, 1);

        assertThat(result).isFalse();
    }

    private OutboxEntity event() {
        return OutboxEntity.builder()
                .id(UUID.randomUUID())
                .topicName("task-events")
                .messageKey("key")
                .payload("{\"event\":\"TASK_CREATED\"}")
                .eventType("TASK_CREATED")
                .sourceService("task-service")
                .build();
    }
}
