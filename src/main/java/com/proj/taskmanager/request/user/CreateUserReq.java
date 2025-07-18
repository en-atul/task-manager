package com.proj.taskmanager.request.user;

import com.proj.taskmanager.enums.UserRole;
import com.proj.taskmanager.validation.user.AccountExists;
import jakarta.validation.constraints.*;

public record CreateUserReq(@NotEmpty String firstName,
                         @NotEmpty String lastName,
                         @NotEmpty @Email @AccountExists String email,
                            @NotEmpty @Pattern(
                                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d[^A-Za-z\\d]]{8,}$",
                                    message = "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character."
                            )
                            String password,
                         @NotNull UserRole role) {
}
