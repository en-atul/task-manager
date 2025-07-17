package com.proj.taskmanager.repository;

import com.proj.taskmanager.enums.UserRole;
import com.proj.taskmanager.model.Role;
import com.proj.taskmanager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Clear the database before each test
        entityManager.clear();
        
        // Create test role
        userRole = new Role(UserRole.USER);
        entityManager.persistAndFlush(userRole);
        
        // Create test users
        user1 = new User();
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setEmail("john.doe@example.com");
        user1.setPassword("encodedPassword123");
        user1.setRoles(new HashSet<>());
        user1.getRoles().add(userRole);
        
        user2 = new User();
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setEmail("jane.smith@example.com");
        user2.setPassword("encodedPassword456");
        user2.setRoles(new HashSet<>());
        user2.getRoles().add(userRole);
    }

    @Test
    void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // Given
        entityManager.persistAndFlush(user1);

        // When
        boolean exists = userRepository.existsByEmail("john.doe@example.com");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertFalse(exists);
    }

    @Test
    void findByEmail_WhenEmailExists_ShouldReturnUser() {
        // Given
        entityManager.persistAndFlush(user1);

        // When
        Optional<User> foundUser = userRepository.findByEmail("john.doe@example.com");

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals("john.doe@example.com", foundUser.get().getEmail());
        assertEquals("John", foundUser.get().getFirstName());
        assertEquals("Doe", foundUser.get().getLastName());
        assertEquals(user1.getId(), foundUser.get().getId());
    }

    @Test
    void findByEmail_WhenEmailDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertFalse(foundUser.isPresent());
    }

    @Test
    void save_ShouldPersistUser() {
        // When
        User savedUser = userRepository.save(user1);

        // Then
        assertNotNull(savedUser.getId());
        assertEquals("john.doe@example.com", savedUser.getEmail());
        assertEquals("John", savedUser.getFirstName());
        assertEquals("Doe", savedUser.getLastName());
        
        // Verify it's actually persisted
        User foundUser = entityManager.find(User.class, savedUser.getId());
        assertNotNull(foundUser);
        assertEquals("john.doe@example.com", foundUser.getEmail());
    }

    @Test
    void save_ShouldPersistUserWithRoles() {
        // When
        User savedUser = userRepository.save(user1);

        // Then
        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getRoles());
        assertEquals(1, savedUser.getRoles().size());
        assertTrue(savedUser.getRoles().contains(userRole));
        
        // Verify roles are persisted
        User foundUser = entityManager.find(User.class, savedUser.getId());
        assertNotNull(foundUser.getRoles());
        assertEquals(1, foundUser.getRoles().size());
    }

    @Test
    void findById_WhenUserExists_ShouldReturnUser() {
        // Given
        entityManager.persistAndFlush(user1);

        // When
        Optional<User> foundUser = userRepository.findById(user1.getId());

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals("john.doe@example.com", foundUser.get().getEmail());
        assertEquals("John", foundUser.get().getFirstName());
    }

    @Test
    void findById_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // When
        Optional<User> foundUser = userRepository.findById(999L);

        // Then
        assertFalse(foundUser.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        // Given
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);

        // When
        List<User> allUsers = userRepository.findAll();

        // Then
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.stream().anyMatch(user -> user.getEmail().equals("john.doe@example.com")));
        assertTrue(allUsers.stream().anyMatch(user -> user.getEmail().equals("jane.smith@example.com")));
    }

    @Test
    void delete_ShouldRemoveUser() {
        // Given
        entityManager.persistAndFlush(user1);
        Long userId = user1.getId();

        // When
        userRepository.delete(user1);
        entityManager.flush();

        // Then
        User deletedUser = entityManager.find(User.class, userId);
        assertNull(deletedUser);
    }

    @Test
    void deleteById_ShouldRemoveUser() {
        // Given
        entityManager.persistAndFlush(user1);
        Long userId = user1.getId();

        // When
        userRepository.deleteById(userId);
        entityManager.flush();

        // Then
        User deletedUser = entityManager.find(User.class, userId);
        assertNull(deletedUser);
    }

    @Test
    void existsById_WhenUserExists_ShouldReturnTrue() {
        // Given
        entityManager.persistAndFlush(user1);

        // When
        boolean exists = userRepository.existsById(user1.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WhenUserDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = userRepository.existsById(999L);

        // Then
        assertFalse(exists);
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // Given
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);

        // When
        long count = userRepository.count();

        // Then
        assertEquals(2, count);
    }

    @Test
    void save_ShouldUpdateExistingUser() {
        // Given
        entityManager.persistAndFlush(user1);
        user1.setFirstName("Updated");
        user1.setLastName("Name");

        // When
        User updatedUser = userRepository.save(user1);

        // Then
        assertEquals("Updated", updatedUser.getFirstName());
        assertEquals("Name", updatedUser.getLastName());
        assertEquals(user1.getId(), updatedUser.getId());
        
        // Verify it's actually updated in database
        User foundUser = entityManager.find(User.class, user1.getId());
        assertEquals("Updated", foundUser.getFirstName());
        assertEquals("Name", foundUser.getLastName());
    }
}