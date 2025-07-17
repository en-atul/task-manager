package com.proj.taskmanager.service.user;

import com.proj.taskmanager.dto.UserDto;
import com.proj.taskmanager.model.User;
import com.proj.taskmanager.request.user.CreateUserReq;

public interface IUserService {

    User createUser(CreateUserReq request);

    UserDto convertUserToDto(User user);

    User getUserById(Long userId);
    
    User getUserByEmail(String email);
}
