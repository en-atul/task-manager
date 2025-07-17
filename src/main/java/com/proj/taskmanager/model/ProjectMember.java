package com.proj.taskmanager.model;

import com.proj.taskmanager.enums.ProjectRole;
import jakarta.persistence.*;

@Entity
@Table(name = "project_members")
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Project project;

    @Enumerated(EnumType.STRING)
    private ProjectRole role;
}