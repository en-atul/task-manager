package com.proj.taskmanager.request.project;

import jakarta.validation.constraints.NotEmpty;

public record CreateProjectReq(
        @NotEmpty String name
) {
}
