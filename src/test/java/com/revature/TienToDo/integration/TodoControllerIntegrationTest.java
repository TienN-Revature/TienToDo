package com.revature.TienToDo.integration;

import com.revature.TienToDo.entity.Subtask;
import com.revature.TienToDo.entity.Todo;
import com.revature.TienToDo.entity.User;
import com.revature.TienToDo.repository.SubtaskRepository;
import com.revature.TienToDo.repository.TodoRepository;
import com.revature.TienToDo.repository.UserRepository;
import com.revature.TienToDo.utility.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class TodoControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private SubtaskRepository subtaskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private Todo todoA;

    @BeforeEach
    void setUp() {
        subtaskRepository.deleteAll();
        todoRepository.deleteAll();
        userRepository.deleteAll();

        // User A
        userA = new User();
        userA.setUsername("user_a");
        userA.setEmail("a@example.com");
        userA.setPasswordHash(passwordEncoder.encode("Secret123!"));
        userA.setCreatedAt(LocalDateTime.now());
        userA.setUpdatedAt(LocalDateTime.now());
        userA = userRepository.save(userA);
        tokenA = jwtUtil.generateToken("user_a");

        // User B
        userB = new User();
        userB.setUsername("user_b");
        userB.setEmail("b@example.com");
        userB.setPasswordHash(passwordEncoder.encode("Secret123!"));
        userB.setCreatedAt(LocalDateTime.now());
        userB.setUpdatedAt(LocalDateTime.now());
        userB = userRepository.save(userB);
        tokenB = jwtUtil.generateToken("user_b");

        // Pre-existing todo for user A
        todoA = new Todo();
        todoA.setTitle("Buy groceries");
        todoA.setDescription("Milk, eggs, bread");
        todoA.setCompleted(false);
        todoA.setUser(userA);
        todoA.setCreatedAt(LocalDateTime.now());
        todoA.setUpdatedAt(LocalDateTime.now());
        todoA = todoRepository.save(todoA);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ==================== TODO CRUD ====================

    @Nested
    @DisplayName("GET /api/todos")
    class GetAllTodosTests {

        @Test
        @DisplayName("200 — returns user's todos only")
        void getAllTodos_OnlyOwned() throws Exception {
            mockMvc.perform(get("/api/todos")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("Buy groceries"));
        }

        @Test
        @DisplayName("200 — user B sees empty list (not A's todos)")
        void getAllTodos_Isolation() throws Exception {
            mockMvc.perform(get("/api/todos")
                            .header("Authorization", bearer(tokenB)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("401 — no token is rejected")
        void getAllTodos_NoAuth() throws Exception {
            mockMvc.perform(get("/api/todos"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/todos/{todoId}")
    class GetTodoByIdTests {

        @Test
        @DisplayName("200 — owner can access their todo")
        void getTodo_Owner() throws Exception {
            mockMvc.perform(get("/api/todos/" + todoA.getId())
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Buy groceries"))
                    .andExpect(jsonPath("$.description").value("Milk, eggs, bread"))
                    .andExpect(jsonPath("$.completed").value(false));
        }

        @Test
        @DisplayName("404 — other user can't access the todo")
        void getTodo_NotOwner() throws Exception {
            mockMvc.perform(get("/api/todos/" + todoA.getId())
                            .header("Authorization", bearer(tokenB)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("404 — non-existent todo ID")
        void getTodo_NotFound() throws Exception {
            mockMvc.perform(get("/api/todos/99999")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/todos")
    class CreateTodoTests {

        @Test
        @DisplayName("201 — creates todo and returns it")
        void createTodo_Success() throws Exception {
            mockMvc.perform(post("/api/todos")
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "title": "Clean house",
                                    "description": "Vacuum and mop"
                                }
                                """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Clean house"))
                    .andExpect(jsonPath("$.description").value("Vacuum and mop"))
                    .andExpect(jsonPath("$.completed").value(false))
                    .andExpect(jsonPath("$.id").isNumber());

            // Verify it's in the database
            assertThat(todoRepository.findByUserIdOrderByCreatedAtDesc(userA.getId()))
                    .hasSize(2);
        }

        @Test
        @DisplayName("201 — creates todo without description")
        void createTodo_NoDescription() throws Exception {
            mockMvc.perform(post("/api/todos")
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Minimal todo\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Minimal todo"))
                    .andExpect(jsonPath("$.description").isEmpty());
        }
    }

    @Nested
    @DisplayName("PUT /api/todos/{todoId}")
    class UpdateTodoTests {

        @Test
        @DisplayName("200 — partial update changes only provided fields")
        void updateTodo_Partial() throws Exception {
            mockMvc.perform(put("/api/todos/" + todoA.getId())
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Updated title\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated title"))
                    .andExpect(jsonPath("$.description").value("Milk, eggs, bread")); // unchanged
        }

        @Test
        @DisplayName("404 — other user can't update the todo")
        void updateTodo_NotOwner() throws Exception {
            mockMvc.perform(put("/api/todos/" + todoA.getId())
                            .header("Authorization", bearer(tokenB))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Hacked\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/todos/{todoId}/complete")
    class MarkCompleteTests {

        @Test
        @DisplayName("200 — marks todo as completed")
        void markComplete_Success() throws Exception {
            mockMvc.perform(patch("/api/todos/" + todoA.getId() + "/complete")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /api/todos/{todoId}")
    class DeleteTodoTests {

        @Test
        @DisplayName("204 — deletes todo")
        void deleteTodo_Success() throws Exception {
            mockMvc.perform(delete("/api/todos/" + todoA.getId())
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isNoContent());

            assertThat(todoRepository.findById(todoA.getId())).isEmpty();
        }

        @Test
        @DisplayName("404 — other user can't delete the todo")
        void deleteTodo_NotOwner() throws Exception {
            mockMvc.perform(delete("/api/todos/" + todoA.getId())
                            .header("Authorization", bearer(tokenB)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== FILTER & SEARCH ====================

    @Nested
    @DisplayName("Filtering and Search")
    class FilterSearchTests {

        @Test
        @DisplayName("GET /api/todos/search?q=groceries — finds matching todo")
        void search_Match() throws Exception {
            mockMvc.perform(get("/api/todos/search")
                            .param("q", "groceries")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("Buy groceries"));
        }

        @Test
        @DisplayName("GET /api/todos/search?q=xyz — returns empty for no match")
        void search_NoMatch() throws Exception {
            mockMvc.perform(get("/api/todos/search")
                            .param("q", "xyz")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ==================== SUBTASK CRUD ====================

    @Nested
    @DisplayName("Subtask CRUD")
    class SubtaskTests {

        @Test
        @DisplayName("POST + GET — create subtask then retrieve it")
        void createAndGetSubtask() throws Exception {
            // Create subtask
            mockMvc.perform(post("/api/todos/" + todoA.getId() + "/subtasks")
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Buy milk\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Buy milk"))
                    .andExpect(jsonPath("$.completed").value(false));

            // Get subtasks
            mockMvc.perform(get("/api/todos/" + todoA.getId() + "/subtasks")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title").value("Buy milk"));
        }

        @Test
        @DisplayName("PUT — update subtask title")
        void updateSubtask() throws Exception {
            Subtask subtask = createSubtask("Buy milk");

            mockMvc.perform(put("/api/todos/" + todoA.getId() + "/subtasks/" + subtask.getId())
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Buy oat milk\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Buy oat milk"));
        }

        @Test
        @DisplayName("PATCH — mark subtask complete")
        void markSubtaskComplete() throws Exception {
            Subtask subtask = createSubtask("Buy milk");

            mockMvc.perform(patch("/api/todos/" + todoA.getId() + "/subtasks/" + subtask.getId() + "/complete")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true));
        }

        @Test
        @DisplayName("DELETE — delete subtask")
        void deleteSubtask() throws Exception {
            Subtask subtask = createSubtask("Buy milk");

            mockMvc.perform(delete("/api/todos/" + todoA.getId() + "/subtasks/" + subtask.getId())
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isNoContent());

            assertThat(subtaskRepository.findById(subtask.getId())).isEmpty();
        }

        @Test
        @DisplayName("404 — other user can't access subtasks")
        void subtask_NotOwner() throws Exception {
            Subtask subtask = createSubtask("Buy milk");

            mockMvc.perform(get("/api/todos/" + todoA.getId() + "/subtasks")
                            .header("Authorization", bearer(tokenB)))
                    .andExpect(status().isNotFound());
        }
    }


    // ==================== END-TO-END FLOW ====================

    @Nested
    @DisplayName("End-to-End Todo Flow")
    class E2ETests {

        @Test
        @DisplayName("create todo → add subtasks → complete subtasks → complete todo → verify")
        void fullTodoFlow() throws Exception {
            // 1. Create todo
            MvcResult todoResult = mockMvc.perform(post("/api/todos")
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Plan trip\", \"description\": \"Summer vacation\"}"))
                    .andExpect(status().isCreated())
                    .andReturn();

            String todoBody = todoResult.getResponse().getContentAsString();
            String todoId = extractJsonNumber(todoBody, "id");

            // 2. Add subtasks
            mockMvc.perform(post("/api/todos/" + todoId + "/subtasks")
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Book flights\"}"))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/todos/" + todoId + "/subtasks")
                            .header("Authorization", bearer(tokenA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Reserve hotel\"}"))
                    .andExpect(status().isCreated());

            // 3. Verify subtasks exist
            mockMvc.perform(get("/api/todos/" + todoId + "/subtasks")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            // 4. Complete all subtasks
            mockMvc.perform(patch("/api/todos/" + todoId + "/subtasks/complete-all")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updatedCount").value(2));

            // 5. Complete the todo
            mockMvc.perform(patch("/api/todos/" + todoId + "/complete")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true));

            // 6. Verify it shows in completed
            mockMvc.perform(get("/api/todos/completed")
                            .header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.title == 'Plan trip')]").exists());
        }
    }

    // ==================== HELPERS ====================

    private Subtask createSubtask(String title) {
        Subtask subtask = new Subtask();
        subtask.setTitle(title);
        subtask.setCompleted(false);
        subtask.setTodo(todoA);
        subtask.setCreatedAt(LocalDateTime.now());
        subtask.setUpdatedAt(LocalDateTime.now());
        return subtaskRepository.save(subtask);
    }

    private String extractJsonNumber(String json, String key) {
        int start = json.indexOf("\"" + key + "\":") + key.length() + 3;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
            end++;
        }
        return json.substring(start, end);
    }
}
