package com.proj.taskmanager.repository;

import com.proj.taskmanager.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findById(Long id);
    
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.members m WHERE p.createdBy.id = :userId OR m.user.id = :userId")
    List<Project> findProjectsByUserId(@Param("userId") Long userId);
}
