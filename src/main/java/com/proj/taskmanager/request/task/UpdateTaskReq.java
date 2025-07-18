package com.proj.taskmanager.request.task;

import java.time.LocalDate;

public record UpdateTaskReq(
        String title,
        String description,
        LocalDate dueDate,
        Long assigneeId
) {
} 