package com.revature.TienToDo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping for the "todos" table.
 *
 * SQLite schema:
 *   CREATE TABLE todos (
 *       id          INTEGER PRIMARY KEY AUTOINCREMENT,
 *       user_id     INTEGER NOT NULL,
 *       title       TEXT NOT NULL,
 *       description TEXT,
 *       completed   INTEGER NOT NULL DEFAULT 0,
 *       created_at  TEXT NOT NULL DEFAULT (datetime('now')),
 *       updated_at  TEXT NOT NULL DEFAULT (datetime('now')),
 *       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
 *   );
 *
 * Column type notes:
 *   - id:          INTEGER → Long with IDENTITY generation
 *   - user_id:     INTEGER → FK to users.id via @ManyToOne
 *   - title:       TEXT    → String
 *   - description: TEXT    → String (nullable)
 *   - completed:   INTEGER → boolean (0 = false, 1 = true)
 *   - created_at:  TEXT    → LocalDateTime (via SQLiteLocalDateTimeConverter)
 *   - updated_at:  TEXT    → LocalDateTime (via SQLiteLocalDateTimeConverter)
 */
@Entity
@Table(name = "todos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INTEGER")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "INTEGER",
            foreignKey = @ForeignKey(name = "fk_todos_user_id"))
    @JsonIgnore
    private User user;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "completed", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private boolean completed = false;

    @Column(name = "created_at", nullable = false, columnDefinition = "TEXT")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false, columnDefinition = "TEXT")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Subtask> subtasks = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
