package com.proj.taskmanager.model;


import com.proj.taskmanager.enums.TaskStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;

    private LocalDate dueDate;

    @ManyToOne
    private Project project;

    @ManyToOne
    private User createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
