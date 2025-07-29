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
import com.pjariwala.model.Attendance;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.AttendanceService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

  @Autowired private DynamoDBMapper dynamoDBMapper;

  @Autowired private ActivityLogService activityLogService;

  @Override
  public AttendanceDTO markAttendance(
      AttendanceMarkRequest request, String coachId, String userType) {
    log.info(
        "evt=mark_attendance sessionId={} studentId={} coachId={} userType={}",
        request.getSessionId(),
        request.getStudentId(),
        coachId,
        userType);

    // Validate that the requesting user is a coach
    if (!"COACH".equals(userType)) {
      log.error("evt=unauthorized_attendance_mark coachId={} userType={}", coachId, userType);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can mark attendance");
    }

    // Set the coach ID from the authenticated user
    request.setMarkedByCoachId(coachId);

    // Check if attendance already exists
    Attendance existingAttendance =
        getAttendanceByStudentAndSessionInternal(request.getStudentId(), request.getSessionId());

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
        "Attendance Record");

    log.info(
        "evt=attendance_saved sessionId={} studentId={}",
        request.getSessionId(),
        request.getStudentId());

    return convertToDTO(attendance);
  }

  @Override
  public PageResponseDTO<AttendanceDTO> getAttendanceByStudent(
      String studentId, int page, int size) {
    log.info("evt=get_attendance_by_student studentId={} page={} size={}", studentId, page, size);

    // Create a scan expression to find all attendance records for the student
    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("studentId = :studentId")
            .withExpressionAttributeValues(
                Map.of(":studentId", new AttributeValue().withS(studentId)));

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

    return response;
  }

  @Override
  public PageResponseDTO<AttendanceDTO> getAttendanceBySession(
      String sessionId, int page, int size) {
    log.info("evt=get_attendance_by_session sessionId={} page={} size={}", sessionId, page, size);

    // Create a query expression to find all attendance records for the session
    DynamoDBQueryExpression<Attendance> queryExpression =
        new DynamoDBQueryExpression<Attendance>()
            .withHashKeyValues(Attendance.builder().sessionId(sessionId).build());

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

    return response;
  }

  @Override
  public AttendanceDTO getAttendanceByStudentAndSession(String studentId, String sessionId) {
    log.info("evt=get_attendance studentId={} sessionId={}", studentId, sessionId);

    Attendance attendance = getAttendanceByStudentAndSessionInternal(studentId, sessionId);

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
      String userType) {
    log.info(
        "evt=update_attendance sessionId={} studentId={} coachId={} userType={}",
        sessionId,
        studentId,
        coachId,
        userType);

    // Validate that the requesting user is a coach
    if (!"COACH".equals(userType)) {
      log.error("evt=unauthorized_attendance_update coachId={} userType={}", coachId, userType);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can update attendance");
    }

    Attendance attendance = getAttendanceByStudentAndSessionInternal(studentId, sessionId);

    if (attendance == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found");
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
        "Attendance Record");

    log.info("evt=attendance_updated sessionId={} studentId={}", sessionId, studentId);

    return convertToDTO(attendance);
  }

  @Override
  public void deleteAttendance(
      String sessionId, String studentId, String coachId, String userType) {
    log.info(
        "evt=delete_attendance sessionId={} studentId={} coachId={} userType={}",
        sessionId,
        studentId,
        coachId,
        userType);

    // Validate that the requesting user is a coach
    if (!"COACH".equals(userType)) {
      log.error("evt=unauthorized_attendance_delete coachId={} userType={}", coachId, userType);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can delete attendance");
    }

    Attendance attendance = getAttendanceByStudentAndSessionInternal(studentId, sessionId);

    if (attendance == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found");
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
        "Attendance Record");

    log.info("evt=attendance_deleted sessionId={} studentId={}", sessionId, studentId);
  }

  private Attendance getAttendanceByStudentAndSessionInternal(String studentId, String sessionId) {
    // Create a query expression to find the specific attendance record
    DynamoDBQueryExpression<Attendance> queryExpression =
        new DynamoDBQueryExpression<Attendance>()
            .withHashKeyValues(Attendance.builder().sessionId(sessionId).build())
            .withFilterExpression("studentId = :studentId")
            .withExpressionAttributeValues(
                Map.of(":studentId", new AttributeValue().withS(studentId)));

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
}
