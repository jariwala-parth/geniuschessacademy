package com.pjariwala.exception;

import com.pjariwala.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the entire application Handles all custom exceptions and provides
 * consistent error responses
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /** Handle AuthException - authentication and authorization related errors */
  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ErrorResponse> handleAuthException(
      AuthException e, HttpServletRequest request) {
    log.error(
        "AuthException caught - Code: {}, Message: {}, Path: {}",
        e.getErrorCode(),
        e.getMessage(),
        request.getRequestURI(),
        e);
    return ResponseEntity.status(e.getHttpStatus())
        .body(
            new ErrorResponse(
                e.getErrorCode(), e.getMessage(), e.getHttpStatus(), request.getRequestURI()));
  }

  /** Handle UserException - user management related errors */
  @ExceptionHandler(UserException.class)
  public ResponseEntity<ErrorResponse> handleUserException(
      UserException e, HttpServletRequest request) {
    log.error(
        "UserException caught - Code: {}, Message: {}, Path: {}",
        e.getErrorCode(),
        e.getMessage(),
        request.getRequestURI(),
        e);
    return ResponseEntity.status(e.getHttpStatus())
        .body(
            new ErrorResponse(
                e.getErrorCode(), e.getMessage(), e.getHttpStatus(), request.getRequestURI()));
  }

  /** Handle generic RuntimeException as fallback */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleRuntimeException(
      RuntimeException e, HttpServletRequest request) {
    log.error(
        "Unhandled RuntimeException caught - Message: {}, Path: {}",
        e.getMessage(),
        request.getRequestURI(),
        e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred: " + e.getMessage(),
                500,
                request.getRequestURI()));
  }

  /** Handle generic Exception as ultimate fallback */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception e, HttpServletRequest request) {
    log.error(
        "Unhandled Exception caught - Type: {}, Message: {}, Path: {}",
        e.getClass().getSimpleName(),
        e.getMessage(),
        request.getRequestURI(),
        e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ErrorResponse(
                "INTERNAL_ERROR",
                "A system error occurred: " + e.getMessage(),
                500,
                request.getRequestURI()));
  }
}
