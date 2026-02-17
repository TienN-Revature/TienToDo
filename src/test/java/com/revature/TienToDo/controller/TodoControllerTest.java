package com.revature.TienToDo.controller;


import com.revature.TienToDo.dto.SubtaskResponse;
import com.revature.TienToDo.dto.TodoRequest;
import com.revature.TienToDo.dto.TodoResponse;
import com.revature.TienToDo.dto.TodoUpdateRequest;
import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.service.AuthService;
import com.revature.TienToDo.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TodoControllerTest {

    @Mock
    private TodoService todoService;

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TodoController todoController;

    private User testUser;
    private TodoResponse todoResponse;
    private SubtaskResponse subtaskResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");

        todoResponse = new TodoResponse();
        todoResponse.setId(1L);
        todoResponse.setTitle("Buy groceries");
        todoResponse.setDescription("Milk, eggs, bread");
        todoResponse.setCompleted(false);
        todoResponse.setCreatedAt(LocalDateTime.now());
        todoResponse.setUpdatedAt(LocalDateTime.now());
        todoResponse.setSubtasks(new ArrayList<>());

        subtaskResponse = new SubtaskResponse();
        subtaskResponse.setId(1L);
        subtaskResponse.setTitle("Buy milk");
        subtaskResponse.setCompleted(false);
        subtaskResponse.setCreatedAt(LocalDateTime.now());
        subtaskResponse.setUpdatedAt(LocalDateTime.now());
    }

    private void stubAuth() {
        when(authentication.getName()).thenReturn("john_doe");
        when(authService.getUserByUsername("john_doe")).thenReturn(testUser);
    }

    @Nested
    @DisplayName("GET /api/todos")
    class GetAllTodosTests {

        @Test
        @DisplayName("should return list of todos")
        void getAllTodos_Success() {
            stubAuth();
            when(todoService.getAllTodos(testUser)).thenReturn(List.of(todoResponse));

            ResponseEntity<List<TodoResponse>> response = todoController.getAllTodos(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getTitle()).isEqualTo("Buy groceries");
        }

        @Test
        @DisplayName("should return empty list when no todos")
        void getAllTodos_Empty() {
            stubAuth();
            when(todoService.getAllTodos(testUser)).thenReturn(List.of());

            ResponseEntity<List<TodoResponse>> response = todoController.getAllTodos(authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/todos/{todoId}")
    class GetTodoByIdTests {

        @Test
        @DisplayName("should return todo by id")
        void getTodoById_Success() {
            stubAuth();
            when(todoService.getTodoById(1L, testUser)).thenReturn(todoResponse);

            ResponseEntity<TodoResponse> response = todoController.getTodoById(1L, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getTitle()).isEqualTo("Buy groceries");
        }

        @Test
        @DisplayName("should throw when todo not found")
        void getTodoById_NotFound() {
            stubAuth();
            when(todoService.getTodoById(99L, testUser))
                    .thenThrow(new TodoService.ResourceNotFoundException("Todo not found"));

            assertThrows(TodoService.ResourceNotFoundException.class,
                    () -> todoController.getTodoById(99L, authentication));
        }
    }

    @Nested
    @DisplayName("POST /api/todos")
    class CreateTodoTests {

        @Test
        @DisplayName("should create todo and return 201")
        void createTodo_Success() {
            stubAuth();
            TodoRequest request = new TodoRequest();
            request.setTitle("Buy groceries");
            request.setDescription("Milk, eggs, bread");

            when(todoService.createTodo(any(TodoRequest.class), eq(testUser))).thenReturn(todoResponse);

            ResponseEntity<TodoResponse> response = todoController.createTodo(request, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getTitle()).isEqualTo("Buy groceries");
            verify(todoService).createTodo(any(TodoRequest.class), eq(testUser));
        }
    }

    @Nested
    @DisplayName("PUT /api/todos/{todoId}")
    class UpdateTodoTests {

        @Test
        @DisplayName("should update todo successfully")
        void updateTodo_Success() {
            stubAuth();
            TodoUpdateRequest request = new TodoUpdateRequest();
            request.setTitle("Updated title");

            TodoResponse updated = new TodoResponse();
            updated.setId(1L);
            updated.setTitle("Updated title");
            updated.setCompleted(false);
            updated.setSubtasks(new ArrayList<>());

            when(todoService.updateTodo(eq(1L), any(TodoUpdateRequest.class), eq(testUser)))
                    .thenReturn(updated);

            ResponseEntity<TodoResponse> response = todoController.updateTodo(1L, request, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getTitle()).isEqualTo("Updated title");
        }

        @Test
        @DisplayName("should throw when todo not found")
        void updateTodo_NotFound() {
            stubAuth();
            TodoUpdateRequest request = new TodoUpdateRequest();
            request.setTitle("Updated title");

            when(todoService.updateTodo(eq(99L), any(TodoUpdateRequest.class), eq(testUser)))
                    .thenThrow(new TodoService.ResourceNotFoundException("Todo not found"));

            assertThrows(TodoService.ResourceNotFoundException.class,
                    () -> todoController.updateTodo(99L, request, authentication));
        }
    }

    @Nested
    @DisplayName("PATCH /api/todos/{todoId}/complete")
    class MarkTodoCompleteTests {

        @Test
        @DisplayName("should mark todo as complete")
        void markTodoComplete_Success() {
            stubAuth();
            TodoResponse completed = new TodoResponse();
            completed.setId(1L);
            completed.setTitle("Buy groceries");
            completed.setCompleted(true);
            completed.setSubtasks(new ArrayList<>());

            when(todoService.markTodoComplete(1L, testUser)).thenReturn(completed);

            ResponseEntity<TodoResponse> response = todoController.markTodoComplete(1L, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isCompleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("DELETE /api/todos/{todoId}")
    class DeleteTodoTests {

        @Test
        @DisplayName("should delete todo and return 204")
        void deleteTodo_Success() {
            stubAuth();
            doNothing().when(todoService).deleteTodo(1L, testUser);

            ResponseEntity<Void> response = todoController.deleteTodo(1L, authentication);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(todoService).deleteTodo(1L, testUser);
        }

        @Test
        @DisplayName("should throw when todo not found")
        void deleteTodo_NotFound() {
            stubAuth();
            doThrow(new TodoService.ResourceNotFoundException("Todo not found"))
                    .when(todoService).deleteTodo(99L, testUser);

            assertThrows(TodoService.ResourceNotFoundException.class,
                    () -> todoController.deleteTodo(99L, authentication));
        }
    }
}
