package com.pjariwala.service;

import com.pjariwala.dto.EnrollmentRequestDTO;
import com.pjariwala.dto.EnrollmentResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import java.util.List;
import java.util.Optional;

public interface EnrollmentService {

  /** Create a new enrollment - only coaches can enroll students */
  EnrollmentResponseDTO createEnrollment(
      EnrollmentRequestDTO enrollmentRequest, String requestingUserId);

  /** Create multiple enrollments in bulk - only coaches can enroll students */
  List<EnrollmentResponseDTO> createBulkEnrollments(
      List<EnrollmentRequestDTO> enrollmentRequests, String requestingUserId);

  /** Get enrollment by batchId and studentId */
  Optional<EnrollmentResponseDTO> getEnrollment(
      String batchId, String studentId, String requestingUserId);

  /** Get all enrollments for a specific batch */
  PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByBatch(
      String batchId, int page, int size, String requestingUserId);

  /** Get all enrollments for a specific student */
  PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByStudent(
      String studentId, int page, int size, String requestingUserId);

  /** Update an existing enrollment - only coaches can update enrollments */
  Optional<EnrollmentResponseDTO> updateEnrollment(
      String batchId,
      String studentId,
      EnrollmentRequestDTO enrollmentRequest,
      String requestingUserId);

  /** Delete an enrollment - only coaches can delete enrollments */
  boolean deleteEnrollment(String batchId, String studentId, String requestingUserId);

  /** Check if a student is enrolled in a batch */
  boolean isStudentEnrolled(String batchId, String studentId);

  /** Get all enrollments with optional filtering */
  PageResponseDTO<EnrollmentResponseDTO> getAllEnrollments(
      Optional<String> batchId,
      Optional<String> studentId,
      int page,
      int size,
      String requestingUserId);

  /** Count active enrollments for a batch */
  int countActiveEnrollmentsByBatch(String batchId);

  /** Get all batch IDs that a student is enrolled in */
  List<String> getEnrolledBatchIdsForStudent(String studentId);
}
