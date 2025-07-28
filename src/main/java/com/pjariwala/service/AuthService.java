package com.pjariwala.service;

import com.pjariwala.dto.AuthRequest;
import com.pjariwala.dto.AuthResponse;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.dto.UserInfo;

public interface AuthService {

  /** Register a new user (coach or student) */
  AuthResponse signup(SignupRequest signupRequest);

  /** Add a new student (for coaches only) */
  UserInfo addStudent(SignupRequest signupRequest, String coachId);

  /** Authenticate user and return tokens */
  AuthResponse login(AuthRequest authRequest);

  /** Refresh access token using refresh token */
  AuthResponse refreshToken(String refreshToken);

  /** Logout user by invalidating tokens */
  void logout(String userId, String accessToken);

  /** Change user password */
  void changePassword(String userId, String accessToken, String oldPassword, String newPassword);

  /** Initiate forgot password process */
  void forgotPassword(String login);

  /** Reset password using confirmation code */
  void resetPassword(String login, String confirmationCode, String newPassword);
}
