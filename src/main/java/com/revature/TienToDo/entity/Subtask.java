package com.revature.TienToDo.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity mapping for the "subtasks" table.
 *
 * SQLite schema:
 *   CREATE TABLE subtasks (
 *       id         INTEGER PRIMARY KEY AUTOINCREMENT,
 *       todo_id    INTEGER NOT NULL,
 *       title      TEXT NOT NULL,
 *       completed  INTEGER NOT NULL DEFAULT 0,
 *       created_at TEXT NOT NULL DEFAULT (datetime('now')),
 *       updated_at TEXT NOT NULL DEFAULT (datetime('now')),
 *       FOREIGN KEY (todo_id) REFERENCES todos(id) ON DELETE CASCADE
 *   );
 *
 * Column type notes:
 *   - id:         INTEGER → Long with IDENTITY generation
 *   - todo_id:    INTEGER → FK to todos.id via @ManyToOne
 *   - title:      TEXT    → String
 *   - completed:  INTEGER → boolean (0 = false, 1 = true)
 *   - created_at: TEXT    → LocalDateTime (via SQLiteLocalDateTimeConverter)
 *   - updated_at: TEXT    → LocalDateTime (via SQLiteLocalDateTimeConverter)
 */
@Entity
@Table(name = "subtasks")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Subtask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INTEGER")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "todo_id", nullable = false, columnDefinition = "INTEGER",
            foreignKey = @ForeignKey(name = "fk_subtasks_todo_id"))
    @JsonIgnore
    private Todo todo;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "completed", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private boolean completed = false;

    @Column(name = "created_at", nullable = false, columnDefinition = "TEXT")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false, columnDefinition = "TEXT")
    private LocalDateTime updatedAt = LocalDateTime.now();

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
