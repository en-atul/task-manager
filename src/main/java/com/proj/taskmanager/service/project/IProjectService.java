package com.proj.taskmanager.service.project;

import com.proj.taskmanager.enums.ProjectRole;
import com.proj.taskmanager.model.Project;
import com.proj.taskmanager.request.project.CreateProjectReq;

import java.util.List;

public interface IProjectService {

    Project createProject(Long userId, CreateProjectReq request);
    
    List<Project> getAllProjectsByUserId(Long userId);
    
    void deleteProject(Long userId, Long projectId);
    
    void addMemberToProject(Long userId, Long projectId, Long projectMemberId, ProjectRole projectRole);
    
    void removeMemberFromProject(Long userId, Long projectId, Long projectMemberId);
    
    void changeMemberRole(Long userId, Long projectId, Long projectMemberId, ProjectRole projectRole);

}
