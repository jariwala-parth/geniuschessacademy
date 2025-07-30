package com.pjariwala.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
  @NotEmpty(message = "Username is required")
  private String username;

  @NotEmpty(message = "Email is required")
  @Email(message = "Email must be valid")
  private String email;

  @NotEmpty(message = "Password is required")
  private String password;

  @NotEmpty(message = "Name is required")
  private String name;

  @NotEmpty(message = "Phone number is required")
  @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
  private String phoneNumber;

  @NotEmpty(message = "User type is required")
  @Pattern(regexp = "^(COACH|STUDENT)$", message = "User type must be either COACH or STUDENT")
  private String userType; // "COACH" or "STUDENT"

  private String organizationId;

  // Student-specific fields
  private String guardianName;
  private String guardianPhone;

  // Coach-specific fields
  private Boolean isAdmin = false;
}
