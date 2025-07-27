package com.pjariwala.controller;

import com.pjariwala.dto.AuthRequest;
import com.pjariwala.dto.AuthResponse;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {

  @Autowired private AuthService authService;

  @PostMapping("/signup")
  public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest signupRequest) {
    log.info(
        "Received signup request for email: {} with userType: {}",
        signupRequest.getEmail(),
        signupRequest.getUserType());
    AuthResponse response = authService.signup(signupRequest);
    log.info("Signup completed successfully for email: {}", signupRequest.getEmail());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
    log.info(
        "Received login request for user: {} with userType: {}",
        authRequest.getLogin(),
        authRequest.getUserType());
    AuthResponse response = authService.login(authRequest);
    log.info("Login completed successfully for user: {}", authRequest.getLogin());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
    log.info("Received token refresh request");
    AuthResponse response = authService.refreshToken(refreshToken);
    log.info("Token refresh completed successfully");
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
    log.info("Received logout request");
    String accessToken = extractAccessToken(authorization);
    authService.logout(accessToken);
    log.info("Logout completed successfully");
    return ResponseEntity.ok().build();
  }

  @PostMapping("/change-password")
  public ResponseEntity<Void> changePassword(
      @RequestHeader("Authorization") String authorization,
      @RequestParam String oldPassword,
      @RequestParam String newPassword) {
    String accessToken = extractAccessToken(authorization);
    authService.changePassword(accessToken, oldPassword, newPassword);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Void> forgotPassword(@RequestParam String email) {
    authService.forgotPassword(email);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(
      @RequestParam String email,
      @RequestParam String confirmationCode,
      @RequestParam String newPassword) {
    authService.resetPassword(email, confirmationCode, newPassword);
    return ResponseEntity.ok().build();
  }

  /** Extract access token from Authorization header */
  private String extractAccessToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new RuntimeException("Valid Bearer token is required");
    }
    return authorization.replace("Bearer ", "");
  }
}
