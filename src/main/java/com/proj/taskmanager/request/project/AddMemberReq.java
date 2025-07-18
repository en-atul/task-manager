package com.proj.taskmanager.request.project;

import com.proj.taskmanager.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record AddMemberReq(
        @NotNull Long projectMemberId,
        @NotNull ProjectRole projectRole
) {
} 