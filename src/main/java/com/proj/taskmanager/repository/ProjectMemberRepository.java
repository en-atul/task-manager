package com.proj.taskmanager.repository;

import com.proj.taskmanager.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    
    List<ProjectMember> findByProjectId(Long projectId);
    
    List<ProjectMember> findByUserId(Long userId);
    
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
    
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
} 