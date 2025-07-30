package com.pjariwala.service;

import com.pjariwala.dto.AttendanceDTO;
import com.pjariwala.dto.AttendanceMarkRequest;
import com.pjariwala.dto.PageResponseDTO;

public interface AttendanceService {

  /** Mark attendance for a student in a session */
  AttendanceDTO markAttendance(
      AttendanceMarkRequest request, String coachId, String organizationId);

  /** Get attendance history for a student */
  PageResponseDTO<AttendanceDTO> getAttendanceByStudent(
      String studentId, int page, int size, String requestingUserId, String organizationId);

  /** Get attendance for a specific session */
  PageResponseDTO<AttendanceDTO> getAttendanceBySession(
      String sessionId, int page, int size, String requestingUserId, String organizationId);

  /** Get attendance by student and session */
  AttendanceDTO getAttendanceByStudentAndSession(
      String studentId, String sessionId, String requestingUserId, String organizationId);

  /** Update attendance record */
  AttendanceDTO updateAttendance(
      String sessionId,
      String studentId,
      AttendanceMarkRequest request,
      String coachId,
      String organizationId);

  /** Delete attendance record */
  void deleteAttendance(String sessionId, String studentId, String coachId, String organizationId);
}
