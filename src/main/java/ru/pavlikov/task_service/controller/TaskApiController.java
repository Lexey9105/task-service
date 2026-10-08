package ru.pavlikov.task_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.pavlikov.task_service.model.*;
import ru.pavlikov.task_service.service.TaskService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskApiController implements TaskApi {

    private final TaskService taskService;

    @Override
    public ResponseEntity<TaskPageResponse> getTasks(Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasks(pageable));
    }

    @Override
    public ResponseEntity<TaskResponse> createTask(@Valid CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @Override
    public ResponseEntity<TaskResponse> getTaskById(UUID taskId) {
        return ResponseEntity.ok(taskService.getTaskById(taskId));
    }

    @Override
    public ResponseEntity<TaskResponse> assignPerformer(UUID taskId, @Valid AssignPerformerRequest request) {
        return ResponseEntity.ok(taskService.assignPerformer(taskId, request));
    }

    @Override
    public ResponseEntity<TaskResponse> changeTaskStatus(UUID taskId, @Valid ChangeStatusRequest request) {
        return ResponseEntity.ok(taskService.changeTaskStatus(taskId, request));
    }
}