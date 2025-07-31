package com.pjariwala.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
  private String accessToken;
  private String refreshToken;
  private String idToken;
  private String tokenType;
  private Integer expiresIn;
}
