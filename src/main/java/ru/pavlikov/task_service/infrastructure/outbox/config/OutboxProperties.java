package ru.pavlikov.task_service.infrastructure.outbox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "outbox.processor")
public class OutboxProperties {
    private int batchSize = 100;
    private int sendTimeoutSec = 10;
    private int maxRetries = 5;
    private long delay = 5000;
}