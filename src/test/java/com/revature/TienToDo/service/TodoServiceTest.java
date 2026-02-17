package com.revature.TienToDo.service;

import com.revature.TienToDo.dto.*;
import com.revature.TienToDo.entity.Subtask;
import com.revature.TienToDo.entity.Todo;
import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.repository.SubtaskRepository;
import com.revature.TienToDo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TodoServiceTest {
    @Mock
    private TodoRepository todoRepository;

    @Mock
    private SubtaskRepository subtaskRepository;

    @InjectMocks
    private TodoService todoService;

    private User testUser;
    private Todo testTodo;
    private Subtask testSubtask;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");

        testTodo = new Todo();
        testTodo.setId(1L);
        testTodo.setTitle("Buy groceries");
        testTodo.setDescription("Milk, eggs, bread");
        testTodo.setCompleted(false);
        testTodo.setUser(testUser);
        testTodo.setCreatedAt(LocalDateTime.now());
        testTodo.setUpdatedAt(LocalDateTime.now());
        testTodo.setSubtasks(new ArrayList<>());

        testSubtask = new Subtask();
        testSubtask.setId(1L);
        testSubtask.setTitle("Buy milk");
        testSubtask.setCompleted(false);
        testSubtask.setTodo(testTodo);
        testSubtask.setCreatedAt(LocalDateTime.now());
        testSubtask.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("getAllTodos()")
    class GetAllTodosTests {

        @Test
        @DisplayName("should return all todos for user")
        void getAllTodos_Success() {
            when(todoRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(testTodo));

            List<TodoResponse> result = todoService.getAllTodos(testUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Buy groceries");
        }

        @Test
        @DisplayName("should return empty list when no todos")
        void getAllTodos_Empty() {
            when(todoRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            List<TodoResponse> result = todoService.getAllTodos(testUser);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTodoById()")
    class GetTodoByIdTests {

        @Test
        @DisplayName("should return todo when owned by user")
        void getTodoById_Success() {
            when(todoRepository.findByIdAndUserId(1L, 1L))
                    .thenReturn(Optional.of(testTodo));

            TodoResponse result = todoService.getTodoById(1L, testUser);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Buy groceries");
        }

        @Test
        @DisplayName("should throw when todo not found or not owned")
        void getTodoById_NotFound() {
            when(todoRepository.findByIdAndUserId(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> todoService.getTodoById(99L, testUser))
                    .isInstanceOf(TodoService.ResourceNotFoundException.class)
                    .hasMessage("Todo not found");
        }
    }

    @Nested
    @DisplayName("createTodo()")
    class CreateTodoTests {

        @Test
        @DisplayName("should create todo and set user ownership")
        void createTodo_Success() {
            TodoRequest request = new TodoRequest();
            request.setTitle("New task");
            request.setDescription("Description");

            when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
                Todo saved = invocation.getArgument(0);
                saved.setId(2L);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                saved.setSubtasks(new ArrayList<>());
                return saved;
            });

            TodoResponse result = todoService.createTodo(request, testUser);

            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getTitle()).isEqualTo("New task");

            verify(todoRepository).save(argThat(todo ->
                    todo.getTitle().equals("New task")
                            && todo.getUser().equals(testUser)
            ));
        }
    }

    @Nested
    @DisplayName("updateTodo()")
    class UpdateTodoTests {

        @Test
        @DisplayName("should update only non-null fields")
        void updateTodo_PartialUpdate() {
            TodoUpdateRequest request = new TodoUpdateRequest();
            request.setTitle("Updated title");
            // description and completed are null — should not change

            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenReturn(testTodo);

            todoService.updateTodo(1L, request, testUser);

            assertThat(testTodo.getTitle()).isEqualTo("Updated title");
            assertThat(testTodo.getDescription()).isEqualTo("Milk, eggs, bread"); // unchanged
            assertThat(testTodo.isCompleted()).isFalse(); // unchanged
        }

        @Test
        @DisplayName("should update all fields when provided")
        void updateTodo_FullUpdate() {
            TodoUpdateRequest request = new TodoUpdateRequest();
            request.setTitle("New title");
            request.setDescription("New description");
            request.setCompleted(true);

            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenReturn(testTodo);

            todoService.updateTodo(1L, request, testUser);

            assertThat(testTodo.getTitle()).isEqualTo("New title");
            assertThat(testTodo.getDescription()).isEqualTo("New description");
            assertThat(testTodo.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("should throw when todo not found")
        void updateTodo_NotFound() {
            when(todoRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> todoService.updateTodo(99L, new TodoUpdateRequest(), testUser))
                    .isInstanceOf(TodoService.ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteTodo()")
    class DeleteTodoTests {

        @Test
        @DisplayName("should delete todo owned by user")
        void deleteTodo_Success() {
            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));

            todoService.deleteTodo(1L, testUser);

            verify(todoRepository).delete(testTodo);
        }

        @Test
        @DisplayName("should throw when deleting non-existent todo")
        void deleteTodo_NotFound() {
            when(todoRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> todoService.deleteTodo(99L, testUser))
                    .isInstanceOf(TodoService.ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markTodoComplete()")
    class MarkCompleteTests {

        @Test
        @DisplayName("should set completed to true")
        void markTodoComplete_Success() {
            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));
            when(todoRepository.save(any(Todo.class))).thenReturn(testTodo);

            todoService.markTodoComplete(1L, testUser);

            assertThat(testTodo.isCompleted()).isTrue();
            verify(todoRepository).save(testTodo);
        }
    }

    @Nested
    @DisplayName("Filter and Search")
    class FilterSearchTests {

        @Test
        @DisplayName("getActiveTodos should return only incomplete todos")
        void getActiveTodos() {
            when(todoRepository.findActiveTodosByUserId(1L)).thenReturn(List.of(testTodo));

            List<TodoResponse> result = todoService.getActiveTodos(testUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isCompleted()).isFalse();
        }

        @Test
        @DisplayName("getCompletedTodos should return only completed todos")
        void getCompletedTodos() {
            testTodo.setCompleted(true);
            when(todoRepository.findCompletedTodosByUserId(1L)).thenReturn(List.of(testTodo));

            List<TodoResponse> result = todoService.getCompletedTodos(testUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isCompleted()).isTrue();
        }

        @Test
        @DisplayName("searchTodos should pass keyword to repository")
        void searchTodos() {
            when(todoRepository.searchByKeyword(1L, "groceries")).thenReturn(List.of(testTodo));

            List<TodoResponse> result = todoService.searchTodos("groceries", testUser);

            assertThat(result).hasSize(1);
            verify(todoRepository).searchByKeyword(1L, "groceries");
        }

        @Test
        @DisplayName("searchTodos should return empty for no match")
        void searchTodos_NoMatch() {
            when(todoRepository.searchByKeyword(1L, "xyz")).thenReturn(List.of());

            List<TodoResponse> result = todoService.searchTodos("xyz", testUser);

            assertThat(result).isEmpty();
        }
    }

    // ==================== SUBTASK CRUD ====================

    @Nested
    @DisplayName("Subtask CRUD")
    class SubtaskTests {

        @Test
        @DisplayName("getSubtasksByTodoId should verify ownership then return subtasks")
        void getSubtasks_Success() {
            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));
            when(subtaskRepository.findByTodoIdOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(testSubtask));

            List<SubtaskResponse> result = todoService.getSubtasksByTodoId(1L, testUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Buy milk");
        }

        @Test
        @DisplayName("getSubtasksByTodoId should throw when todo not owned")
        void getSubtasks_TodoNotOwned() {
            when(todoRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> todoService.getSubtasksByTodoId(99L, testUser))
                    .isInstanceOf(TodoService.ResourceNotFoundException.class)
                    .hasMessage("Todo not found");
        }

        @Test
        @DisplayName("createSubtask should link to parent todo")
        void createSubtask_Success() {
            SubtaskRequest request = new SubtaskRequest();
            request.setTitle("Buy eggs");

            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));
            when(subtaskRepository.save(any(Subtask.class))).thenAnswer(invocation -> {
                Subtask saved = invocation.getArgument(0);
                saved.setId(2L);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                return saved;
            });

            SubtaskResponse result = todoService.createSubtask(1L, request, testUser);

            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getTitle()).isEqualTo("Buy eggs");

            verify(subtaskRepository).save(argThat(subtask ->
                    subtask.getTitle().equals("Buy eggs")
                            && subtask.getTodo().equals(testTodo)
            ));
        }

        @Test
        @DisplayName("updateSubtask should update only non-null fields")
        void updateSubtask_PartialUpdate() {
            SubtaskUpdateRequest request = new SubtaskUpdateRequest();
            request.setTitle("Buy oat milk");
            // completed is null — should not change

            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));
            when(subtaskRepository.findByIdAndTodoId(1L, 1L)).thenReturn(Optional.of(testSubtask));
            when(subtaskRepository.save(any(Subtask.class))).thenReturn(testSubtask);

            todoService.updateSubtask(1L, 1L, request, testUser);

            assertThat(testSubtask.getTitle()).isEqualTo("Buy oat milk");
            assertThat(testSubtask.isCompleted()).isFalse(); // unchanged
        }

        @Test
        @DisplayName("deleteSubtask should verify ownership chain")
        void deleteSubtask_Success() {
            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));
            when(subtaskRepository.findByIdAndTodoId(1L, 1L)).thenReturn(Optional.of(testSubtask));

            todoService.deleteSubtask(1L, 1L, testUser);

            verify(subtaskRepository).delete(testSubtask);
        }

        @Test
        @DisplayName("deleteSubtask should throw when subtask not found")
        void deleteSubtask_SubtaskNotFound() {
            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));
            when(subtaskRepository.findByIdAndTodoId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> todoService.deleteSubtask(1L, 99L, testUser))
                    .isInstanceOf(TodoService.ResourceNotFoundException.class)
                    .hasMessage("Subtask not found");
        }

        @Test
        @DisplayName("markSubtaskComplete should set completed to true")
        void markSubtaskComplete_Success() {
            when(todoRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testTodo));
            when(subtaskRepository.findByIdAndTodoId(1L, 1L)).thenReturn(Optional.of(testSubtask));
            when(subtaskRepository.save(any(Subtask.class))).thenReturn(testSubtask);

            todoService.markSubtaskComplete(1L, 1L, testUser);

            assertThat(testSubtask.isCompleted()).isTrue();
        }
    }

}
