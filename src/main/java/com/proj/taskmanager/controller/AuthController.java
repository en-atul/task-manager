package com.proj.taskmanager.controller;

import com.proj.taskmanager.dto.UserDto;
import com.proj.taskmanager.model.User;
import com.proj.taskmanager.request.user.CreateUserReq;
import com.proj.taskmanager.response.ApiResponse;
import com.proj.taskmanager.service.user.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CONFLICT;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/auth")
public class AuthController {
    private final IUserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody CreateUserReq request) {
        try {
            User user = userService.createUser(request);
            UserDto userDto = userService.convertUserToDto(user);
            return ResponseEntity.ok(new ApiResponse("Register Success!", userDto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(CONFLICT).body(new ApiResponse(e.getMessage(), null));
        }
    }

}
