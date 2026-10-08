package ru.pavlikov.task_service.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskStatusTest {

    @Test
    void shouldExposeSerializedValues() {
        assertThat(TaskStatus.NEW.getValue()).isEqualTo("NEW");
        assertThat(TaskStatus.IN_PROGRESS.getValue()).isEqualTo("IN_PROGRESS");
        assertThat(TaskStatus.DONE.getValue()).isEqualTo("DONE");
        assertThat(TaskStatus.HOLD.getValue()).isEqualTo("HOLD");
        assertThat(TaskStatus.DONE.toString()).isEqualTo("DONE");
    }

    @Test
    void shouldConvertKnownValue() {
        assertThat(TaskStatus.fromValue("HOLD")).isEqualTo(TaskStatus.HOLD);
    }

    @Test
    void shouldRejectUnknownValue() {
        assertThatThrownBy(() -> TaskStatus.fromValue("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unexpected value 'UNKNOWN'");
    }
}
