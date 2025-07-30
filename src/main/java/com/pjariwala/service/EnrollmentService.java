package com.pjariwala.service;

import com.pjariwala.dto.EnrollmentRequestDTO;
import com.pjariwala.dto.EnrollmentResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import java.util.List;
import java.util.Optional;

public interface EnrollmentService {

  /** Create a new enrollment - only coaches can enroll students */
  EnrollmentResponseDTO createEnrollment(
      EnrollmentRequestDTO enrollmentRequest, String requestingUserId, String organizationId);

  /** Create multiple enrollments in bulk - only coaches can enroll students */
  List<EnrollmentResponseDTO> createBulkEnrollments(
      List<EnrollmentRequestDTO> enrollmentRequests,
      String requestingUserId,
      String organizationId);

  /** Get enrollment by batchId and studentId */
  Optional<EnrollmentResponseDTO> getEnrollment(
      String batchId, String studentId, String requestingUserId, String organizationId);

  /** Get all enrollments for a specific batch */
  PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByBatch(
      String batchId, int page, int size, String requestingUserId, String organizationId);

  /** Get all enrollments for a specific student */
  PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByStudent(
      String studentId, int page, int size, String requestingUserId, String organizationId);

  /** Update an existing enrollment - only coaches can update enrollments */
  Optional<EnrollmentResponseDTO> updateEnrollment(
      String batchId,
      String studentId,
      EnrollmentRequestDTO enrollmentRequest,
      String requestingUserId,
      String organizationId);

  /** Delete an enrollment - only coaches can delete enrollments */
  boolean deleteEnrollment(
      String batchId, String studentId, String requestingUserId, String organizationId);

  /** Check if a student is enrolled in a batch */
  boolean isStudentEnrolled(String batchId, String studentId, String organizationId);

  /** Get all enrollments with optional filtering */
  PageResponseDTO<EnrollmentResponseDTO> getAllEnrollments(
      Optional<String> batchId,
      Optional<String> studentId,
      int page,
      int size,
      String requestingUserId,
      String organizationId);

  /** Count active enrollments for a batch */
  int countActiveEnrollmentsByBatch(String batchId, String organizationId);

  /** Get all batch IDs that a student is enrolled in */
  List<String> getEnrolledBatchIdsForStudent(String studentId, String organizationId);
}
