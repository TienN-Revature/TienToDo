package com.revature.TienToDo.service;

import com.revature.TienToDo.dto.*;
import com.revature.TienToDo.entity.Subtask;
import com.revature.TienToDo.entity.Todo;
import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.repository.SubtaskRepository;
import com.revature.TienToDo.repository.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TodoService {
    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private SubtaskRepository subtaskRepository;

    @Transactional(readOnly = true)
    public List<TodoResponse> getAllTodos(User user) {
        return todoRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapTodoToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodoById(Long todoId, User user) {
        Todo todo = findTodoByIdAndUser(todoId, user);
        return mapTodoToResponse(todo);
    }

    public TodoResponse createTodo(TodoRequest request, User user) {
        Todo todo = new Todo();
        todo.setTitle(request.getTitle());
        todo.setDescription(request.getDescription());
        todo.setUser(user);
        todo = todoRepository.save(todo);
        return mapTodoToResponse(todo);
    }

    public TodoResponse updateTodo(Long todoId, TodoUpdateRequest request, User user) {
        Todo todo = findTodoByIdAndUser(todoId, user);

        if (request.getTitle() != null) {
            todo.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            todo.setDescription(request.getDescription());
        }
        if (request.getCompleted() != null) {
            todo.setCompleted(request.getCompleted());
        }

        todo = todoRepository.save(todo);
        return mapTodoToResponse(todo);
    }

    public void deleteTodo(Long todoId, User user) {
        Todo todo = findTodoByIdAndUser(todoId, user);
        todoRepository.delete(todo);
    }

    public TodoResponse markTodoComplete(Long todoId, User user) {
        Todo todo = findTodoByIdAndUser(todoId, user);
        todo.setCompleted(true);
        todo = todoRepository.save(todo);
        return mapTodoToResponse(todo);
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> getActiveTodos(User user) {
        return todoRepository.findActiveTodosByUserId(user.getId())
                .stream()
                .map(this::mapTodoToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> getCompletedTodos(User user) {
        return todoRepository.findCompletedTodosByUserId(user.getId())
                .stream()
                .map(this::mapTodoToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> searchTodos(String keyword, User user) {
        return todoRepository.searchByKeyword(user.getId(), keyword)
                .stream()
                .map(this::mapTodoToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubtaskResponse> getSubtasksByTodoId(Long todoId, User user) {
        findTodoByIdAndUser(todoId, user);
        return subtaskRepository.findByTodoIdOrderByCreatedAtAsc(todoId)
                .stream()
                .map(this::mapSubtaskToResponse)
                .collect(Collectors.toList());
    }

    public SubtaskResponse createSubtask(Long todoId, SubtaskRequest request, User user) {
        Todo todo = findTodoByIdAndUser(todoId, user);

        Subtask subtask = new Subtask();
        subtask.setTitle(request.getTitle());
        subtask.setTodo(todo);
        subtask = subtaskRepository.save(subtask);
        return mapSubtaskToResponse(subtask);
    }

    public SubtaskResponse updateSubtask(Long todoId, Long subtaskId,
                                         SubtaskUpdateRequest request, User user) {
        findTodoByIdAndUser(todoId, user);
        Subtask subtask = findSubtaskByIdAndTodoId(subtaskId, todoId);

        if (request.getTitle() != null) {
            subtask.setTitle(request.getTitle());
        }
        if (request.getCompleted() != null) {
            subtask.setCompleted(request.getCompleted());
        }

        subtask = subtaskRepository.save(subtask);
        return mapSubtaskToResponse(subtask);
    }

    public void deleteSubtask(Long todoId, Long subtaskId, User user) {
        findTodoByIdAndUser(todoId, user);
        Subtask subtask = findSubtaskByIdAndTodoId(subtaskId, todoId);
        subtaskRepository.delete(subtask);
    }

    public SubtaskResponse markSubtaskComplete(Long todoId, Long subtaskId, User user) {
        findTodoByIdAndUser(todoId, user);
        Subtask subtask = findSubtaskByIdAndTodoId(subtaskId, todoId);
        subtask.setCompleted(true);
        subtask = subtaskRepository.save(subtask);
        return mapSubtaskToResponse(subtask);
    }

    private Todo findTodoByIdAndUser(Long todoId, User user) {
        return todoRepository.findByIdAndUserId(todoId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Todo not found"));
    }

    private Subtask findSubtaskByIdAndTodoId(Long subtaskId, Long todoId) {
        return subtaskRepository.findByIdAndTodoId(subtaskId, todoId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask not found"));
    }

    private TodoResponse mapTodoToResponse(Todo todo) {
        TodoResponse response = new TodoResponse();
        response.setId(todo.getId());
        response.setTitle(todo.getTitle());
        response.setDescription(todo.getDescription());
        response.setCompleted(todo.isCompleted());
        response.setCreatedAt(todo.getCreatedAt());
        response.setUpdatedAt(todo.getUpdatedAt());
        response.setSubtasks(
                todo.getSubtasks().stream()
                        .map(this::mapSubtaskToResponse)
                        .collect(Collectors.toList())
        );
        return response;
    }

    private SubtaskResponse mapSubtaskToResponse(Subtask subtask) {
        SubtaskResponse response = new SubtaskResponse();
        response.setId(subtask.getId());
        response.setTitle(subtask.getTitle());
        response.setCompleted(subtask.isCompleted());
        response.setCreatedAt(subtask.getCreatedAt());
        response.setUpdatedAt(subtask.getUpdatedAt());
        return response;
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
