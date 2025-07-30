package com.pjariwala.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
  private String username;
  private String email;
  private String password;
  private String name;
  private String phoneNumber;
  private String userType; // "COACH" or "STUDENT"
  private String organizationId;

  // Student-specific fields
  private String guardianName;
  private String guardianPhone;

  // Coach-specific fields
  private Boolean isAdmin = false;
}
