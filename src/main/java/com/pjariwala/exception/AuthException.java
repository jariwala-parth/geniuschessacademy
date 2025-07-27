package com.pjariwala.exception;

/** Custom exception for authentication-related operations */
public class AuthException extends RuntimeException {

  private final String errorCode;
  private final int httpStatus;

  public AuthException(String errorCode, String message, int httpStatus) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }

  public AuthException(String errorCode, String message, int httpStatus, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  // Static factory methods for common auth exceptions
  public static AuthException invalidCredentials() {
    return new AuthException("INVALID_CREDENTIALS", "Invalid email or password", 401);
  }

  public static AuthException userExists() {
    return new AuthException("USER_EXISTS", "User already exists", 409);
  }

  public static AuthException invalidPassword(String message) {
    return new AuthException("INVALID_PASSWORD", message, 400);
  }

  public static AuthException emailNotConfirmed() {
    return new AuthException("EMAIL_NOT_CONFIRMED", "Email not confirmed", 403);
  }

  public static AuthException invalidToken() {
    return new AuthException("INVALID_TOKEN", "Invalid or expired token", 401);
  }

  public static AuthException validationError(String message) {
    return new AuthException("VALIDATION_ERROR", message, 400);
  }

  public static AuthException invalidCode() {
    return new AuthException("INVALID_CODE", "Invalid confirmation code", 400);
  }

  public static AuthException expiredCode() {
    return new AuthException("EXPIRED_CODE", "Confirmation code has expired", 400);
  }

  public static AuthException cognitoError(String message, Throwable cause) {
    return new AuthException(
        "COGNITO_ERROR", "Authentication service error: " + message, 500, cause);
  }
}
