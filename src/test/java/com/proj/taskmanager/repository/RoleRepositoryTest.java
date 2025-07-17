package com.proj.taskmanager.repository;

import com.proj.taskmanager.enums.UserRole;
import com.proj.taskmanager.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        entityManager.clear();
        
        // Create test roles
        adminRole = new Role(UserRole.ADMIN);
        userRole = new Role(UserRole.USER);
    }

    @Test
    void findByName_WhenRoleExists_ShouldReturnRole() {
        // Given
        entityManager.persistAndFlush(adminRole);

        // When
        Optional<Role> foundRole = roleRepository.findByName(UserRole.ADMIN);

        // Then
        assertTrue(foundRole.isPresent());
        assertEquals(UserRole.ADMIN, foundRole.get().getName());
        assertEquals(adminRole.getId(), foundRole.get().getId());
    }

    @Test
    void findByName_WhenRoleDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Role> foundRole = roleRepository.findByName(UserRole.ADMIN);

        // Then
        assertFalse(foundRole.isPresent());
    }

    @Test
    void save_ShouldPersistRole() {
        // When
        Role savedRole = roleRepository.save(adminRole);

        // Then
        assertNotNull(savedRole.getId());
        assertEquals(UserRole.ADMIN, savedRole.getName());
        
        // Verify it's actually persisted
        Role foundRole = entityManager.find(Role.class, savedRole.getId());
        assertNotNull(foundRole);
        assertEquals(UserRole.ADMIN, foundRole.getName());
    }

    @Test
    void findById_WhenRoleExists_ShouldReturnRole() {
        // Given
        entityManager.persistAndFlush(adminRole);

        // When
        Optional<Role> foundRole = roleRepository.findById(adminRole.getId());

        // Then
        assertTrue(foundRole.isPresent());
        assertEquals(UserRole.ADMIN, foundRole.get().getName());
    }

    @Test
    void findById_WhenRoleDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<Role> foundRole = roleRepository.findById(999L);

        // Then
        assertFalse(foundRole.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllRoles() {
        // Given
        entityManager.persistAndFlush(adminRole);
        entityManager.persistAndFlush(userRole);

        // When
        List<Role> allRoles = roleRepository.findAll();

        // Then
        assertEquals(2, allRoles.size());
        assertTrue(allRoles.stream().anyMatch(role -> role.getName() == UserRole.ADMIN));
        assertTrue(allRoles.stream().anyMatch(role -> role.getName() == UserRole.USER));
    }

    @Test
    void delete_ShouldRemoveRole() {
        // Given
        entityManager.persistAndFlush(adminRole);
        Long roleId = adminRole.getId();

        // When
        roleRepository.delete(adminRole);
        entityManager.flush();

        // Then
        Role deletedRole = entityManager.find(Role.class, roleId);
        assertNull(deletedRole);
    }

    @Test
    void deleteById_ShouldRemoveRole() {
        // Given
        entityManager.persistAndFlush(adminRole);
        Long roleId = adminRole.getId();

        // When
        roleRepository.deleteById(roleId);
        entityManager.flush();

        // Then
        Role deletedRole = entityManager.find(Role.class, roleId);
        assertNull(deletedRole);
    }

    @Test
    void existsById_WhenRoleExists_ShouldReturnTrue() {
        // Given
        entityManager.persistAndFlush(adminRole);

        // When
        boolean exists = roleRepository.existsById(adminRole.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WhenRoleDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = roleRepository.existsById(999L);

        // Then
        assertFalse(exists);
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // Given
        entityManager.persistAndFlush(adminRole);
        entityManager.persistAndFlush(userRole);

        // When
        long count = roleRepository.count();

        // Then
        assertEquals(2, count);
    }
}