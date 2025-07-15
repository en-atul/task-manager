package com.proj.taskmanager.validation.user;

import com.proj.taskmanager.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UserValidator implements ConstraintValidator<AccountExists, String> {
    private UserRepository userRepository;

    public boolean isValid(String email, ConstraintValidatorContext context) {

        boolean userExists = userRepository.existsByEmail(email);
        return !userExists;
    }
}
