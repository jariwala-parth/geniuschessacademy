package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.AttendanceDTO;
import com.pjariwala.dto.AttendanceMarkRequest;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.AuthException;
import com.pjariwala.model.Attendance;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.AttendanceService;
import com.pjariwala.service.UserService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

  @Autowired private DynamoDBMapper dynamoDBMapper;
  @Autowired private ActivityLogService activityLogService;
  @Autowired private UserService userService;

  @Override
  public AttendanceDTO markAttendance(
      AttendanceMarkRequest request, String coachId, String organizationId) {
    log.info(
        "evt=mark_attendance sessionId={} studentId={} coachId={} organizationId={}",
        request.getSessionId(),
        request.getStudentId(),
        coachId,
        organizationId);

    // Validate organization access
    validateOrganizationAccess(coachId, organizationId);

    // Validate that the requesting user is a coach
    requireCoachPermission(coachId, organizationId);

    // Set the coach ID from the authenticated user
    request.setMarkedByCoachId(coachId);

    // Check if attendance already exists
    Attendance existingAttendance =
        getAttendanceByStudentAndSessionInternal(
            request.getStudentId(), request.getSessionId(), organizationId);

    Attendance attendance;
    if (existingAttendance != null) {
      // Update existing attendance
      existingAttendance.setIsPresent(request.getIsPresent());
      existingAttendance.setMarkedByCoachId(request.getMarkedByCoachId());
      existingAttendance.setNotes(request.getNotes());
      attendance = existingAttendance;
    } else {
      // Create new attendance
      attendance =
          Attendance.builder()
              .organizationId(organizationId)
              .sessionId(request.getSessionId())
              .studentId(request.getStudentId())
              .isPresent(request.getIsPresent())
              .markedByCoachId(request.getMarkedByCoachId())
              .markedAt(LocalDateTime.now())
              .notes(request.getNotes())
              .build();
    }

    dynamoDBMapper.save(attendance);

    // Log the activity
    activityLogService.logAction(
        ActionType.MARK_ATTENDANCE,
        coachId,
        "Coach", // We'll need to get the actual coach name
        UserType.COACH,
        "Marked attendance for student "
            + attendance.getStudentId()
            + " in session "
            + attendance.getSessionId(),
        EntityType.ATTENDANCE,
        attendance.getSessionId() + "_" + attendance.getStudentId(),
        "Attendance Record",
        organizationId);

    log.info(
        "evt=attendance_saved sessionId={} studentId={} organizationId={}",
        request.getSessionId(),
        request.getStudentId(),
        organizationId);

    return convertToDTO(attendance);
  }

  @Override
  public PageResponseDTO<AttendanceDTO> getAttendanceByStudent(
      String studentId, int page, int size, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_attendance_by_student studentId={} requestingUserId={} organizationId={} page={}"
            + " size={}",
        studentId,
        requestingUserId,
        organizationId,
        page,
        size);

    // Validate organization access
    validateOrganizationAccess(requestingUserId, organizationId);

    // Validate user access - users can only see their own attendance, coaches can see any student's
    // attendance
    validateUserAccess(requestingUserId, studentId, organizationId);

    // Create a scan expression to find all attendance records for the student
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":organizationId", new AttributeValue().withS(organizationId));
    eav.put(":studentId", new AttributeValue().withS(studentId));

    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("organizationId = :organizationId AND studentId = :studentId")
            .withExpressionAttributeValues(eav);

    List<Attendance> attendances = dynamoDBMapper.scan(Attendance.class, scanExpression);

    // Apply pagination
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, attendances.size());
    List<Attendance> paginatedAttendances = attendances.subList(startIndex, endIndex);

    List<AttendanceDTO> attendanceDTOs =
        paginatedAttendances.stream().map(this::convertToDTO).collect(Collectors.toList());

    PageResponseDTO.PageInfoDTO pageInfo = new PageResponseDTO.PageInfoDTO();
    pageInfo.setCurrentPage(page);
    pageInfo.setPageSize(size);
    pageInfo.setTotalElements((long) attendances.size());
    pageInfo.setTotalPages((int) Math.ceil((double) attendances.size() / size));

    PageResponseDTO<AttendanceDTO> response = new PageResponseDTO<>();
    response.setContent(attendanceDTOs);
    response.setPageInfo(pageInfo);

    log.info(
        "evt=get_attendance_by_student_success studentId={} requestingUserId={} organizationId={}"
            + " count={}",
        studentId,
        requestingUserId,
        organizationId,
        attendanceDTOs.size());

    return response;
  }

  @Override
  public PageResponseDTO<AttendanceDTO> getAttendanceBySession(
      String sessionId, int page, int size, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_attendance_by_session sessionId={} requestingUserId={} organizationId={} page={}"
            + " size={}",
        sessionId,
        requestingUserId,
        organizationId,
        page,
        size);

    // Validate organization access
    validateOrganizationAccess(requestingUserId, organizationId);

    // Only coaches can view session attendance
    requireCoachPermission(requestingUserId, organizationId);

    // Create a query expression to find all attendance records for the session
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":organizationId", new AttributeValue().withS(organizationId));
    eav.put(":sessionId", new AttributeValue().withS(sessionId));

    DynamoDBQueryExpression<Attendance> queryExpression =
        new DynamoDBQueryExpression<Attendance>()
            .withKeyConditionExpression(
                "organizationId = :organizationId AND sessionId = :sessionId")
            .withExpressionAttributeValues(eav);

    List<Attendance> attendances = dynamoDBMapper.query(Attendance.class, queryExpression);

    // Apply pagination
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, attendances.size());
    List<Attendance> paginatedAttendances = attendances.subList(startIndex, endIndex);

    List<AttendanceDTO> attendanceDTOs =
        paginatedAttendances.stream().map(this::convertToDTO).collect(Collectors.toList());

    PageResponseDTO.PageInfoDTO pageInfo = new PageResponseDTO.PageInfoDTO();
    pageInfo.setCurrentPage(page);
    pageInfo.setPageSize(size);
    pageInfo.setTotalElements((long) attendances.size());
    pageInfo.setTotalPages((int) Math.ceil((double) attendances.size() / size));

    PageResponseDTO<AttendanceDTO> response = new PageResponseDTO<>();
    response.setContent(attendanceDTOs);
    response.setPageInfo(pageInfo);

    log.info(
        "evt=get_attendance_by_session_success sessionId={} requestingUserId={} organizationId={}"
            + " count={}",
        sessionId,
        requestingUserId,
        organizationId,
        attendanceDTOs.size());

    return response;
  }

  @Override
  public AttendanceDTO getAttendanceByStudentAndSession(
      String studentId, String sessionId, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_attendance studentId={} sessionId={} requestingUserId={} organizationId={}",
        studentId,
        sessionId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validateOrganizationAccess(requestingUserId, organizationId);

    // Validate user access - users can only see their own attendance, coaches can see any student's
    // attendance
    validateUserAccess(requestingUserId, studentId, organizationId);

    Attendance attendance =
        getAttendanceByStudentAndSessionInternal(studentId, sessionId, organizationId);

    if (attendance == null) {
      return null;
    }

    return convertToDTO(attendance);
  }

  @Override
  public AttendanceDTO updateAttendance(
      String sessionId,
      String studentId,
      AttendanceMarkRequest request,
      String coachId,
      String organizationId) {
    log.info(
        "evt=update_attendance sessionId={} studentId={} coachId={} organizationId={}",
        sessionId,
        studentId,
        coachId,
        organizationId);

    // Validate organization access
    validateOrganizationAccess(coachId, organizationId);

    // Validate that the requesting user is a coach
    requireCoachPermission(coachId, organizationId);

    Attendance attendance =
        getAttendanceByStudentAndSessionInternal(studentId, sessionId, organizationId);

    if (attendance == null) {
      throw new AuthException("NOT_FOUND", "Attendance record not found", 404);
    }

    attendance.setIsPresent(request.getIsPresent());
    attendance.setMarkedByCoachId(coachId);
    attendance.setNotes(request.getNotes());

    dynamoDBMapper.save(attendance);

    // Log the activity
    activityLogService.logAction(
        ActionType.MARK_ATTENDANCE,
        coachId,
        "Coach", // We'll need to get the actual coach name
        UserType.COACH,
        "Updated attendance for student " + studentId + " in session " + sessionId,
        EntityType.ATTENDANCE,
        sessionId + "_" + studentId,
        "Attendance Record",
        organizationId);

    log.info(
        "evt=attendance_updated sessionId={} studentId={} organizationId={}",
        sessionId,
        studentId,
        organizationId);

    return convertToDTO(attendance);
  }

  @Override
  public void deleteAttendance(
      String sessionId, String studentId, String coachId, String organizationId) {
    log.info(
        "evt=delete_attendance sessionId={} studentId={} coachId={} organizationId={}",
        sessionId,
        studentId,
        coachId,
        organizationId);

    // Validate organization access
    validateOrganizationAccess(coachId, organizationId);

    // Validate that the requesting user is a coach
    requireCoachPermission(coachId, organizationId);

    Attendance attendance =
        getAttendanceByStudentAndSessionInternal(studentId, sessionId, organizationId);

    if (attendance == null) {
      throw new AuthException("NOT_FOUND", "Attendance record not found", 404);
    }

    dynamoDBMapper.delete(attendance);

    // Log the activity
    activityLogService.logAction(
        ActionType.SYSTEM_ACTION,
        coachId,
        "Coach", // We'll need to get the actual coach name
        UserType.COACH,
        "Deleted attendance for student " + studentId + " in session " + sessionId,
        EntityType.ATTENDANCE,
        sessionId + "_" + studentId,
        "Attendance Record",
        organizationId);

    log.info(
        "evt=attendance_deleted sessionId={} studentId={} organizationId={}",
        sessionId,
        studentId,
        organizationId);
  }

  private Attendance getAttendanceByStudentAndSessionInternal(
      String studentId, String sessionId, String organizationId) {
    // Create a query expression to find the specific attendance record
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":organizationId", new AttributeValue().withS(organizationId));
    eav.put(":sessionId", new AttributeValue().withS(sessionId));
    eav.put(":studentId", new AttributeValue().withS(studentId));

    DynamoDBQueryExpression<Attendance> queryExpression =
        new DynamoDBQueryExpression<Attendance>()
            .withKeyConditionExpression(
                "organizationId = :organizationId AND sessionId = :sessionId")
            .withFilterExpression("studentId = :studentId")
            .withExpressionAttributeValues(eav);

    List<Attendance> attendances = dynamoDBMapper.query(Attendance.class, queryExpression);

    if (attendances.isEmpty()) {
      return null;
    }

    return attendances.get(0);
  }

  private AttendanceDTO convertToDTO(Attendance attendance) {
    AttendanceDTO dto = new AttendanceDTO();
    dto.setSessionId(attendance.getSessionId());
    dto.setStudentId(attendance.getStudentId());
    dto.setIsPresent(attendance.getIsPresent());
    dto.setMarkedByCoachId(attendance.getMarkedByCoachId());
    dto.setMarkedAt(attendance.getMarkedAt());
    dto.setNotes(attendance.getNotes());
    return dto;
  }

  /** Validate that the requesting user has access to the organization */
  private void validateOrganizationAccess(String requestingUserId, String organizationId) {
    try {
      User user = userService.getUserById(requestingUserId).orElse(null);
      if (user == null) {
        log.error(
            "evt=validate_org_access_user_not_found requestingUserId={} organizationId={}",
            requestingUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "User not found", 403);
      }

      if (!organizationId.equals(user.getOrganizationId())) {
        log.error(
            "evt=validate_org_access_denied requestingUserId={} userOrgId={} requestedOrgId={}",
            requestingUserId,
            user.getOrganizationId(),
            organizationId);
        throw new AuthException("ACCESS_DENIED", "You can only access your own organization", 403);
      }

      log.debug(
          "evt=validate_org_access_success requestingUserId={} organizationId={}",
          requestingUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=validate_org_access_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate organization access", 403);
    }
  }

  /** Validate that the requesting user can access the target user's data */
  private void validateUserAccess(
      String requestingUserId, String targetUserId, String organizationId) {
    try {
      User requestingUser = userService.getUserById(requestingUserId).orElse(null);
      if (requestingUser == null) {
        log.error(
            "evt=validate_user_access_requesting_user_not_found requestingUserId={} targetUserId={}"
                + " organizationId={}",
            requestingUserId,
            targetUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "Requesting user not found", 403);
      }

      // Coaches can access any user's data within their organization
      if (UserType.COACH.name().equals(requestingUser.getUserType())) {
        log.debug(
            "evt=validate_user_access_coach_granted requestingUserId={} targetUserId={}"
                + " organizationId={}",
            requestingUserId,
            targetUserId,
            organizationId);
        return;
      }

      // Students can only access their own data
      if (!requestingUserId.equals(targetUserId)) {
        log.error(
            "evt=validate_user_access_student_denied requestingUserId={} targetUserId={}"
                + " organizationId={}",
            requestingUserId,
            targetUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "You can only access your own data", 403);
      }

      log.debug(
          "evt=validate_user_access_student_own_data requestingUserId={} targetUserId={}"
              + " organizationId={}",
          requestingUserId,
          targetUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=validate_user_access_error requestingUserId={} targetUserId={} organizationId={}"
              + " error={}",
          requestingUserId,
          targetUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate user access", 403);
    }
  }

  /** Require that the requesting user is a coach */
  private void requireCoachPermission(String requestingUserId, String organizationId) {
    try {
      User user = userService.getUserById(requestingUserId).orElse(null);
      if (user == null) {
        log.error(
            "evt=require_coach_permission_user_not_found requestingUserId={} organizationId={}",
            requestingUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "User not found", 403);
      }

      if (!UserType.COACH.name().equals(user.getUserType())) {
        log.error(
            "evt=require_coach_permission_denied requestingUserId={} userType={} organizationId={}",
            requestingUserId,
            user.getUserType(),
            organizationId);
        throw new AuthException("ACCESS_DENIED", "Only coaches can perform this action", 403);
      }

      log.debug(
          "evt=require_coach_permission_granted requestingUserId={} organizationId={}",
          requestingUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=require_coach_permission_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate coach permission", 403);
    }
  }
}
