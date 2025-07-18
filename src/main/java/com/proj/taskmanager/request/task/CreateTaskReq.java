package com.proj.taskmanager.request.task;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;

public record CreateTaskReq(
        @NotEmpty String title,
        @NotEmpty String description,
        @NotEmpty Long projectId,
        LocalDate dueDate,
        Long assigneeId
        ) {
}
