package com.revature.TienToDo.repository;

import com.revature.TienToDo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.user.id = :userId")
    long countTodosByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.user.id = :userId AND t.completed = true")
    long countCompletedTodosByUserId(@Param("userId") Long userId);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByUsername(@Param("keyword") String keyword);

    List<User> findByCreatedAtAfter(LocalDateTime date);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.updatedAt = :now WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId,
                        @Param("passwordHash") String passwordHash,
                        @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.email = :email, u.updatedAt = :now WHERE u.id = :userId")
    void updateEmail(@Param("userId") Long userId,
                     @Param("email") String email,
                     @Param("now") LocalDateTime now);

}
