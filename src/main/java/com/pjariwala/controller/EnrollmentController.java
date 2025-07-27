package com.pjariwala.controller;

import com.pjariwala.dto.EnrollmentRequestDTO;
import com.pjariwala.dto.EnrollmentResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.model.Enrollment;
import com.pjariwala.service.EnrollmentService;
import com.pjariwala.util.AuthUtil;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@CrossOrigin(origins = "*")
@Slf4j
public class EnrollmentController {

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private AuthUtil authUtil;

  @PostMapping
  public ResponseEntity<EnrollmentResponseDTO> createEnrollment(
      @RequestBody EnrollmentRequestDTO enrollmentRequest,
      @RequestHeader("Authorization") String authorization) {
    log.info(
        "evt=create_enrollment_request batchId={} studentId={}",
        enrollmentRequest.getBatchId(),
        enrollmentRequest.getStudentId());
    try {
      // Only coaches can enroll students
      authUtil.requireCoach(authorization);

      EnrollmentResponseDTO response = enrollmentService.createEnrollment(enrollmentRequest);
      log.info(
          "evt=create_enrollment_success batchId={} studentId={}",
          response.getBatchId(),
          response.getStudentId());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      log.error(
          "evt=create_enrollment_error batchId={} studentId={}",
          enrollmentRequest.getBatchId(),
          enrollmentRequest.getStudentId(),
          e);
      throw e;
    }
  }

