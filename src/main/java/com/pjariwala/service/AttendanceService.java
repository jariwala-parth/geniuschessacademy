package com.pjariwala.service;

import com.pjariwala.dto.AttendanceDTO;
import com.pjariwala.dto.AttendanceMarkRequest;
import com.pjariwala.dto.PageResponseDTO;

public interface AttendanceService {

  /** Mark attendance for a student in a session */
  AttendanceDTO markAttendance(AttendanceMarkRequest request, String coachId, String userType);

  /** Get attendance history for a student */
  PageResponseDTO<AttendanceDTO> getAttendanceByStudent(String studentId, int page, int size);

  /** Get attendance for a specific session */
  PageResponseDTO<AttendanceDTO> getAttendanceBySession(String sessionId, int page, int size);

  /** Get attendance by student and session */
  AttendanceDTO getAttendanceByStudentAndSession(String studentId, String sessionId);

  /** Update attendance record */
  AttendanceDTO updateAttendance(
      String sessionId,
      String studentId,
      AttendanceMarkRequest request,
      String coachId,
      String userType);

  /** Delete attendance record */
  void deleteAttendance(String sessionId, String studentId, String coachId, String userType);
}
