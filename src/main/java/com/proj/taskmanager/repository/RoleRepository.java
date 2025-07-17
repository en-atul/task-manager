package com.proj.taskmanager.repository;

import com.proj.taskmanager.enums.UserRole;
import com.proj.taskmanager.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(UserRole role);
}