  @GetMapping("/{batchId}/{studentId}")
  public ResponseEntity<EnrollmentResponseDTO> getEnrollment(
      @PathVariable String batchId, @PathVariable String studentId) {
    log.info("evt=get_enrollment_request received batchId={} studentId={}", batchId, studentId);
    try {
      Optional<EnrollmentResponseDTO> enrollment =
          enrollmentService.getEnrollment(batchId, studentId);
      if (enrollment.isPresent()) {
        log.info("evt=get_enrollment_response success batchId={} studentId={}", batchId, studentId);
        return ResponseEntity.ok(enrollment.get());
      } else {
        log.info(
            "evt=get_enrollment_response not_found batchId={} studentId={}", batchId, studentId);
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      log.error("evt=get_enrollment_response error batchId={} studentId={}", batchId, studentId, e);
      throw e;
    }
  }

  @GetMapping
  public ResponseEntity<PageResponseDTO<EnrollmentResponseDTO>> getAllEnrollments(
      @RequestParam(required = false) String enrollmentStatus,
      @RequestParam(required = false) String paymentStatus,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info(
        "evt=get_all_enrollments_request received page={} size={} enrollmentStatus={}"
            + " paymentStatus={}",
        page,
        size,
        enrollmentStatus,
        paymentStatus);
    try {
      Optional<Enrollment.EnrollmentStatus> statusEnum = Optional.empty();
      if (enrollmentStatus != null) {
        try {
          statusEnum =
              Optional.of(Enrollment.EnrollmentStatus.valueOf(enrollmentStatus.toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn(
              "evt=get_all_enrollments_request invalid_enrollment_status status={}",
              enrollmentStatus);
        }
      }

      Optional<Enrollment.PaymentStatus> paymentStatusEnum = Optional.empty();
      if (paymentStatus != null) {
        try {
          paymentStatusEnum =
              Optional.of(Enrollment.PaymentStatus.valueOf(paymentStatus.toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn(
              "evt=get_all_enrollments_request invalid_payment_status paymentStatus={}",
              paymentStatus);
        }
      }

      PageResponseDTO<EnrollmentResponseDTO> response =
          enrollmentService.getAllEnrollments(statusEnum, paymentStatusEnum, page, size);
      log.info(
          "evt=get_all_enrollments_response success totalElements={}",
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=get_all_enrollments_response error", e);
      throw e;
    }
  }

  @GetMapping("/batch/{batchId}")
  public ResponseEntity<PageResponseDTO<EnrollmentResponseDTO>> getEnrollmentsByBatch(
      @PathVariable String batchId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info(
        "evt=get_enrollments_by_batch_request received batchId={} page={} size={}",
        batchId,
        page,
        size);
    try {
      PageResponseDTO<EnrollmentResponseDTO> response =
          enrollmentService.getEnrollmentsByBatch(batchId, page, size);
      log.info(
          "evt=get_enrollments_by_batch_response success batchId={} totalElements={}",
          batchId,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=get_enrollments_by_batch_response error batchId={}", batchId, e);
      throw e;
    }
  }

  @GetMapping("/student/{studentId}")
  public ResponseEntity<PageResponseDTO<EnrollmentResponseDTO>> getEnrollmentsByStudent(
      @PathVariable String studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info(
        "evt=get_enrollments_by_student_request received studentId={} page={} size={}",
        studentId,
        page,
        size);
    try {
      PageResponseDTO<EnrollmentResponseDTO> response =
          enrollmentService.getEnrollmentsByStudent(studentId, page, size);
      log.info(
          "evt=get_enrollments_by_student_response success studentId={} totalElements={}",
          studentId,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=get_enrollments_by_student_response error studentId={}", studentId, e);
      throw e;
    }
  }

  @PutMapping("/{batchId}/{studentId}")
  public ResponseEntity<EnrollmentResponseDTO> updateEnrollment(
      @PathVariable String batchId,
      @PathVariable String studentId,
      @RequestBody EnrollmentRequestDTO enrollmentRequest) {
    log.info("evt=update_enrollment_request received batchId={} studentId={}", batchId, studentId);
    try {
      EnrollmentResponseDTO response =
          enrollmentService.updateEnrollment(batchId, studentId, enrollmentRequest);
      log.info(
          "evt=update_enrollment_response success batchId={} studentId={}", batchId, studentId);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error(
          "evt=update_enrollment_response error batchId={} studentId={}", batchId, studentId, e);
      throw e;
    }
  }

  @DeleteMapping("/{batchId}/{studentId}")
  public ResponseEntity<Void> deleteEnrollment(
      @PathVariable String batchId, @PathVariable String studentId) {
    log.info("evt=delete_enrollment_request received batchId={} studentId={}", batchId, studentId);
    try {
      enrollmentService.deleteEnrollment(batchId, studentId);
      log.info(
          "evt=delete_enrollment_response success batchId={} studentId={}", batchId, studentId);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error(
          "evt=delete_enrollment_response error batchId={} studentId={}", batchId, studentId, e);
      throw e;
    }
  }

  @PatchMapping("/{batchId}/{studentId}/payment")
  public ResponseEntity<EnrollmentResponseDTO> updatePaymentStatus(
      @PathVariable String batchId,
      @PathVariable String studentId,
      @RequestParam String paymentStatus,
      @RequestParam(required = false) Double paymentAmount) {
    log.info(
        "evt=update_payment_status_request received batchId={} studentId={} paymentStatus={}"
            + " amount={}",
        batchId,
        studentId,
        paymentStatus,
        paymentAmount);
    try {
      Enrollment.PaymentStatus statusEnum;
      try {
        statusEnum = Enrollment.PaymentStatus.valueOf(paymentStatus.toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn(
            "evt=update_payment_status_request invalid_payment_status paymentStatus={}",
            paymentStatus);
        return ResponseEntity.badRequest().build();
      }

      EnrollmentResponseDTO response =
          enrollmentService.updatePaymentStatus(batchId, studentId, statusEnum, paymentAmount);
      log.info(
          "evt=update_payment_status_response success batchId={} studentId={}", batchId, studentId);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error(
          "evt=update_payment_status_response error batchId={} studentId={}",
          batchId,
          studentId,
          e);
      throw e;
    }
  }

  @GetMapping("/payment-status/{paymentStatus}")
  public ResponseEntity<PageResponseDTO<EnrollmentResponseDTO>> getEnrollmentsByPaymentStatus(
      @PathVariable String paymentStatus,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info(
        "evt=get_enrollments_by_payment_status_request received paymentStatus={} page={} size={}",
        paymentStatus,
        page,
        size);
    try {
      Enrollment.PaymentStatus statusEnum;
      try {
        statusEnum = Enrollment.PaymentStatus.valueOf(paymentStatus.toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn(
            "evt=get_enrollments_by_payment_status_request invalid_payment_status paymentStatus={}",
            paymentStatus);
        return ResponseEntity.badRequest().build();
      }

      PageResponseDTO<EnrollmentResponseDTO> response =
          enrollmentService.getEnrollmentsByPaymentStatus(statusEnum, page, size);
      log.info(
          "evt=get_enrollments_by_payment_status_response success paymentStatus={}"
              + " totalElements={}",
          paymentStatus,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error(
          "evt=get_enrollments_by_payment_status_response error paymentStatus={}",
          paymentStatus,
          e);
      throw e;
    }
  }

  // Alternative endpoints for better RESTful design

  @GetMapping("/batches/{batchId}/enrollments")
  public ResponseEntity<PageResponseDTO<EnrollmentResponseDTO>> getBatchEnrollments(
      @PathVariable String batchId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return getEnrollmentsByBatch(batchId, page, size);
  }

  @GetMapping("/students/{studentId}/enrollments")
  public ResponseEntity<PageResponseDTO<EnrollmentResponseDTO>> getStudentEnrollments(
      @PathVariable String studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return getEnrollmentsByStudent(studentId, page, size);
  }
}
