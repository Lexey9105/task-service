package ru.pavlikov.task_service.infrastructure.outbox.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class OutboxCommand {
    private String topicName;
    private String messageKey;
    private String eventType;
    private String sourceService;
    private String payload;
    private Map<String, String> headers;
}