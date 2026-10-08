package ru.pavlikov.task_service.infrastructure.outbox.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPropertiesTest {

    @Test
    void shouldExposeDefaultsAndAllowChangingProperties() {
        OutboxProperties properties = new OutboxProperties();

        assertThat(properties.getBatchSize()).isEqualTo(100);
        assertThat(properties.getSendTimeoutSec()).isEqualTo(10);
        assertThat(properties.getMaxRetries()).isEqualTo(5);
        assertThat(properties.getDelay()).isEqualTo(5000L);

        properties.setBatchSize(20);
        properties.setSendTimeoutSec(3);
        properties.setMaxRetries(7);
        properties.setDelay(1500L);

        assertThat(properties.getBatchSize()).isEqualTo(20);
        assertThat(properties.getSendTimeoutSec()).isEqualTo(3);
        assertThat(properties.getMaxRetries()).isEqualTo(7);
        assertThat(properties.getDelay()).isEqualTo(1500L);
    }
}
