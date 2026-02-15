package com.revature.TienToDo.controller;


import com.revature.TienToDo.dto.*;
import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.service.AuthService;

import com.revature.TienToDo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
public class TodoController {
    @Autowired
    private TodoService todoService;

    @Autowired
    private AuthService authService;

    @GetMapping
    public ResponseEntity<List<TodoResponse>> getAllTodos(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(todoService.getAllTodos(user));
    }

    @GetMapping("/{todoId}")
    public ResponseEntity<TodoResponse> getTodoById(
            @PathVariable Long todoId, Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(todoService.getTodoById(todoId, user));
    }

    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
            @Valid @RequestBody TodoRequest request, Authentication auth) {
        User user = getUser(auth);
        TodoResponse response = todoService.createTodo(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{todoId}")
    public ResponseEntity<TodoResponse> updateTodo(
            @PathVariable Long todoId,
            @Valid @RequestBody TodoUpdateRequest request,
            Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(todoService.updateTodo(todoId, request, user));
    }

    @PatchMapping("/{todoId}/complete")
    public ResponseEntity<TodoResponse> markTodoComplete(
            @PathVariable Long todoId, Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(todoService.markTodoComplete(todoId, user));
    }

    @DeleteMapping("/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable Long todoId, Authentication auth) {
        User user = getUser(auth);
        todoService.deleteTodo(todoId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<TodoResponse>> getActiveTodos(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(todoService.getActiveTodos(user));
    }

    @GetMapping("/completed")
    public ResponseEntity<List<TodoResponse>> getCompletedTodos(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(todoService.getCompletedTodos(user));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TodoResponse>> searchTodos(
            @RequestParam("q") String keyword, Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(todoService.searchTodos(keyword, user));
    }

    @GetMapping("/{todoId}/subtasks")
    public ResponseEntity<List<SubtaskResponse>> getSubtasks(
            @PathVariable Long todoId, Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(todoService.getSubtasksByTodoId(todoId, user));
    }

    @PostMapping("/{todoId}/subtasks")
    public ResponseEntity<SubtaskResponse> createSubtask(
            @PathVariable Long todoId,
            @Valid @RequestBody SubtaskRequest request,
            Authentication auth) {
        User user = getUser(auth);
        SubtaskResponse response = todoService.createSubtask(todoId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{todoId}/subtasks/{subtaskId}")
    public ResponseEntity<SubtaskResponse> updateSubtask(
            @PathVariable Long todoId,
            @PathVariable Long subtaskId,
            @Valid @RequestBody SubtaskUpdateRequest request,
            Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(
                todoService.updateSubtask(todoId, subtaskId, request, user));
    }

    @PatchMapping("/{todoId}/subtasks/{subtaskId}/complete")
    public ResponseEntity<SubtaskResponse> markSubtaskComplete(
            @PathVariable Long todoId,
            @PathVariable Long subtaskId,
            Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(
                todoService.markSubtaskComplete(todoId, subtaskId, user));
    }

    @DeleteMapping("/{todoId}/subtasks/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(
            @PathVariable Long todoId,
            @PathVariable Long subtaskId,
            Authentication auth) {
        User user = getUser(auth);
        todoService.deleteSubtask(todoId, subtaskId, user);
        return ResponseEntity.noContent().build();
    }

    private User getUser(Authentication auth) {
        return authService.getUserByUsername(auth.getName());
    }
}
