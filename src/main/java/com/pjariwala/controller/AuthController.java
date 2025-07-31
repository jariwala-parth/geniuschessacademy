package com.pjariwala.controller;

import com.pjariwala.dto.AuthChallengeRequest;
import com.pjariwala.dto.AuthRequest;
import com.pjariwala.dto.AuthResponse;
import com.pjariwala.dto.LoginResult;
import com.pjariwala.dto.RefreshTokenRequest;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.AuthService;
import com.pjariwala.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
  @Autowired private ActivityLogService activityLogService;

  @PostMapping("/signup")
  @Operation(
      summary = "User registration",
      description =
          "Register a new user (coach or student). No authentication required for initial coach"
              + " signup.")
  public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
    log.info(
        "Received signup request for email: {} with userType: {}",
        signupRequest.getEmail(),
        signupRequest.getUserType());
    AuthResponse response = authService.signup(signupRequest);
    log.info("Signup completed successfully for email: {}", signupRequest.getEmail());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/add-student")
  @Operation(
      summary = "Add new student",
      description = "Allows coaches to register new students. Requires coach authentication.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<UserInfo> addStudent(
      @Parameter(hidden = true) @RequestAttribute("userId") String coachId,
      @Valid @RequestBody SignupRequest signupRequest) {

    log.info("evt=add_student_request coachId={} email={}", coachId, signupRequest.getEmail());

    // Force the user type to be STUDENT regardless of what's in the request
    signupRequest.setUserType("STUDENT");

    UserInfo studentInfo = authService.addStudent(signupRequest, coachId);
    log.info("evt=add_student_success coachId={} email={}", coachId, signupRequest.getEmail());
    return ResponseEntity.status(HttpStatus.CREATED).body(studentInfo);
  }

  @PostMapping("/login")
  @Operation(
      summary = "User login",
      description =
          "Authenticate user and get JWT token or challenge response. No authentication required.")
  public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
    log.info("evt=login_request login={}", authRequest.getLogin());
    LoginResult result = authService.login(authRequest);

    if (result.isChallenge()) {
      log.info("evt=login_challenge_required login={}", authRequest.getLogin());
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(result.getChallengeResponse());
    }

    log.info("evt=login_success login={}", authRequest.getLogin());
    return ResponseEntity.ok(result.getAuthResponse());
  }

  @PostMapping("/respond-challenge")
  @Operation(
      summary = "Respond to authentication challenge",
      description = "Respond to NEW_PASSWORD_REQUIRED challenge by setting a new password")
  public ResponseEntity<AuthResponse> respondChallenge(
      @Valid @RequestBody AuthChallengeRequest challengeRequest) {
    log.info("evt=respond_challenge_request");
    AuthResponse response = authService.respondChallenge(challengeRequest);
    log.info("evt=respond_challenge_success");
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refreshToken(
      @RequestBody RefreshTokenRequest refreshTokenRequest) {
    log.info(
        "evt=refresh_token_request refreshToken={}",
        refreshTokenRequest.getRefreshToken() != null
            ? refreshTokenRequest
                    .getRefreshToken()
                    .substring(0, Math.min(20, refreshTokenRequest.getRefreshToken().length()))
                + "..."
            : "null");
    AuthResponse response = authService.refreshToken(refreshTokenRequest.getRefreshToken());
    log.info("evt=refresh_token_success");
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
