package ru.pavlikov.task_service.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pavlikov.task_service.infrastructure.outbox.dto.OutboxCommand;
import ru.pavlikov.task_service.infrastructure.outbox.service.OutboxWriterService;
import ru.pavlikov.task_service.model.AssignPerformerRequest;
import ru.pavlikov.task_service.model.ChangeStatusRequest;
import ru.pavlikov.task_service.model.CreateTaskRequest;
import ru.pavlikov.task_service.model.Task;
import ru.pavlikov.task_service.model.TaskPageResponse;
import ru.pavlikov.task_service.model.TaskResponse;
import ru.pavlikov.task_service.model.TaskStatus;
import ru.pavlikov.task_service.repository.TaskRepository;
import ru.pavlikov.task_service.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final OutboxWriterService outboxWriterService;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.task-events:task-events}")
    private String taskEventsTopic;

    @Value("${spring.application.name:task-service}")
    private String sourceService;

    public TaskPageResponse getTasks(Pageable pageable) {
        Page<Task> entityPage = taskRepository.findAll(pageable);

        List<TaskResponse> items = entityPage.getContent().stream()
                .map(this::toDto)
                .toList();

        TaskPageResponse response = new TaskPageResponse();
        response.setItems(items);
        response.setPage(entityPage.getNumber());
        response.setSize(entityPage.getSize());
        response.setTotalElements(entityPage.getTotalElements());
        response.setTotalPages(entityPage.getTotalPages());
        return response;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        Task entity = Task.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(TaskStatus.NEW)
                .performerId(null)
                .build();

        Task saved = taskRepository.save(entity);
        saveTaskCreatedEvent(saved);

        return toDto(saved);
    }

    public TaskResponse getTaskById(UUID taskId) {
        return toDto(getEntityOrThrow(taskId));
    }

    @Transactional
    public TaskResponse assignPerformer(UUID taskId, AssignPerformerRequest request) {
        Task entity = getEntityOrThrow(taskId);

        UUID performerId = request.getPerformerId();
        if (!userRepository.existsById(performerId)) {
            throw new EntityNotFoundException("Пользователь не найден: " + performerId);
        }

        entity.setPerformerId(performerId);
        Task saved = taskRepository.save(entity);
        savePerformerAssignedEvent(saved, performerId);

        return toDto(saved);
    }

    @Transactional
    public TaskResponse changeTaskStatus(UUID taskId, ChangeStatusRequest request) {
        Task entity = getEntityOrThrow(taskId);
        entity.setStatus(request.getStatus());
        return toDto(taskRepository.save(entity));
    }

    private void saveTaskCreatedEvent(Task task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "TASK_CREATED");
        payload.put("taskId", task.getId());
        payload.put("name", task.getName());
        payload.put("description", task.getDescription());
        payload.put("status", task.getStatus());
        payload.put("performerId", task.getPerformerId());

        saveEvent("TASK_CREATED", task.getId(), payload);
    }

    private void savePerformerAssignedEvent(Task task, UUID performerId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "PERFORMER_ASSIGNED");
        payload.put("taskId", task.getId());
        payload.put("performerId", performerId);

        saveEvent("PERFORMER_ASSIGNED", task.getId(), payload);
    }

    private void saveEvent(String eventType, UUID taskId, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            outboxWriterService.saveEvent(OutboxCommand.builder()
                    .topicName(taskEventsTopic)
                    .messageKey(taskId.toString())
                    .eventType(eventType)
                    .sourceService(sourceService)
                    .payload(json)
                    .headers(Map.of("eventType", eventType))
                    .build());
        } catch (JacksonException e) {
            throw new IllegalStateException("Ошибка сериализации события " + eventType, e);
        }
    }

    private Task getEntityOrThrow(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Задача с ID " + taskId + " не найдена"));
    }

    private TaskResponse toDto(Task e) {
        TaskResponse dto = new TaskResponse();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setPerformerId(e.getPerformerId());
        dto.setDescription(e.getDescription());
        dto.setStatus(e.getStatus());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
