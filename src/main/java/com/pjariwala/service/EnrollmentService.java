package com.pjariwala.service;

import com.pjariwala.dto.EnrollmentRequestDTO;
import com.pjariwala.dto.EnrollmentResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.model.Enrollment;
import java.util.Optional;

public interface EnrollmentService {

  /** Create a new enrollment (enroll student in batch) */
  EnrollmentResponseDTO createEnrollment(EnrollmentRequestDTO enrollmentRequest);

  /** Get enrollment by batchId and studentId */
  Optional<EnrollmentResponseDTO> getEnrollment(String batchId, String studentId);

  /** Get all enrollments for a specific batch */
  PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByBatch(String batchId, int page, int size);

  /** Get all enrollments for a specific student */
  PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByStudent(
      String studentId, int page, int size);

  /** Update an existing enrollment */
  EnrollmentResponseDTO updateEnrollment(
      String batchId, String studentId, EnrollmentRequestDTO enrollmentRequest);

  /** Delete an enrollment (unenroll student from batch) */
  void deleteEnrollment(String batchId, String studentId);

  /** Check if a student is enrolled in a batch */
  boolean isStudentEnrolled(String batchId, String studentId);

  /** Get all enrollments with optional filtering */
  PageResponseDTO<EnrollmentResponseDTO> getAllEnrollments(
      Optional<Enrollment.EnrollmentStatus> status,
      Optional<Enrollment.PaymentStatus> paymentStatus,
      int page,
      int size);

  /** Update payment status for an enrollment */
  EnrollmentResponseDTO updatePaymentStatus(
      String batchId,
      String studentId,
      Enrollment.PaymentStatus paymentStatus,
      Double paymentAmount);

  /** Get enrollments by payment status */
  PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByPaymentStatus(
      Enrollment.PaymentStatus paymentStatus, int page, int size);
}
