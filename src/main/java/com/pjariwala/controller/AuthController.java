package com.pjariwala.controller;

import com.pjariwala.dto.AuthRequest;
import com.pjariwala.dto.AuthResponse;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.service.AuthService;
import com.pjariwala.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Authentication", description = "APIs for user authentication and registration")
public class AuthController {

  @Autowired private AuthService authService;
  @Autowired private AuthUtil authUtil;

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
  @Operation(
      summary = "User login",
      description = "Authenticate user and get JWT token. No authentication required.")
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
  @Operation(summary = "User logout", description = "Logout user by invalidating JWT token")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> logout(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("accessToken") String accessToken) {
    log.info("evt=logout_request userId={}", userId);
    authService.logout(userId, accessToken);
    log.info("evt=logout_success userId={}", userId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/change-password")
  @Operation(summary = "Change password", description = "Change password for authenticated user")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> changePassword(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("accessToken") String accessToken,
      @RequestParam String oldPassword,
      @RequestParam String newPassword) {
    log.info("evt=change_password_request userId={}", userId);
    authService.changePassword(userId, accessToken, oldPassword, newPassword);
    log.info("evt=change_password_success userId={}", userId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/forgot-password")
  @Operation(summary = "Forgot password", description = "Initiate password reset process")
  public ResponseEntity<Void> forgotPassword(@RequestParam String email) {
    log.info("evt=forgot_password_request email={}", email);
    authService.forgotPassword(email);
    log.info("evt=forgot_password_success email={}", email);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reset-password")
  @Operation(summary = "Reset password", description = "Reset password using confirmation code")
  public ResponseEntity<Void> resetPassword(
      @RequestParam String email,
      @RequestParam String confirmationCode,
      @RequestParam String newPassword) {
    log.info("evt=reset_password_request email={}", email);
    authService.resetPassword(email, confirmationCode, newPassword);
    log.info("evt=reset_password_success email={}", email);
    return ResponseEntity.ok().build();
  }
}
