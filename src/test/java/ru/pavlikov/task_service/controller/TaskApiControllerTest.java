package ru.pavlikov.task_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.pavlikov.task_service.model.*;
import ru.pavlikov.task_service.service.TaskService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskApiControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskApiController controller;

    private UUID taskId;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        taskResponse = new TaskResponse().id(taskId).name("Task").status(TaskStatus.NEW);
    }

    @Test
    void getTasksShouldReturnOk() {
        Pageable pageable = PageRequest.of(0, 20);
        TaskPageResponse response = new TaskPageResponse().page(0).size(20).totalElements(0L).totalPages(0);
        when(taskService.getTasks(pageable)).thenReturn(response);

        ResponseEntity<TaskPageResponse> result = controller.getTasks(pageable);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(taskService).getTasks(pageable);
    }

    @Test
    void createTaskShouldReturnCreated() {
        CreateTaskRequest request = new CreateTaskRequest("Task");
        when(taskService.createTask(request)).thenReturn(taskResponse);

        ResponseEntity<TaskResponse> result = controller.createTask(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(taskResponse);
    }

    @Test
    void getTaskByIdShouldReturnOk() {
        when(taskService.getTaskById(taskId)).thenReturn(taskResponse);

        ResponseEntity<TaskResponse> result = controller.getTaskById(taskId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(taskResponse);
    }

    @Test
    void assignPerformerShouldReturnOk() {
        AssignPerformerRequest request = new AssignPerformerRequest(UUID.randomUUID());
        when(taskService.assignPerformer(taskId, request)).thenReturn(taskResponse);

        ResponseEntity<TaskResponse> result = controller.assignPerformer(taskId, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(taskResponse);
    }

    @Test
    void changeTaskStatusShouldReturnOk() {
        ChangeStatusRequest request = new ChangeStatusRequest(TaskStatus.DONE);
        when(taskService.changeTaskStatus(taskId, request)).thenReturn(taskResponse);

        ResponseEntity<TaskResponse> result = controller.changeTaskStatus(taskId, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(taskResponse);
    }
}
