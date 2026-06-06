package com.parth.sportsapp.sportsbackend.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central exception handler. Converts all uncaught exceptions into safe JSON
 * responses without leaking stack traces, SQL fragments, or internal class
 * names to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // --- Domain exceptions (typed) ---------------------------------------------

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
    return body(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
    return body(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
    return body(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
    return body(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  // --- Spring Security ------------------------------------------------------

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
    return body(HttpStatus.FORBIDDEN, "Access denied");
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, Object>> handleAuth(AuthenticationException ex) {
    return body(HttpStatus.UNAUTHORIZED, "Authentication required");
  }

  // --- Bean Validation ------------------------------------------------------

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleBodyValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
            (a, b) -> a));
    Map<String, Object> b = baseBody(HttpStatus.BAD_REQUEST, "Validation failed");
    b.put("fieldErrors", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(b);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, Object>> handleParamValidation(ConstraintViolationException ex) {
    return body(HttpStatus.BAD_REQUEST, "Invalid request parameters");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
    return body(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return body(HttpStatus.BAD_REQUEST, "Invalid value for parameter: " + ex.getName());
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> handleUnreadableBody(
      org.springframework.http.converter.HttpMessageNotReadableException ex) {
    // Malformed JSON, or an unknown property when fail-on-unknown-properties is on.
    return body(HttpStatus.BAD_REQUEST, "Malformed or unexpected request body");
  }

  // --- Database -------------------------------------------------------------

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
    // Log full details server-side, but never return them to the client.
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return body(HttpStatus.CONFLICT, "Request conflicts with existing data");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException ex) {
    return body(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  // --- Last-resort catch-all -----------------------------------------------

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnknown(Exception ex) {
    // Full stack trace stays in server logs only.
    log.error("Unhandled exception", ex);
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  // --- helpers --------------------------------------------------------------

  private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(baseBody(status, message));
  }

  private Map<String, Object> baseBody(HttpStatus status, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", OffsetDateTime.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message == null ? "" : message);
    return body;
  }
}
