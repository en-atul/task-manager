package com.proj.taskmanager.service.project;

import com.proj.taskmanager.enums.ProjectRole;
import com.proj.taskmanager.model.Project;
import com.proj.taskmanager.model.ProjectMember;
import com.proj.taskmanager.model.User;
import com.proj.taskmanager.repository.ProjectMemberRepository;
import com.proj.taskmanager.repository.ProjectRepository;
import com.proj.taskmanager.request.project.CreateProjectReq;
import com.proj.taskmanager.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService implements IProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final IUserService userService;

    @Override
    @Transactional
    public Project createProject(Long userId, CreateProjectReq request) {
        User user = userService.getUserById(userId);
        
        Project project = new Project();
        project.setName(request.name());
        project.setCreatedBy(user);
        project.setCreatedAt(LocalDateTime.now());
        
        project = projectRepository.save(project);
        
        ProjectMember projectMember = new ProjectMember();
        projectMember.setUser(user);
        projectMember.setProject(project);
        projectMember.setRole(ProjectRole.OWNER);
        
        projectMemberRepository.save(projectMember);
        
        return project;
    }

    @Override
    public List<Project> getAllProjectsByUserId(Long userId) {
        return projectRepository.findProjectsByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        Project project = getProjectById(projectId);
        validateUserCanManageProject(userId, project, "delete");
        
        projectRepository.deleteById(projectId);
    }

    @Override
    @Transactional
    public void addMemberToProject(Long userId, Long projectId, Long projectMemberId, ProjectRole projectRole) {
        Project project = getProjectById(projectId);
        validateUserCanManageProject(userId, project, "add member to");
        
        User userToAdd = userService.getUserById(projectMemberId);
        
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, projectMemberId)) {
            throw new RuntimeException("User is already a member of this project");
        }
        
        ProjectMember projectMember = new ProjectMember();
        projectMember.setUser(userToAdd);
        projectMember.setProject(project);
        projectMember.setRole(projectRole);
        
        projectMemberRepository.save(projectMember);
    }

    @Override
    @Transactional
    public void removeMemberFromProject(Long userId, Long projectId, Long projectMemberId) {
        Project project = getProjectById(projectId);
        validateUserCanManageProject(userId, project, "remove member from");
        
        if (project.getCreatedBy().getId().equals(projectMemberId)) {
            throw new RuntimeException("Cannot remove project owner from the project");
        }
        
        if (userId.equals(projectMemberId)) {
            throw new RuntimeException("Cannot remove yourself from the project");
        }
        
        Optional<ProjectMember> projectMember = projectMemberRepository.findByProjectIdAndUserId(projectId, projectMemberId);
        if (projectMember.isEmpty()) {
            throw new RuntimeException("User is not a member of this project");
        }
        
        projectMemberRepository.deleteById(projectMember.get().getId());
    }

    @Override
    @Transactional
    public void changeMemberRole(Long userId, Long projectId, Long projectMemberId, ProjectRole projectRole) {
        Project project = getProjectById(projectId);
        validateUserCanManageProject(userId, project, "update member's role in");
        
        if (project.getCreatedBy().getId().equals(projectMemberId)) {
            throw new RuntimeException("Cannot change project owner's role");
        }
        
        Optional<ProjectMember> projectMember = projectMemberRepository.findByProjectIdAndUserId(projectId, projectMemberId);
        if (projectMember.isEmpty()) {
            throw new RuntimeException("User is not a member of this project");
        }
        
        projectMember.get().setRole(projectRole);
        projectMemberRepository.save(projectMember.get());
    }

    // Helper methods
    private Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
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
}
