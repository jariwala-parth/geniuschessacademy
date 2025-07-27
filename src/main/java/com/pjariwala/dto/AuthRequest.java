package com.pjariwala.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
  private String login; // Can be username, email, or phone number
  private String password;
  private String userType; // "COACH" or "STUDENT"
}
