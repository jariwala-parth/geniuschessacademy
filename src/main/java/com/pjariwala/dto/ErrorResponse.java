package com.pjariwala.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
  private String error;
  private String message;
  private int status;
  private LocalDateTime timestamp;
  private String path;

  public ErrorResponse(String error, String message, int status, String path) {
    this.error = error;
    this.message = message;
    this.status = status;
    this.path = path;
    this.timestamp = LocalDateTime.now();
  }
}
