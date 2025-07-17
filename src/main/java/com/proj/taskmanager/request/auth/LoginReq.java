package com.proj.taskmanager.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record LoginReq(
                            @NotEmpty @Email String email,
                            @Pattern(
                                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])[A-Za-z\\d[^A-Za-z\\d]]{8,}$",
                                    message = "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character."
                            )
                            String password) {
}
