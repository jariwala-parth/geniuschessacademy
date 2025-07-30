package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.SessionCreateRequest;
import com.pjariwala.dto.SessionDTO;
import com.pjariwala.dto.SessionUpdateRequest;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Batch;
import com.pjariwala.model.Session;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.EnrollmentService;
import com.pjariwala.service.SessionService;
import com.pjariwala.service.UserService;
import com.pjariwala.util.ValidationUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SessionServiceImpl implements SessionService {

  @Autowired private DynamoDBMapper dynamoDBMapper;

  @Autowired private UserService userService;

  @Autowired private ActivityLogService activityLogService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private ValidationUtil validationUtil;

  @Override
  public SessionDTO createSession(
      SessionCreateRequest request, String requestingUserId, String organizationId) {
    log.info(
        "evt=create_session_start batchId={} requestingUserId={} organizationId={}",
        request.getBatchId(),
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);
    validationUtil.requireCoachPermission(requestingUserId, organizationId);

    // Validate batch exists
    Batch batch = getBatch(request.getBatchId(), organizationId);
    if (batch == null) {
      throw UserException.validationError("Batch not found: " + request.getBatchId());
    }

    // Validate coach permissions
    validateCoachPermissions(batch.getCoachId(), requestingUserId);

    // Create session
    Session session =
        Session.builder()
            .sessionId("SESSION_" + UUID.randomUUID().toString().replace("-", ""))
            .organizationId(organizationId)
            .batchId(request.getBatchId())
            .sessionDate(request.getSessionDate())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .status(Session.SessionStatus.SCHEDULED)
            .coachId(request.getCoachId() != null ? request.getCoachId() : batch.getCoachId())
            .notes(request.getNotes())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    dynamoDBMapper.save(session);

    // Log activity
    try {
      activityLogService.logAction(
          ActionType.CREATE_SESSION,
          requestingUserId,
          getUser(requestingUserId).getName(),
          UserType.COACH,
          String.format("Created session for batch: %s", batch.getBatchName()),
          EntityType.SESSION,
          session.getSessionId(),
          organizationId,
          String.format("Session on %s", session.getSessionDate()));
    } catch (Exception e) {
      log.warn(
          "Failed to log session creation activity for session: {}", session.getSessionId(), e);
    }

    log.info("evt=create_session_success sessionId={}", session.getSessionId());
    return convertToDTO(session);
  }

  @Override
  public SessionDTO getSessionById(
      String sessionId, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_session_by_id sessionId={} requestingUserId={} organizationId={}",
        sessionId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Find session by scanning (since sessionId is the range key)
    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("sessionId = :sessionId AND organizationId = :organizationId")
            .withExpressionAttributeValues(
                java.util.Map.of(
                    ":sessionId", new AttributeValue(sessionId),
                    ":organizationId", new AttributeValue(organizationId)));

    List<Session> sessions = dynamoDBMapper.scan(Session.class, scanExpression);
    if (sessions.isEmpty()) {
      throw UserException.validationError("Session not found: " + sessionId);
    }

    Session session = sessions.get(0);

    // Validate permissions
    Batch batch = getBatch(session.getBatchId(), organizationId);
    if (batch != null) {
      validateCoachPermissions(batch.getCoachId(), requestingUserId);
    }

    return convertToDTO(session);
  }

  @Override
  public PageResponseDTO<SessionDTO> getSessionsByBatch(
      String batchId, int page, int size, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_sessions_by_batch batchId={} page={} size={} requestingUserId={}"
            + " organizationId={}",
        batchId,
        page,
        size,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Validate batch exists
    Batch batch = getBatch(batchId, organizationId);
    if (batch == null) {
      throw UserException.validationError("Batch not found: " + batchId);
    }

    // Check permissions: coaches can see their own batches, students can see enrolled batches
    User requestingUser = getUser(requestingUserId);
    if (UserType.COACH.name().equals(requestingUser.getUserType())) {
      // Coach can only see their own batches
      if (!batch.getCoachId().equals(requestingUserId)) {
        throw UserException.validationError(
            "Access denied: You can only view sessions for your own batches");
      }
    } else {
      // Student can only see sessions for batches they're enrolled in
      boolean isEnrolled =
          enrollmentService.isStudentEnrolled(batchId, requestingUserId, organizationId);
      if (!isEnrolled) {
        throw UserException.validationError(
            "Access denied: You can only view sessions for batches you're enrolled in");
      }
    }

    // Query sessions for this batch
    Session sessionKey = new Session();
    sessionKey.setOrganizationId(organizationId);
    sessionKey.setBatchId(batchId);

    DynamoDBQueryExpression<Session> queryExpression =
        new DynamoDBQueryExpression<Session>()
            .withHashKeyValues(sessionKey)
            .withLimit(size)
            .withScanIndexForward(false); // Most recent first

    PaginatedQueryList<Session> sessions = dynamoDBMapper.query(Session.class, queryExpression);

    // Apply pagination
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, sessions.size());
    List<Session> paginatedSessions = sessions.subList(startIndex, endIndex);

    List<SessionDTO> sessionDTOs = paginatedSessions.stream().map(this::convertToDTO).toList();

    return new PageResponseDTO<>(
        sessionDTOs,
        new PageResponseDTO.PageInfoDTO(
            page, size, (int) Math.ceil((double) sessions.size() / size), sessions.size()));
  }

  @Override
  public PageResponseDTO<SessionDTO> getSessionsByDateRange(
      LocalDate startDate,
      LocalDate endDate,
      int page,
      int size,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=get_sessions_by_date_range startDate={} endDate={} page={} size={}"
            + " requestingUserId={} organizationId={}",
        startDate,
        endDate,
        page,
        size,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Get all sessions and filter by date range
    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("organizationId = :organizationId")
            .withExpressionAttributeValues(
                java.util.Map.of(":organizationId", new AttributeValue(organizationId)));
    List<Session> allSessions = dynamoDBMapper.scan(Session.class, scanExpression);

    // Get user type for permission checking
    User requestingUser = getUser(requestingUserId);

    // Filter by date range and permissions
    List<Session> filteredSessions =
        allSessions.stream()
            .filter(
                session -> {
                  if (session.getSessionDate().isBefore(startDate)
                      || session.getSessionDate().isAfter(endDate)) {
                    return false;
                  }

                  // Check permissions based on user type
                  Batch batch = getBatch(session.getBatchId(), organizationId);
                  if (batch == null) return false;

                  if (UserType.COACH.name().equals(requestingUser.getUserType())) {
                    // Coach can only see their own batches
                    return batch.getCoachId().equals(requestingUserId);
                  } else {
                    // Student can see sessions for batches they're enrolled in
                    return enrollmentService.isStudentEnrolled(
                        batch.getBatchId(), requestingUserId, organizationId);
                  }
                })
            .sorted(
                (s1, s2) -> s2.getSessionDate().compareTo(s1.getSessionDate())) // Most recent first
            .toList();

    // Apply pagination
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, filteredSessions.size());
    List<Session> paginatedSessions = filteredSessions.subList(startIndex, endIndex);

    List<SessionDTO> sessionDTOs = paginatedSessions.stream().map(this::convertToDTO).toList();

    return new PageResponseDTO<>(
        sessionDTOs,
        new PageResponseDTO.PageInfoDTO(
            page,
            size,
            (int) Math.ceil((double) filteredSessions.size() / size),
            filteredSessions.size()));
  }

  @Override
  public SessionDTO updateSession(
      String sessionId,
      SessionUpdateRequest request,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=update_session sessionId={} requestingUserId={} organizationId={}",
        sessionId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);
    validationUtil.requireCoachPermission(requestingUserId, organizationId);

    // Get existing session
    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("sessionId = :sessionId AND organizationId = :organizationId")
            .withExpressionAttributeValues(
                java.util.Map.of(
                    ":sessionId", new AttributeValue(sessionId),
                    ":organizationId", new AttributeValue(organizationId)));

    List<Session> sessions = dynamoDBMapper.scan(Session.class, scanExpression);
    if (sessions.isEmpty()) {
      throw UserException.validationError("Session not found: " + sessionId);
    }

    Session session = sessions.get(0);

    // Validate permissions
    Batch batch = getBatch(session.getBatchId(), organizationId);
    if (batch == null) {
      throw UserException.validationError("Batch not found: " + session.getBatchId());
    }
    validateCoachPermissions(batch.getCoachId(), requestingUserId);

    // Update session
    if (request.getStatus() != null) {
      session.setStatus(request.getStatus());
    }
    if (request.getCoachId() != null) {
      session.setCoachId(request.getCoachId());
    }
    if (request.getNotes() != null) {
      session.setNotes(request.getNotes());
    }
    session.setUpdatedAt(LocalDateTime.now());

    dynamoDBMapper.save(session);

    // Log activity
    try {
      activityLogService.logAction(
          ActionType.UPDATE_SESSION,
          requestingUserId,
          getUser(requestingUserId).getName(),
          UserType.COACH,
          String.format("Updated session: %s", sessionId),
          EntityType.SESSION,
          sessionId,
          organizationId,
          String.format("Session on %s", session.getSessionDate()));
    } catch (Exception e) {
      log.warn("Failed to log session update activity for session: {}", sessionId, e);
    }

    return convertToDTO(session);
  }

  @Override
  public void deleteSession(String sessionId, String requestingUserId, String organizationId) {
    log.info(
        "evt=delete_session sessionId={} requestingUserId={} organizationId={}",
        sessionId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);
    validationUtil.requireCoachPermission(requestingUserId, organizationId);

    // Get existing session
    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("sessionId = :sessionId AND organizationId = :organizationId")
            .withExpressionAttributeValues(
                java.util.Map.of(
                    ":sessionId", new AttributeValue(sessionId),
                    ":organizationId", new AttributeValue(organizationId)));

    List<Session> sessions = dynamoDBMapper.scan(Session.class, scanExpression);
    if (sessions.isEmpty()) {
      throw UserException.validationError("Session not found: " + sessionId);
    }

    Session session = sessions.get(0);

    // Validate permissions
    Batch batch = getBatch(session.getBatchId(), organizationId);
    if (batch == null) {
      throw UserException.validationError("Batch not found: " + session.getBatchId());
    }
    validateCoachPermissions(batch.getCoachId(), requestingUserId);

    // Delete session
    dynamoDBMapper.delete(session);

    // Log activity
    try {
      activityLogService.logAction(
          ActionType.DELETE_SESSION,
          requestingUserId,
          getUser(requestingUserId).getName(),
          UserType.COACH,
          String.format("Deleted session: %s", sessionId),
          EntityType.SESSION,
          sessionId,
          organizationId,
          String.format("Session on %s", session.getSessionDate()));
    } catch (Exception e) {
      log.warn("Failed to log session deletion activity for session: {}", sessionId, e);
    }
  }

  @Override
  public List<SessionDTO> generateSessionsForBatch(
      String batchId,
      LocalDate startDate,
      LocalDate endDate,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=generate_sessions_for_batch batchId={} startDate={} endDate={} requestingUserId={}"
            + " organizationId={}",
        batchId,
        startDate,
        endDate,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);
    validationUtil.requireCoachPermission(requestingUserId, organizationId);

    // Validate batch exists and permissions
    Batch batch = getBatch(batchId, organizationId);
    if (batch == null) {
      throw UserException.validationError("Batch not found: " + batchId);
    }
    validateCoachPermissions(batch.getCoachId(), requestingUserId);

    List<SessionDTO> generatedSessions = new ArrayList<>();
    LocalDate currentDate = startDate;

    while (!currentDate.isAfter(endDate)) {
      // Check if this date matches any of the batch's days
      String dayOfWeek =
          currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase();

      if (batch.getBatchTiming().getDaysOfWeek().contains(dayOfWeek)) {
        // Create session for this date
        Session session =
            Session.builder()
                .sessionId("SESSION_" + UUID.randomUUID().toString().replace("-", ""))
                .organizationId(organizationId)
                .batchId(batchId)
                .sessionDate(currentDate)
                .startTime(batch.getBatchTiming().getStartTime())
                .endTime(batch.getBatchTiming().getEndTime())
                .status(Session.SessionStatus.SCHEDULED)
                .coachId(batch.getCoachId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        dynamoDBMapper.save(session);
        generatedSessions.add(convertToDTO(session));
      }

      currentDate = currentDate.plusDays(1);
    }

    // Log activity
    try {
      activityLogService.logAction(
          ActionType.BULK_OPERATION,
          requestingUserId,
          getUser(requestingUserId).getName(),
          UserType.COACH,
          String.format(
              "Generated %d sessions for batch: %s",
              generatedSessions.size(), batch.getBatchName()),
          EntityType.SESSION,
          null,
          organizationId,
          null);
    } catch (Exception e) {
      log.warn("Failed to log session generation activity for batch: {}", batchId, e);
    }

    return generatedSessions;
  }

  private SessionDTO convertToDTO(Session session) {
    // Get batch to include batch name
    Batch batch = getBatch(session.getBatchId(), session.getOrganizationId());
    String batchName = batch != null ? batch.getBatchName() : "Unknown Batch";

    return new SessionDTO(
        session.getSessionId(),
        session.getBatchId(),
        batchName,
        session.getSessionDate(),
        session.getStartTime(),
        session.getEndTime(),
        session.getStatus(),
        session.getCoachId(),
        session.getNotes(),
        session.getCreatedAt(),
        session.getUpdatedAt());
  }

  private Batch getBatch(String batchId, String organizationId) {
    return dynamoDBMapper.load(Batch.class, organizationId, batchId);
  }

  private com.pjariwala.model.User getUser(String userId) {
    try {
      // Try both COACH and STUDENT types since we don't know which type the user is
      User user = userService.getUserById(userId, UserType.COACH.name());
      if (user == null) {
        user = userService.getUserById(userId, UserType.STUDENT.name());
      }
      if (user == null) {
        log.error("evt=get_user_error userId={} msg=user_not_found", userId);
        throw UserException.validationError("User not found: " + userId);
      }
      return user;
    } catch (Exception e) {
      log.error("evt=get_user_error userId={}", userId, e);
      throw UserException.validationError("Failed to get user: " + userId);
    }
  }

  private void validateCoachPermissions(String batchCoachId, String requestingUserId) {
    if (!batchCoachId.equals(requestingUserId)) {
      throw UserException.validationError(
          "Access denied: You can only manage sessions for your own batches");
    }
  }
}
