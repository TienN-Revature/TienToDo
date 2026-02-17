package com.revature.TienToDo.repository;


import com.revature.TienToDo.entity.Todo;
import com.revature.TienToDo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
public class UserRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TodoRepository todoRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setPasswordHash("$2a$10$hash123");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        savedUser = entityManager.persistAndFlush(user);
    }

    @Nested
    @DisplayName("Lookup Methods")
    class LookupTests {

        @Test
        @DisplayName("findByUsername should return user when exists")
        void findByUsername_Found() {
            Optional<User> result = userRepository.findByUsername("john_doe");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("findByUsername should return empty when not exists")
        void findByUsername_NotFound() {
            Optional<User> result = userRepository.findByUsername("ghost");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findByEmail should return user when exists")
        void findByEmail_Found() {
            Optional<User> result = userRepository.findByEmail("john@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("john_doe");
        }
    }

    @Nested
    @DisplayName("Existence Checks")
    class ExistenceTests {

        @Test
        @DisplayName("existsByUsername should return true for existing username")
        void existsByUsername_True() {
            assertThat(userRepository.existsByUsername("john_doe")).isTrue();
        }

        @Test
        @DisplayName("existsByUsername should return false for non-existing username")
        void existsByUsername_False() {
            assertThat(userRepository.existsByUsername("ghost")).isFalse();
        }

        @Test
        @DisplayName("existsByEmail should return true for existing email")
        void existsByEmail_True() {
            assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
        }

        @Test
        @DisplayName("existsByEmail should return false for non-existing email")
        void existsByEmail_False() {
            assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("Aggregate Queries")
    class AggregateTests {

        @Test
        @DisplayName("countTodosByUserId should count all todos")
        void countTodos() {
            createTodo("Task 1", false);
            createTodo("Task 2", true);

            long count = userRepository.countTodosByUserId(savedUser.getId());

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("countCompletedTodosByUserId should count only completed")
        void countCompletedTodos() {
            createTodo("Active", false);
            createTodo("Done", true);

            long count = userRepository.countCompletedTodosByUserId(savedUser.getId());

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should return 0 when user has no todos")
        void countTodos_NoTodos() {
            assertThat(userRepository.countTodosByUserId(savedUser.getId())).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateTests {

        @Test
        @DisplayName("updatePassword should change password hash")
        void updatePassword() {
            userRepository.updatePassword(savedUser.getId(), "$2a$10$new_hash", LocalDateTime.now());
            entityManager.clear(); // clear cache to force reload

            User updated = userRepository.findById(savedUser.getId()).orElseThrow();
            assertThat(updated.getPasswordHash()).isEqualTo("$2a$10$new_hash");
        }

        @Test
        @DisplayName("updateEmail should change email")
        void updateEmail() {
            userRepository.updateEmail(savedUser.getId(), "new@example.com", LocalDateTime.now());
            entityManager.clear();

            User updated = userRepository.findById(savedUser.getId()).orElseThrow();
            assertThat(updated.getEmail()).isEqualTo("new@example.com");
        }
    }

    // Helper
    private void createTodo(String title, boolean completed) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(completed);
        todo.setUser(savedUser);
        todo.setCreatedAt(LocalDateTime.now());
        todo.setUpdatedAt(LocalDateTime.now());
        entityManager.persistAndFlush(todo);
    }
}
