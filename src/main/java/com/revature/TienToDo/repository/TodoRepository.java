package com.revature.TienToDo.repository;

import com.revature.TienToDo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Todo> findByIdAndUserId(Long id, Long userId);
    List<Todo> findByUserIdAndCompletedOrderByCreatedAtDesc(Long userId, boolean completed);

    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId AND t.completed = false ORDER BY t.createdAt DESC")
    List<Todo> findActiveTodosByUserId(@Param("userId") Long userId);
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId AND t.completed = true ORDER BY t.createdAt DESC")
    List<Todo> findCompletedTodosByUserId(@Param("userId") Long userId);
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId " +
            "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY t.createdAt DESC")
    List<Todo> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId " +
            "AND LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY t.createdAt DESC")
    List<Todo> searchByTitle(@Param("userId") Long userId, @Param("keyword") String keyword);
    List<Todo> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
    List<Todo> findByUserIdAndUpdatedAtAfterOrderByUpdatedAtDesc(Long userId, LocalDateTime since);

    long countByUserId(Long userId);
    long countByUserIdAndCompleted(Long userId, boolean completed);
    @Query("SELECT DISTINCT t FROM Todo t JOIN t.subtasks s " +
            "WHERE t.user.id = :userId AND s.completed = false " +
            "ORDER BY t.createdAt DESC")
    List<Todo> findTodosWithIncompleteSubtasks(@Param("userId") Long userId);
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId AND t.subtasks IS EMPTY " +
            "ORDER BY t.createdAt DESC")
    List<Todo> findTodosWithoutSubtasks(@Param("userId") Long userId);

}
