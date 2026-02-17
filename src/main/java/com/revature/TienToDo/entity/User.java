package com.revature.TienToDo.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping for the "users" table.
 *
 * SQLite schema:
 *   CREATE TABLE users (
 *       id           INTEGER PRIMARY KEY AUTOINCREMENT,
 *       username     TEXT NOT NULL UNIQUE,
 *       email        TEXT NOT NULL UNIQUE,
 *       password_hash TEXT NOT NULL,
 *       created_at   TEXT NOT NULL DEFAULT (datetime('now')),
 *       updated_at   TEXT NOT NULL DEFAULT (datetime('now'))
 *   );
 *
 * Column type notes:
 *   - id:            INTEGER → Long with IDENTITY generation
 *   - username:      TEXT    → String
 *   - email:         TEXT    → String
 *   - password_hash: TEXT    → String (BCrypt hash)
 *   - created_at:    TEXT    → LocalDateTime (via SQLiteLocalDateTimeConverter)
 *   - updated_at:    TEXT    → LocalDateTime (via SQLiteLocalDateTimeConverter)
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, unique = true, columnDefinition = "TEXT")
    private String username;

    @Column(name = "email", nullable = false, unique = true, columnDefinition = "TEXT")
    private String email;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "created_at", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = com.revature.TienToDo.config.SQLiteLocalDateTimeConverter.class)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = com.revature.TienToDo.config.SQLiteLocalDateTimeConverter.class)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Todo> todos = new ArrayList<>();

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
