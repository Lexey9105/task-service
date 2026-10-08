package ru.pavlikov.task_service.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit-тесты для TaskService")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxWriterService outboxWriterService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TaskService taskService;

    private UUID taskId;
    private UUID performerId;
    private Task task;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(taskService, "taskEventsTopic", "task-events");
        ReflectionTestUtils.setField(taskService, "sourceService", "task-service");

        taskId = UUID.randomUUID();
        performerId = UUID.randomUUID();

        task = Task.builder()
                .id(taskId)
                .name("Test Task")
                .description("Test Description")
                .status(TaskStatus.NEW)
                .performerId(null)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Метод getTasks")
    class GetTasksTest {

        @Test
        @DisplayName("Должен вернуть страницу задач с корректной пагинацией")
        void getTasks_ShouldReturnPaginatedTasks() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Task> page = new PageImpl<>(List.of(task), pageable, 1);
            when(taskRepository.findAll(pageable)).thenReturn(page);

            TaskPageResponse result = taskService.getTasks(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getId()).isEqualTo(taskId);
            assertThat(result.getItems().get(0).getName()).isEqualTo("Test Task");
            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(20);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);

            verify(taskRepository).findAll(pageable);
            verifyNoInteractions(userRepository, outboxWriterService, objectMapper);
        }

        @Test
        @DisplayName("Должен вернуть пустую страницу если задач нет")
        void getTasks_ShouldReturnEmptyPage_WhenNoTasks() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Task> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(taskRepository.findAll(pageable)).thenReturn(emptyPage);

            TaskPageResponse result = taskService.getTasks(pageable);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();
        }
    }

    @Nested
    @DisplayName("Метод createTask")
    class CreateTaskTest {

        @Test
        @DisplayName("Должен создать задачу со статусом NEW и сохранить событие в outbox")
        void createTask_ShouldCreateTaskWithNewStatusAndSaveEvent() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setName("New Task");
            request.setDescription("Description");

            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                entity.setCreatedAt(OffsetDateTime.now());
                return entity;
            });
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"TASK_CREATED\"}");

            TaskResponse result = taskService.createTask(request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Task");
            assertThat(result.getDescription()).isEqualTo("Description");
            assertThat(result.getStatus()).isEqualTo(TaskStatus.NEW);
            assertThat(result.getPerformerId()).isNull();

            verify(taskRepository).save(argThat(entity ->
                    "New Task".equals(entity.getName())
                            && "Description".equals(entity.getDescription())
                            && entity.getStatus() == TaskStatus.NEW
                            && entity.getPerformerId() == null));
            verify(objectMapper).writeValueAsString(any());
            verify(outboxWriterService).saveEvent(argThat(command ->
                    "task-events".equals(command.getTopicName())
                            && "TASK_CREATED".equals(command.getEventType())
                            && command.getMessageKey() != null
                            && "task-service".equals(command.getSourceService())));
        }

        @Test
        @DisplayName("Не должен сохранять outbox, если сериализация события завершилась ошибкой")
        void createTask_ShouldThrowException_WhenSerializationFails() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setName("New Task");
            request.setDescription("Description");
            when(taskRepository.save(any(Task.class))).thenReturn(task);
            when(objectMapper.writeValueAsString(any()))
                    .thenThrow(new JacksonException("err") {});

            assertThatThrownBy(() -> taskService.createTask(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Ошибка сериализации");

            verify(outboxWriterService, never()).saveEvent(any());
        }
    }

    @Nested
    @DisplayName("Метод getTaskById")
    class GetTaskByIdTest {

        @Test
        @DisplayName("Должен вернуть задачу по существующему ID")
        void getTaskById_ShouldReturnTask_WhenTaskExists() {
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

            TaskResponse result = taskService.getTaskById(taskId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(taskId);
            assertThat(result.getName()).isEqualTo("Test Task");
            assertThat(result.getStatus()).isEqualTo(TaskStatus.NEW);
            verify(taskRepository).findById(taskId);
        }

        @Test
        @DisplayName("Должен выбросить исключение если задача не найдена")
        void getTaskById_ShouldThrowException_WhenTaskNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(nonExistentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("не найдена");

            verify(taskRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Метод assignPerformer")
    class AssignPerformerTest {

        @Test
        @DisplayName("Должен назначить исполнителя и сохранить событие в outbox")
        void assignPerformer_ShouldAssignPerformerAndSaveEvent() throws Exception {
            AssignPerformerRequest request = new AssignPerformerRequest(performerId);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(userRepository.existsById(performerId)).thenReturn(true);
            when(taskRepository.save(any(Task.class))).thenReturn(task);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"PERFORMER_ASSIGNED\"}");

            TaskResponse result = taskService.assignPerformer(taskId, request);

            assertThat(result).isNotNull();
            assertThat(result.getPerformerId()).isEqualTo(performerId);

            verify(taskRepository).findById(taskId);
            verify(userRepository).existsById(performerId);
            verify(taskRepository).save(argThat(entity -> performerId.equals(entity.getPerformerId())));
            verify(outboxWriterService).saveEvent(argThat(command ->
                    "PERFORMER_ASSIGNED".equals(command.getEventType())
                            && taskId.toString().equals(command.getMessageKey())));
        }

        @Test
        @DisplayName("Должен выбросить исключение если задача не найдена")
        void assignPerformer_ShouldThrowException_WhenTaskNotFound() {
            UUID nonExistentTaskId = UUID.randomUUID();
            AssignPerformerRequest request = new AssignPerformerRequest(performerId);
            when(taskRepository.findById(nonExistentTaskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.assignPerformer(nonExistentTaskId, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("не найдена");

            verify(userRepository, never()).existsById(any());
            verify(outboxWriterService, never()).saveEvent(any());
        }

        @Test
        @DisplayName("Должен выбросить исключение если пользователь не найден")
        void assignPerformer_ShouldThrowException_WhenUserNotFound() {
            AssignPerformerRequest request = new AssignPerformerRequest(performerId);
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(userRepository.existsById(performerId)).thenReturn(false);

            assertThatThrownBy(() -> taskService.assignPerformer(taskId, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Пользователь не найден");

            verify(taskRepository, never()).save(any());
            verify(outboxWriterService, never()).saveEvent(any());
        }
    }

    @Nested
    @DisplayName("Метод changeTaskStatus")
    class ChangeTaskStatusTest {

        @Test
        @DisplayName("Должен изменить статус существующей задачи")
        void changeTaskStatus_ShouldChangeStatus() {
            ChangeStatusRequest request = new ChangeStatusRequest(TaskStatus.IN_PROGRESS);
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            TaskResponse result = taskService.changeTaskStatus(taskId, request);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            verify(taskRepository).save(argThat(entity -> entity.getStatus() == TaskStatus.IN_PROGRESS));
            verifyNoInteractions(outboxWriterService);
        }

        @Test
        @DisplayName("Должен выбросить исключение если задача не найдена")
        void changeTaskStatus_ShouldThrowException_WhenTaskNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            ChangeStatusRequest request = new ChangeStatusRequest(TaskStatus.DONE);
            when(taskRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.changeTaskStatus(nonExistentId, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("не найдена");

            verify(taskRepository, never()).save(any());
        }
    }
}
