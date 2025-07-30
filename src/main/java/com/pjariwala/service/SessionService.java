package com.pjariwala.service;

import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.SessionCreateRequest;
import com.pjariwala.dto.SessionDTO;
import com.pjariwala.dto.SessionUpdateRequest;
import java.time.LocalDate;
import java.util.List;

public interface SessionService {

  /** Create a new session */
  SessionDTO createSession(
      SessionCreateRequest request, String requestingUserId, String organizationId);

  /** Get session by ID */
  SessionDTO getSessionById(String sessionId, String requestingUserId, String organizationId);

  /** Get all sessions for a batch */
  PageResponseDTO<SessionDTO> getSessionsByBatch(
      String batchId, int page, int size, String requestingUserId, String organizationId);

  /** Get sessions by date range */
  PageResponseDTO<SessionDTO> getSessionsByDateRange(
      LocalDate startDate,
      LocalDate endDate,
      int page,
      int size,
      String requestingUserId,
      String organizationId);

  /** Update session */
  SessionDTO updateSession(
      String sessionId,
      SessionUpdateRequest request,
      String requestingUserId,
      String organizationId);

  /** Delete session */
  void deleteSession(String sessionId, String requestingUserId, String organizationId);

  /** Generate sessions for a batch (automated) */
  List<SessionDTO> generateSessionsForBatch(
      String batchId,
      LocalDate startDate,
      LocalDate endDate,
      String requestingUserId,
      String organizationId);
}
