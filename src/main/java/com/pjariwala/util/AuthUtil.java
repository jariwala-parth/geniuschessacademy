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

    String cognitoSub = jwtUtil.getCognitoSubFromToken(accessToken);
    if (cognitoSub == null) {
      log.error("evt=get_current_user_error msg=cognito_sub_not_found_in_token");
      throw AuthException.invalidToken();
    }

    User user =
        userService
            .getUserByCognitoSub(cognitoSub)
            .orElseThrow(
                () -> {
                  log.error(
                      "evt=get_current_user_error msg=user_not_found cognitoSub={}", cognitoSub);
                  return AuthException.invalidToken();
                });

    log.debug(
        "evt=get_current_user_success userId={} cognitoSub={} userType={}",
        user.getUserId(),
        user.getCognitoSub(),
        user.getUserType());
    return user;
  }

  /**
   * Validate JWT token and return cognitoSub
   *
   * @param accessToken JWT access token
   * @return cognitoSub from token
   */
  public String validateTokenAndGetCognitoSub(String accessToken) {
    if (!jwtUtil.validateToken(accessToken)) {
      log.error("evt=validate_token_error msg=invalid_token");
      throw AuthException.invalidToken();
    }

    String cognitoSub = jwtUtil.getCognitoSubFromToken(accessToken);
    if (cognitoSub == null) {
      log.error("evt=validate_token_error msg=cognito_sub_not_found_in_token");
      throw AuthException.invalidToken();
    }

    return cognitoSub;
  }

  /**
   * Validate JWT token and return userId (our internal ID)
   *
   * @param accessToken JWT access token
   * @return userId from our database
   */
  public String validateTokenAndGetUserId(String accessToken) {
    User user = getCurrentUser("Bearer " + accessToken);
    return user.getUserId();
  }

  /**
   * Validate JWT token and return email (legacy method for backward compatibility)
   *
   * @param accessToken JWT access token
   * @return email from token
   */
  public String validateTokenAndGetEmail(String accessToken) {
    if (!jwtUtil.validateToken(accessToken)) {
      log.error("evt=validate_token_error msg=invalid_token");
      throw AuthException.invalidToken();
    }

    String email = jwtUtil.getEmailFromToken(accessToken);
    if (email == null) {
      log.error("evt=validate_token_error msg=email_not_found_in_token");
      throw AuthException.invalidToken();
    }

    return email;
  }
}
