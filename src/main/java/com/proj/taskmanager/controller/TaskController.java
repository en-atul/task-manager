package com.proj.taskmanager.controller;

import com.proj.taskmanager.model.Task;
import com.proj.taskmanager.request.task.CreateTaskReq;
import com.proj.taskmanager.request.task.UpdateTaskReq;
import com.proj.taskmanager.response.ApiResponse;
import com.proj.taskmanager.security.JwtUtil;
import com.proj.taskmanager.service.task.ITaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/task")
@Tag(name = "Task Controller", description = "APIs related to Task management")
public class TaskController {
    private final ITaskService taskService;
    private final JwtUtil jwtUtil;

    @PostMapping("/create")
    @Operation(summary = "Create a new task", description = "Creates a new task in a project. Can optionally assign the task and set a due date.")
    public ResponseEntity<ApiResponse> createTask(
            @Valid @RequestBody CreateTaskReq request,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                Task task = taskService.createTask(userId, request);

                return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Task created successfully!", task));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to create task: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID", description = "Retrieves a specific task by its ID")
    public ResponseEntity<ApiResponse> getTask(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                Task task = taskService.getTaskById(taskId);

                return ResponseEntity.ok(new ApiResponse("Task retrieved successfully!", task));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to retrieve task: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all tasks in project", description = "Retrieves all tasks in a specific project")
    public ResponseEntity<ApiResponse> getAllTasksByProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                List<Task> tasks = taskService.getAllTaskByProjectId(projectId);

                return ResponseEntity.ok(new ApiResponse("Tasks retrieved successfully!", tasks));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to retrieve tasks: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @GetMapping("/project/{projectId}/user/{userId}")
    @Operation(summary = "Get tasks assigned to user", description = "Retrieves all tasks assigned to a specific user in a project")
    public ResponseEntity<ApiResponse> getAllTasksByUser(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long currentUserId = jwtUtil.extractUserId(token);

                List<Task> tasks = taskService.getAllTaskByUserId(projectId, userId);

                return ResponseEntity.ok(new ApiResponse("User tasks retrieved successfully!", tasks));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to retrieve user tasks: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update task", description = "Updates task properties including title, description, due date, and assignee")
    public ResponseEntity<ApiResponse> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskReq request,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                Task task = taskService.updateTask(userId, taskId, request);

                return ResponseEntity.ok(new ApiResponse("Task updated successfully!", task));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to update task: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @PutMapping("/{taskId}/assign/{assigneeId}")
    @Operation(summary = "Assign task to user", description = "Assigns a task to a specific user. The assignee must be a project member.")
    public ResponseEntity<ApiResponse> assignTask(
            @PathVariable Long taskId,
            @PathVariable Long assigneeId,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                Task task = taskService.assignTask(userId, taskId, assigneeId);

                return ResponseEntity.ok(new ApiResponse("Task assigned successfully!", task));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to assign task: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @PutMapping("/{taskId}/due-date")
    @Operation(summary = "Update task due date", description = "Updates the due date of a specific task")
    public ResponseEntity<ApiResponse> updateTaskDueDate(
            @PathVariable Long taskId,
            @RequestParam LocalDate dueDate,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                Task task = taskService.updateTaskDueDate(userId, taskId, dueDate);

                return ResponseEntity.ok(new ApiResponse("Task due date updated successfully!", task));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to update task due date: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete task", description = "Deletes a specific task. Only project owners and editors can delete tasks.")
    public ResponseEntity<ApiResponse> deleteTask(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Long userId = jwtUtil.extractUserId(token);

                taskService.deleteTask(userId, taskId);

                return ResponseEntity.ok(new ApiResponse("Task deleted successfully!", null));
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage(), null));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to delete task: " + e.getMessage(), null));
            }
        }
        return ResponseEntity.badRequest().body(new ApiResponse("Authorization header is required", null));
    }
}
