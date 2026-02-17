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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
public class TodoRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TodoRepository todoRepository;

    private User savedUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setPasswordHash("hash");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        savedUser = entityManager.persistAndFlush(user);

        User other = new User();
        other.setUsername("jane_doe");
        other.setEmail("jane@example.com");
        other.setPasswordHash("hash");
        other.setCreatedAt(LocalDateTime.now());
        other.setUpdatedAt(LocalDateTime.now());
        otherUser = entityManager.persistAndFlush(other);
    }

    private Todo createTodo(String title, boolean completed, User user) {
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCompleted(completed);
        todo.setUser(user);
        todo.setCreatedAt(LocalDateTime.now());
        todo.setUpdatedAt(LocalDateTime.now());
        return entityManager.persistAndFlush(todo);
    }

    @Nested
    @DisplayName("findByIdAndUserId()")
    class OwnershipTests {

        @Test
        @DisplayName("should find todo owned by user")
        void findByIdAndUserId_Owned() {
            Todo todo = createTodo("My task", false, savedUser);

            Optional<Todo> result = todoRepository.findByIdAndUserId(todo.getId(), savedUser.getId());

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("should not find todo owned by another user")
        void findByIdAndUserId_NotOwned() {
            Todo todo = createTodo("Other's task", false, otherUser);

            Optional<Todo> result = todoRepository.findByIdAndUserId(todo.getId(), savedUser.getId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Filtered Queries")
    class FilterTests {

        @Test
        @DisplayName("findActiveTodosByUserId should return only incomplete")
        void findActive() {
            createTodo("Active", false, savedUser);
            createTodo("Done", true, savedUser);

            List<Todo> result = todoRepository.findActiveTodosByUserId(savedUser.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Active");
        }

        @Test
        @DisplayName("findCompletedTodosByUserId should return only completed")
        void findCompleted() {
            createTodo("Active", false, savedUser);
            createTodo("Done", true, savedUser);

            List<Todo> result = todoRepository.findCompletedTodosByUserId(savedUser.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Done");
        }

        @Test
        @DisplayName("should not return todos from other users")
        void filterIsolation() {
            createTodo("Mine", false, savedUser);
            createTodo("Theirs", false, otherUser);

            List<Todo> result = todoRepository.findByUserIdOrderByCreatedAtDesc(savedUser.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Mine");
        }
    }

    @Nested
    @DisplayName("searchByKeyword()")
    class SearchTests {

        @Test
        @DisplayName("should find by title keyword")
        void searchByTitle() {
            createTodo("Buy groceries", false, savedUser);
            createTodo("Clean house", false, savedUser);

            List<Todo> result = todoRepository.searchByKeyword(savedUser.getId(), "groceries");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Buy groceries");
        }

        @Test
        @DisplayName("should be case-insensitive")
        void searchCaseInsensitive() {
            createTodo("Buy GROCERIES", false, savedUser);

            List<Todo> result = todoRepository.searchByKeyword(savedUser.getId(), "groceries");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should return empty for no match")
        void searchNoMatch() {
            createTodo("Buy groceries", false, savedUser);

            List<Todo> result = todoRepository.searchByKeyword(savedUser.getId(), "xyz");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not return other user's todos")
        void searchIsolation() {
            createTodo("Buy groceries", false, savedUser);
            createTodo("Buy groceries", false, otherUser);

            List<Todo> result = todoRepository.searchByKeyword(savedUser.getId(), "groceries");

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Counting")
    class CountTests {

        @Test
        @DisplayName("countByUserId should count all user's todos")
        void countAll() {
            createTodo("Task 1", false, savedUser);
            createTodo("Task 2", true, savedUser);

            assertThat(todoRepository.countByUserId(savedUser.getId())).isEqualTo(2);
        }

        @Test
        @DisplayName("countByUserIdAndCompleted should filter by status")
        void countByStatus() {
            createTodo("Active", false, savedUser);
            createTodo("Done", true, savedUser);

            assertThat(todoRepository.countByUserIdAndCompleted(savedUser.getId(), true)).isEqualTo(1);
            assertThat(todoRepository.countByUserIdAndCompleted(savedUser.getId(), false)).isEqualTo(1);
        }
    }
}
