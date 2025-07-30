package com.pjariwala.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
  @NotEmpty(message = "Login (username, email, or phone) is required")
  private String login; // Can be username, email, or phone number

  @NotEmpty(message = "Password is required")
  private String password;
}
