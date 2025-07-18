package com.proj.taskmanager.service.task;

import com.proj.taskmanager.enums.ProjectRole;
import com.proj.taskmanager.enums.TaskStatus;
import com.proj.taskmanager.model.Project;
import com.proj.taskmanager.model.ProjectMember;
import com.proj.taskmanager.model.Task;
import com.proj.taskmanager.model.User;
import com.proj.taskmanager.repository.ProjectMemberRepository;
import com.proj.taskmanager.repository.ProjectRepository;
import com.proj.taskmanager.repository.TaskRepository;
import com.proj.taskmanager.request.task.CreateTaskReq;
import com.proj.taskmanager.request.task.UpdateTaskReq;
import com.proj.taskmanager.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService implements ITaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final IUserService userService;


    @Override
    public Task createTask(Long userId, CreateTaskReq request) {
        Project project = getProjectById(request.projectId());
        validateUserCanManageProject(userId, project, "add task in");

        User user = userService.getUserById(userId);

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(TaskStatus.PENDING);
        task.setProject(project);
        task.setCreatedBy(user);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        if (request.assigneeId() != null) {
            User assignee = userService.getUserById(request.assigneeId());
            validateUserIsProjectMember(assignee.getId(), project, "assign task to");
            task.setAssignee(assignee);
        }

        if(request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        taskRepository.save(task);
        return task;
    }

    @Override
    public Task getTaskById(Long taskId) {
        return findTaskById(taskId);
    }

    @Override
    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = findTaskById(taskId);
        validateUserCanManageTask(userId, task, "delete");

        taskRepository.deleteById(taskId);
    }

    @Override
    @Transactional
    public Task updateTask(Long userId, Long taskId, UpdateTaskReq request) {
        Task task = findTaskById(taskId);
        validateUserCanManageTask(userId, task, "update");

        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.assigneeId() != null) {
            User assignee = userService.getUserById(request.assigneeId());
            validateUserIsProjectMember(assignee.getId(), task.getProject(), "assign task to");
            task.setAssignee(assignee);
        }

        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task assignTask(Long userId, Long taskId, Long assigneeId) {
        Task task = findTaskById(taskId);
        validateUserCanManageTask(userId, task, "assign");

        User assignee = userService.getUserById(assigneeId);
        validateUserIsProjectMember(assignee.getId(), task.getProject(), "assign task to");
        
        task.setAssignee(assignee);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task updateTaskDueDate(Long userId, Long taskId, LocalDate dueDate) {
        Task task = findTaskById(taskId);
        validateUserCanManageTask(userId, task, "update due date for");

        task.setDueDate(dueDate);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Override
    public List<Task> getAllTaskByProjectId(Long projectId) {
        Project project = getProjectById(projectId);
        return taskRepository.findByProjectId(projectId);
    }

    @Override
    public List<Task> getAllTaskByUserId(Long projectId, Long userId) {
        Project project = getProjectById(projectId);
        return taskRepository.findByProjectIdAndAssigneeId(projectId, userId);
    }

    // Helper methods
    private Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

    private Task findTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }

    private void validateUserCanManageProject(Long userId, Project project, String action) {
        boolean isOwner = project.getCreatedBy().getId().equals(userId);

        if (!isOwner) {
            Optional<ProjectMember> userMembership = projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId);

            if (userMembership.isEmpty() || userMembership.get().getRole() != ProjectRole.EDITOR) {
                throw new RuntimeException("Only project owner or editor can " + action + " the project");
            }
        }
    }

    private void validateUserCanManageTask(Long userId, Task task, String action) {
        Project project = task.getProject();
        boolean isOwner = project.getCreatedBy().getId().equals(userId);

        if (!isOwner) {
            Optional<ProjectMember> userMembership = projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId);

            if (userMembership.isEmpty() || userMembership.get().getRole() != ProjectRole.EDITOR) {
                throw new RuntimeException("Only project owner or editor can " + action + " the task");
            }
        }
    }

    private void validateUserIsProjectMember(Long userId, Project project, String action) {
        boolean isOwner = project.getCreatedBy().getId().equals(userId);

        if (!isOwner) {
            Optional<ProjectMember> userMembership = projectMemberRepository.findByProjectIdAndUserId(project.getId(), userId);

            if (userMembership.isEmpty()) {
                throw new RuntimeException("User must be a project member to " + action + " the project");
            }
        }
    }
}
