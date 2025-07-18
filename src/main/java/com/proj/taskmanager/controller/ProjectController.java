package com.proj.taskmanager.controller;

import com.proj.taskmanager.model.Project;
import com.proj.taskmanager.request.project.AddMemberReq;
import com.proj.taskmanager.request.project.ChangeMemberRoleReq;
import com.proj.taskmanager.request.project.CreateProjectReq;
import com.proj.taskmanager.response.ApiResponse;
import com.proj.taskmanager.security.JwtUtil;
import com.proj.taskmanager.service.project.IProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/project")
@Tag(name = "Project Controller", description = "APIs related to Project management")
public class ProjectController {
    private final IProjectService projectService;
    private final JwtUtil jwtUtil;

    @PostMapping("/create")
    @Operation(summary = "Create a new project", description = "Creates a new project and assigns the creator as the owner")
    public ResponseEntity<ApiResponse> createProject(
            @Valid @RequestBody CreateProjectReq request,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                Project project = projectService.createProject(userId, request);

                return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Project created successfully!", project));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to create project: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @GetMapping("/user")
    @Operation(summary = "Get all projects for user", description = "Retrieves all projects where the user is either the creator or a member")
    public ResponseEntity<ApiResponse> getAllProjectsByUser(
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                List<Project> projects = projectService.getAllProjectsByUserId(userId);

                return ResponseEntity.ok(new ApiResponse("Projects retrieved successfully!", projects));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to retrieve projects: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @PostMapping("/{projectId}/members")
    @Operation(summary = "Add member to project", description = "Adds a new member to the project with specified role. Only project owners and editors can add members.")
    public ResponseEntity<ApiResponse> addMemberToProject(
            @PathVariable Long projectId,
            @Valid @RequestBody AddMemberReq request,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                projectService.addMemberToProject(userId, projectId, request.projectMemberId(), request.projectRole());

                return ResponseEntity.ok(new ApiResponse("Member added to project successfully!", null));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to add member to project: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    @Operation(summary = "Remove member from project", description = "Removes a member from the project. Cannot remove project owner or yourself.")
    public ResponseEntity<ApiResponse> removeMemberFromProject(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                projectService.removeMemberFromProject(userId, projectId, memberId);

                return ResponseEntity.ok(new ApiResponse("Member removed from project successfully!", null));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to remove member from project: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @PutMapping("/{projectId}/members/{memberId}/role")
    @Operation(summary = "Change member role", description = "Changes the role of a project member. Cannot change project owner's role.")
    public ResponseEntity<ApiResponse> changeMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @Valid @RequestBody ChangeMemberRoleReq request,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                projectService.changeMemberRole(userId, projectId, memberId, request.projectRole());

                return ResponseEntity.ok(new ApiResponse("Member role changed successfully!", null));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to change member role: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete project", description = "Deletes a project. Only project owners and editors can delete projects.")
    public ResponseEntity<ApiResponse> deleteProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                projectService.deleteProject(userId, projectId);

                return ResponseEntity.ok(new ApiResponse("Project deleted successfully!", null));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to delete project: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }
}
