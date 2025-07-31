package com.pjariwala.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
  @NotEmpty(message = "Refresh token is required")
  private String refreshToken;

  @NotEmpty(message = "Username is required for SECRET_HASH calculation")
  private String username;
}
