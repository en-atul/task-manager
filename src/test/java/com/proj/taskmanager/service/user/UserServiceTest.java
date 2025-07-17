package com.proj.taskmanager.service.user;

import com.proj.taskmanager.dto.UserDto;
import com.proj.taskmanager.enums.UserRole;
import com.proj.taskmanager.model.Role;
import com.proj.taskmanager.model.User;
import com.proj.taskmanager.repository.RoleRepository;
import com.proj.taskmanager.repository.UserRepository;
import com.proj.taskmanager.request.user.CreateUserReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private CreateUserReq createUserReq;
    private User user;
    private Role role;
    private String encodedPassword;

    @BeforeEach
    void setUp() {
        createUserReq = new CreateUserReq(
                "John",
                "Doe",
                "john.doe@example.com",
                "Password123!",
                UserRole.USER
        );

        user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("encodedPassword123");

        role = new Role(UserRole.USER);
        role.setId(1L);

        encodedPassword = "encodedPassword123";
    }

    @Test
    void createUser_Success() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(roleRepository.findByName(UserRole.USER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User result = userService.createUser(createUserReq);

        // Then
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getFirstName(), result.getFirstName());
        assertEquals(user.getLastName(), result.getLastName());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getPassword(), result.getPassword());

        // Verify interactions
        verify(passwordEncoder).encode(createUserReq.password());
        verify(roleRepository).findByName(UserRole.USER);
        verify(userRepository).save(any(User.class));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void createUser_WithNewRole_Success() {
        // Given
        CreateUserReq adminReq = new CreateUserReq(
                "Admin",
                "User",
                "admin@example.com",
                "AdminPass123!",
                UserRole.ADMIN
        );

        Role adminRole = new Role(UserRole.ADMIN);
        adminRole.setId(2L);

        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(encodedPassword);

        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(roleRepository.findByName(UserRole.ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(adminRole);
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        // When
        User result = userService.createUser(adminReq);

        // Then
        assertNotNull(result);
        assertEquals(adminUser.getId(), result.getId());
        assertEquals(adminUser.getFirstName(), result.getFirstName());
        assertEquals(adminUser.getLastName(), result.getLastName());
        assertEquals(adminUser.getEmail(), result.getEmail());

        // Verify interactions
        verify(passwordEncoder).encode(adminReq.password());
        verify(roleRepository).findByName(UserRole.ADMIN);
        verify(roleRepository).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_PasswordEncoded() {
        // Given
        String rawPassword = "Password123!";
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(roleRepository.findByName(UserRole.USER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        // When
        User result = userService.createUser(createUserReq);

        // Then
        verify(passwordEncoder).encode(rawPassword);
        assertEquals(encodedPassword, result.getPassword());
        assertNotEquals(rawPassword, result.getPassword());
    }

    @Test
    void createUser_UserFieldsSetCorrectly() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(roleRepository.findByName(UserRole.USER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        // When
        User result = userService.createUser(createUserReq);

        // Then
        assertEquals(createUserReq.firstName(), result.getFirstName());
        assertEquals(createUserReq.lastName(), result.getLastName());
        assertEquals(createUserReq.email(), result.getEmail());
        assertTrue(result.getRoles().contains(role));
        assertEquals(1, result.getRoles().size());
    }

    @Test
    void createUser_RoleAddedToUser() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(roleRepository.findByName(UserRole.USER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        // When
        User result = userService.createUser(createUserReq);

        // Then
        assertNotNull(result.getRoles());
        assertTrue(result.getRoles().contains(role));
        assertEquals(1, result.getRoles().size());
    }

    @Test
    void convertUserToDto() {
        UserDto mappedDto = new UserDto();
        mappedDto.setId(1L);
        mappedDto.setFirstName(user.getFirstName());
        mappedDto.setLastName(user.getLastName());
        mappedDto.setEmail(user.getEmail());

        // Given
        when(modelMapper.map(user, UserDto.class)).thenReturn(mappedDto);

        // When
        UserDto userDto = userService.convertUserToDto(user);

        // Then
        assertNotNull(userDto);
        assertEquals(userDto.getFirstName(), user.getFirstName());
        assertEquals(userDto.getLastName(), user.getLastName());

        // Verify
        verify(modelMapper).map(user, UserDto.class);

    }

    @Test
    void getUserById() {
        // Given
        when(userRepository.findById(any(Long.class))).thenReturn(Optional.of(user));

        // When
        User result = userService.getUserById(1L);

        // Then
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getFirstName(), result.getFirstName());

        // Verify
        verify(userRepository).findById(1L);

    }

    @Test
    void getUserByEmail() {
        // When
        when(userRepository.findByEmail(any(String.class))).thenReturn(Optional.of(user));

        // When
        User result = userService.getUserByEmail(user.getEmail());

        // Then
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getFirstName(), result.getFirstName());

        // Verify
        verify(userRepository).findByEmail(user.getEmail());
    }
}