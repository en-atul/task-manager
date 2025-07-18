package com.proj.taskmanager.model;


import com.proj.taskmanager.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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

    @ManyToOne
    private User assignee;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
