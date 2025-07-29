package com.pjariwala.controller;

import com.pjariwala.dto.AttendanceDTO;
import com.pjariwala.dto.AttendanceMarkRequest;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/api/v1/attendance")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Attendance Management", description = "APIs for managing student attendance")
public class AttendanceController {

  @Autowired private AttendanceService attendanceService;

  @Autowired private ActivityLogService activityLogService;

  @PostMapping
  @Operation(
      summary = "Mark student attendance for a session",
      description = "Allows a coach to mark a student's presence or absence for a specific session")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<AttendanceDTO> markAttendance(
      @Parameter(hidden = true) @RequestAttribute("userId") String coachId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @RequestBody AttendanceMarkRequest request) {

    log.info(
        "evt=attendance_mark_request coachId={} sessionId={} studentId={}",
        coachId,
        request.getSessionId(),
        request.getStudentId());

    AttendanceDTO attendance = attendanceService.markAttendance(request, coachId, userType);

    log.info(
        "evt=attendance_marked sessionId={} studentId={}",
        request.getSessionId(),
        request.getStudentId());

    return ResponseEntity.ok(attendance);
  }

  @GetMapping("/student/{studentId}")
  @Operation(
      summary = "Get attendance history for a student",
      description = "Retrieves all attendance records for a given student")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<AttendanceDTO>> getAttendanceByStudent(
      @PathVariable String studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    log.info("evt=get_attendance_history studentId={} page={} size={}", studentId, page, size);

    PageResponseDTO<AttendanceDTO> attendance =
        attendanceService.getAttendanceByStudent(studentId, page, size);

    return ResponseEntity.ok(attendance);
  }

  @GetMapping("/session/{sessionId}")
  @Operation(
      summary = "Get attendance for a specific session",
      description = "Retrieves all attendance records for a specific session")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<AttendanceDTO>> getAttendanceBySession(
      @PathVariable String sessionId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    log.info("evt=get_session_attendance sessionId={} page={} size={}", sessionId, page, size);

    PageResponseDTO<AttendanceDTO> attendance =
        attendanceService.getAttendanceBySession(sessionId, page, size);

    return ResponseEntity.ok(attendance);
  }

  @GetMapping("/student/{studentId}/session/{sessionId}")
  @Operation(
      summary = "Get attendance for a specific student and session",
      description = "Retrieves attendance record for a specific student in a specific session")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<AttendanceDTO> getAttendanceByStudentAndSession(
      @PathVariable String studentId, @PathVariable String sessionId) {

    log.info("evt=get_student_session_attendance studentId={} sessionId={}", studentId, sessionId);

    AttendanceDTO attendance =
        attendanceService.getAttendanceByStudentAndSession(studentId, sessionId);

    if (attendance == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(attendance);
  }

  @PutMapping("/session/{sessionId}/student/{studentId}")
  @Operation(
      summary = "Update attendance record",
      description = "Update an existing attendance record")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<AttendanceDTO> updateAttendance(
      @Parameter(hidden = true) @RequestAttribute("userId") String coachId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @PathVariable String sessionId,
      @PathVariable String studentId,
      @RequestBody AttendanceMarkRequest request) {

    log.info(
        "evt=update_attendance_request coachId={} sessionId={} studentId={}",
        coachId,
        sessionId,
        studentId);

    AttendanceDTO attendance =
        attendanceService.updateAttendance(sessionId, studentId, request, coachId, userType);

    log.info("evt=attendance_updated sessionId={} studentId={}", sessionId, studentId);

    return ResponseEntity.ok(attendance);
  }

  @DeleteMapping("/session/{sessionId}/student/{studentId}")
  @Operation(summary = "Delete attendance record", description = "Delete an attendance record")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> deleteAttendance(
      @Parameter(hidden = true) @RequestAttribute("userId") String coachId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @PathVariable String sessionId,
      @PathVariable String studentId) {

    log.info(
        "evt=delete_attendance_request coachId={} sessionId={} studentId={}",
        coachId,
        sessionId,
        studentId);

    attendanceService.deleteAttendance(sessionId, studentId, coachId, userType);

    log.info("evt=attendance_deleted sessionId={} studentId={}", sessionId, studentId);

    return ResponseEntity.ok().build();
  }
}
