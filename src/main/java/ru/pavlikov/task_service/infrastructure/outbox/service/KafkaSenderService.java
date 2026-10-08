package ru.pavlikov.task_service.infrastructure.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.pavlikov.task_service.infrastructure.outbox.entity.OutboxEntity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaSenderService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public boolean send(OutboxEntity event, int timeoutSec) {
        try {
            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(event.getTopicName(), event.getMessageKey(), event.getPayload());

            SendResult<String, String> result = future.get(timeoutSec, TimeUnit.SECONDS);

            log.info("Сообщение успешно отправлено в Kafka: topic={}, partition={}, offset={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

            return true;

        } catch (Exception e) {
            log.error("Ошибка отправки сообщения в Kafka: topic={}, id={}, error={}",
                    event.getTopicName(), event.getId(), e.getMessage(), e);
            return false;
        }
    }
}