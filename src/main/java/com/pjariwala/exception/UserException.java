package com.pjariwala.exception;

/** Custom exception for user-related operations */
public class UserException extends RuntimeException {

  private final String errorCode;
  private final int httpStatus;

  public UserException(String errorCode, String message, int httpStatus) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
  }

  public UserException(String errorCode, String message, int httpStatus, Throwable cause) {
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

  // Static factory methods for common exceptions
  public static UserException userNotFound(String identifier) {
    return new UserException("USER_NOT_FOUND", "User not found: " + identifier, 404);
  }

  public static UserException userExists(String identifier) {
    return new UserException("USER_EXISTS", "User already exists: " + identifier, 409);
  }

  public static UserException validationError(String message) {
    return new UserException("VALIDATION_ERROR", message, 400);
  }

  public static UserException databaseError(String message, Throwable cause) {
    return new UserException("DATABASE_ERROR", "Database operation failed: " + message, 500, cause);
  }
}
