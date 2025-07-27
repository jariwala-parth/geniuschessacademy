package com.pjariwala.util;

import com.pjariwala.exception.AuthException;
import com.pjariwala.model.User;
import com.pjariwala.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthUtil {

  @Autowired private JwtUtil jwtUtil;

  @Autowired private UserService userService;

  /**
   * Extract access token from Authorization header
   *
   * @param authorization Authorization header value
   * @return access token without "Bearer " prefix
   */
  public String extractAccessToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      log.error("evt=extract_access_token_error msg=invalid_authorization_header");
      throw AuthException.invalidToken();
    }
    return authorization.replace("Bearer ", "");
  }

  /**
   * Get user information from JWT token
   *
   * @param authorization Authorization header value
   * @return User object from database
   */
  public User getCurrentUser(String authorization) {
    log.debug("evt=get_current_user_start");

    String accessToken = extractAccessToken(authorization);

    if (!jwtUtil.validateToken(accessToken)) {
      log.error("evt=get_current_user_error msg=invalid_token");
      throw AuthException.invalidToken();
    }

    String email = jwtUtil.getEmailFromToken(accessToken);
    if (email == null) {
      log.error("evt=get_current_user_error msg=email_not_found_in_token");
      throw AuthException.invalidToken();
    }

    User user =
        userService
            .getUserByEmail(email)
            .orElseThrow(
                () -> {
                  log.error("evt=get_current_user_error msg=user_not_found email={}", email);
                  return AuthException.invalidToken();
                });

    log.debug(
        "evt=get_current_user_success userId={} userType={}", user.getUserId(), user.getUserType());
    return user;
  }

  /**
   * Check if current user is a coach (admin)
   *
   * @param authorization Authorization header value
   * @return true if user is a coach
   */
  public boolean isCoach(String authorization) {
    try {
      User user = getCurrentUser(authorization);
      return "COACH".equals(user.getUserType());
    } catch (Exception e) {
      log.error("evt=is_coach_check_error", e);
      return false;
    }
  }

  /**
   * Require that the current user is a coach, throw exception if not
   *
   * @param authorization Authorization header value
   * @return User object (guaranteed to be a coach)
   */
  public User requireCoach(String authorization) {
    log.debug("evt=require_coach_start");

    User user = getCurrentUser(authorization);

    if (!"COACH".equals(user.getUserType())) {
      log.error(
          "evt=require_coach_error userId={} userType={} msg=insufficient_permissions",
          user.getUserId(),
          user.getUserType());
      throw new AuthException("ACCESS_DENIED", "Only coaches can perform this action", 403);
    }

    log.debug("evt=require_coach_success userId={}", user.getUserId());
    return user;
  }

  /**
   * Check if current user can access student data (either the student themselves or a coach)
   *
   * @param authorization Authorization header value
   * @param studentId Student ID to check access for
   * @return true if user can access this student's data
   */
  public boolean canAccessStudent(String authorization, String studentId) {
    try {
      User user = getCurrentUser(authorization);

      // Coaches can access any student
      if ("COACH".equals(user.getUserType())) {
        return true;
      }

      // Students can only access their own data
      return user.getUserId().equals(studentId);
    } catch (Exception e) {
      log.error("evt=can_access_student_error studentId={}", studentId, e);
      return false;
    }
  }

  /**
   * Require that current user can access student data
   *
   * @param authorization Authorization header value
   * @param studentId Student ID to check access for
   * @return User object
   */
  public User requireStudentAccess(String authorization, String studentId) {
    log.debug("evt=require_student_access_start studentId={}", studentId);

    User user = getCurrentUser(authorization);

    // Coaches can access any student
    if ("COACH".equals(user.getUserType())) {
      log.debug(
          "evt=require_student_access_success userId={} role=coach studentId={}",
          user.getUserId(),
          studentId);
      return user;
    }

    // Students can only access their own data
    if (!user.getUserId().equals(studentId)) {
      log.error(
          "evt=require_student_access_error userId={} studentId={} msg=access_denied",
          user.getUserId(),
          studentId);
      throw new AuthException("ACCESS_DENIED", "You can only access your own data", 403);
    }

    log.debug("evt=require_student_access_success userId={} role=student", user.getUserId());
    return user;
  }
}
