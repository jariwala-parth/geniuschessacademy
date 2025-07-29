package com.pjariwala.controller;

import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.SessionCreateRequest;
import com.pjariwala.dto.SessionDTO;
import com.pjariwala.dto.SessionUpdateRequest;
import com.pjariwala.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/sessions")
@CrossOrigin(origins = "*")
@Tag(name = "Session Management", description = "APIs for managing chess sessions")
public class SessionController {

  @Autowired private SessionService sessionService;

  @PostMapping
  @Operation(summary = "Create a new session", description = "Creates a new session for a batch")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<SessionDTO> createSession(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @RequestBody SessionCreateRequest request) {
    log.info(
        "evt=create_session_request batchId={} userId={} userType={}",
        request.getBatchId(),
        userId,
        userType);

    SessionDTO session = sessionService.createSession(request, userId);
    return ResponseEntity.ok(session);
  }

  @GetMapping("/{sessionId}")
  @Operation(summary = "Get session by ID", description = "Retrieves a specific session by its ID")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<SessionDTO> getSessionById(
      @PathVariable String sessionId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType) {
    log.info(
        "evt=get_session_by_id_request sessionId={} userId={} userType={}",
        sessionId,
        userId,
        userType);

    SessionDTO session = sessionService.getSessionById(sessionId, userId);
    return ResponseEntity.ok(session);
  }

  @GetMapping("/batch/{batchId}")
  @Operation(
      summary = "Get sessions by batch",
      description = "Retrieves all sessions for a specific batch")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<SessionDTO>> getSessionsByBatch(
      @PathVariable String batchId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType) {
    log.info(
        "evt=get_sessions_by_batch_request batchId={} page={} size={} userId={} userType={}",
        batchId,
        page,
        size,
        userId,
        userType);

    PageResponseDTO<SessionDTO> sessions =
        sessionService.getSessionsByBatch(batchId, page, size, userId);
    return ResponseEntity.ok(sessions);
  }

  @GetMapping("/date-range")
  @Operation(
      summary = "Get sessions by date range",
      description = "Retrieves sessions within a specified date range")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<SessionDTO>> getSessionsByDateRange(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType) {
    log.info(
        "evt=get_sessions_by_date_range_request startDate={} endDate={} page={} size={}"
            + " userId={} userType={}",
        startDate,
        endDate,
        page,
        size,
        userId,
        userType);

    PageResponseDTO<SessionDTO> sessions =
        sessionService.getSessionsByDateRange(startDate, endDate, page, size, userId);
    return ResponseEntity.ok(sessions);
  }

  @PutMapping("/{sessionId}")
  @Operation(summary = "Update session", description = "Updates an existing session")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<SessionDTO> updateSession(
      @PathVariable String sessionId,
      @RequestBody SessionUpdateRequest request,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType) {
    log.info(
        "evt=update_session_request sessionId={} userId={} userType={}",
        sessionId,
        userId,
        userType);

    SessionDTO session = sessionService.updateSession(sessionId, request, userId);
    return ResponseEntity.ok(session);
  }

  @DeleteMapping("/{sessionId}")
  @Operation(summary = "Delete session", description = "Deletes an existing session")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> deleteSession(
      @PathVariable String sessionId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType) {
    log.info(
        "evt=delete_session_request sessionId={} userId={} userType={}",
        sessionId,
        userId,
        userType);

    sessionService.deleteSession(sessionId, userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/batch/{batchId}/generate")
  @Operation(
      summary = "Generate sessions for batch",
      description = "Generates multiple sessions for a batch within a date range")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<SessionDTO>> generateSessionsForBatch(
      @PathVariable String batchId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType) {
    log.info(
        "evt=generate_sessions_for_batch_request batchId={} startDate={} endDate={}"
            + " userId={} userType={}",
        batchId,
        startDate,
        endDate,
        userId,
        userType);

    List<SessionDTO> sessions =
        sessionService.generateSessionsForBatch(batchId, startDate, endDate, userId);
    return ResponseEntity.ok(sessions);
  }

  @GetMapping("/today")
  @Operation(
      summary = "Get today's sessions",
      description = "Retrieves all sessions scheduled for today")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<SessionDTO>> getTodaysSessions(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType) {
    log.info("evt=get_todays_sessions_request userId={} userType={}", userId, userType);

    List<SessionDTO> sessions = sessionService.getTodaysSessions(userId);
    return ResponseEntity.ok(sessions);
  }
}
