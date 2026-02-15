package com.revature.TienToDo.repository;

import com.revature.TienToDo.entity.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {
    List<Subtask> findByTodoIdOrderByCreatedAtAsc(Long todoId);
    Optional<Subtask> findByIdAndTodoId(Long id, Long todoId);
    boolean existsByIdAndTodoId(Long id, Long todoId);

    List<Subtask> findByTodoIdAndCompletedOrderByCreatedAtAsc(Long todoId, boolean completed);
    @Query("SELECT s FROM Subtask s WHERE s.todo.id = :todoId AND s.completed = false ORDER BY s.createdAt ASC")
    List<Subtask> findActiveSubtasksByTodoId(@Param("todoId") Long todoId);
    @Query("SELECT s FROM Subtask s WHERE s.todo.id = :todoId AND s.completed = true ORDER BY s.createdAt ASC")
    List<Subtask> findCompletedSubtasksByTodoId(@Param("todoId") Long todoId);

    @Query("SELECT s FROM Subtask s WHERE s.todo.user.id = :userId ORDER BY s.createdAt ASC")
    List<Subtask> findAllSubtasksByUserId(@Param("userId") Long userId);
    @Query("SELECT s FROM Subtask s WHERE s.todo.user.id = :userId AND s.completed = false ORDER BY s.createdAt ASC")
    List<Subtask> findAllIncompleteSubtasksByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Subtask s WHERE s.todo.id = :todoId " +
            "AND LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY s.createdAt ASC")
    List<Subtask> searchByTitleInTodo(@Param("todoId") Long todoId, @Param("keyword") String keyword);
    @Query("SELECT s FROM Subtask s WHERE s.todo.user.id = :userId " +
            "AND LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY s.createdAt ASC")
    List<Subtask> searchByTitleForUser(@Param("userId") Long userId, @Param("keyword") String keyword);

    List<Subtask> findByTodoIdAndCreatedAtBetween(Long todoId, LocalDateTime start, LocalDateTime end);

    long countByTodoId(Long todoId);
    long countByTodoIdAndCompleted(Long todoId, boolean completed);
    @Query("SELECT COUNT(s) FROM Subtask s WHERE s.todo.user.id = :userId")
    long countAllSubtasksByUserId(@Param("userId") Long userId);
    @Query("SELECT COUNT(s) FROM Subtask s WHERE s.todo.user.id = :userId AND s.completed = false")
    long countIncompleteSubtasksByUserId(@Param("userId") Long userId);

}
