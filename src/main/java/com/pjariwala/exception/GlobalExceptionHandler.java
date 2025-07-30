package com.pjariwala.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.pjariwala.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for the entire application Handles all custom exceptions and provides
 * consistent error responses
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /** Handle AuthException - authentication and authorization related errors */
  @ExceptionHandler(AuthException.class)
  public ResponseEntity<?> handleAuthException(AuthException e, HttpServletRequest request) {

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

  /** Handle Bean Validation errors (@Valid annotations) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException e, HttpServletRequest request) {

    StringBuilder errorMessage = new StringBuilder("Validation failed: ");
    e.getBindingResult()
        .getFieldErrors()
        .forEach(
            error ->
                errorMessage
                    .append(error.getField())
                    .append(" - ")
                    .append(error.getDefaultMessage())
                    .append("; "));

    log.warn(
        "Validation error - Path: {}, Errors: {}",
        request.getRequestURI(),
        errorMessage.toString());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "VALIDATION_ERROR", errorMessage.toString(), 400, request.getRequestURI()));
  }

  /** Handle Constraint Validation errors */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException e, HttpServletRequest request) {

    StringBuilder errorMessage = new StringBuilder("Validation failed: ");
    for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
      errorMessage
          .append(violation.getPropertyPath())
          .append(" - ")
          .append(violation.getMessage())
          .append("; ");
    }

    log.warn(
        "Constraint violation - Path: {}, Errors: {}",
        request.getRequestURI(),
        errorMessage.toString());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "VALIDATION_ERROR", errorMessage.toString(), 400, request.getRequestURI()));
  }

  /** Handle JSON parsing errors including time format issues */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleJsonParsingException(
      HttpMessageNotReadableException e, HttpServletRequest request) {

    String errorMessage = "Invalid request format";

    // Check for specific InvalidFormatException (like time format issues)
    if (e.getCause() instanceof InvalidFormatException) {
      InvalidFormatException ife = (InvalidFormatException) e.getCause();
      if (ife.getTargetType() != null && ife.getTargetType().getSimpleName().equals("LocalTime")) {
        errorMessage =
            String.format(
                "Invalid time format '%s'. Expected formats: HH:mm, H:mm, H.mm (e.g., '18:30',"
                    + " '6:30', '6.30')",
                ife.getValue());
      } else {
        errorMessage =
            String.format(
                "Invalid value '%s' for field '%s'",
                ife.getValue(), ife.getPath().get(ife.getPath().size() - 1).getFieldName());
      }
    }

    log.warn("JSON parsing error - Path: {}, Error: {}", request.getRequestURI(), errorMessage);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "INVALID_REQUEST_FORMAT", errorMessage, 400, request.getRequestURI()));
  }

  /** Handle method argument type mismatch (e.g., invalid enum values) */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatchException(
      MethodArgumentTypeMismatchException e, HttpServletRequest request) {

    String errorMessage =
        String.format("Invalid value '%s' for parameter '%s'", e.getValue(), e.getName());

    log.warn("Type mismatch error - Path: {}, Error: {}", request.getRequestURI(), errorMessage);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_PARAMETER", errorMessage, 400, request.getRequestURI()));
  }

  /** Handle IllegalArgumentException */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException e, HttpServletRequest request) {

    log.warn("Illegal argument - Path: {}, Error: {}", request.getRequestURI(), e.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_ARGUMENT", e.getMessage(), 400, request.getRequestURI()));
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
