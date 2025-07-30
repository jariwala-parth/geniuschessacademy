package com.pjariwala.controller;

import com.pjariwala.dto.EnrollmentRequestDTO;
import com.pjariwala.dto.EnrollmentResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/organizations/{organizationId}/enrollments")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(
    name = "Enrollment Management",
    description = "APIs for managing student enrollments in batches within organizations")
public class EnrollmentController {

  @Autowired private EnrollmentService enrollmentService;

  @PostMapping
  @Operation(
      summary = "Enroll a student in a batch",
      description =
          "Create a new enrollment within an organization. Only coaches can enroll students.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EnrollmentResponseDTO> createEnrollment(
      @PathVariable String organizationId,
      @RequestBody EnrollmentRequestDTO enrollmentRequest,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info(
        "evt=create_enrollment_request organizationId={} batchId={} studentId={}"
            + " requestingUserId={}",
        organizationId,
        enrollmentRequest.getBatchId(),
        enrollmentRequest.getStudentId(),
        userId);
    try {
      EnrollmentResponseDTO response =
          enrollmentService.createEnrollment(enrollmentRequest, userId, organizationId);
      log.info(
          "evt=create_enrollment_success organizationId={} batchId={} studentId={}",
          organizationId,
          response.getBatchId(),
          response.getStudentId());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      log.error(
          "evt=create_enrollment_error organizationId={} batchId={} studentId={}",
          organizationId,
          enrollmentRequest.getBatchId(),
          enrollmentRequest.getStudentId(),
          e);
      throw e;
    }
  }

  @PostMapping("/bulk")
  @Operation(
      summary = "Enroll multiple students in batches",
      description =
          "Create multiple enrollments in one request within an organization. Only coaches can"
              + " enroll students.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<EnrollmentResponseDTO>> createBulkEnrollments(
      @PathVariable String organizationId,
      @Parameter(hidden = true) @RequestAttribute("userId") String requestingUserId,
      @RequestBody List<EnrollmentRequestDTO> enrollmentRequests) {

    log.info(
        "evt=create_bulk_enrollments_request organizationId={} userId={} count={}",
        organizationId,
        requestingUserId,
        enrollmentRequests.size());

    try {
      List<EnrollmentResponseDTO> results =
          enrollmentService.createBulkEnrollments(
              enrollmentRequests, requestingUserId, organizationId);

      log.info(
          "evt=create_bulk_enrollments_response organizationId={} successful={} total_requested={}",
          organizationId,
          results.size(),
          enrollmentRequests.size());

      return ResponseEntity.ok(results);
    } catch (Exception e) {
      log.error(
          "evt=create_bulk_enrollments_error organizationId={} userId={}",
          organizationId,
          requestingUserId,
          e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/{batchId}/{studentId}")
  @Operation(
      summary = "Get enrollment by batch and student",
      description =
          "Retrieve enrollment information for a specific student in a batch within an"
              + " organization")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EnrollmentResponseDTO> getEnrollment(
      @PathVariable String organizationId,
      @PathVariable String batchId,
      @PathVariable String studentId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=get_enrollment_request organizationId={} batchId={} studentId={} requestingUserId={}",
        organizationId,
        batchId,
        studentId,
        userId);
    try {
      Optional<EnrollmentResponseDTO> enrollment =
          enrollmentService.getEnrollment(batchId, studentId, userId, organizationId);
      if (enrollment.isPresent()) {
        log.info(
            "evt=get_enrollment_success organizationId={} batchId={} studentId={}",
            organizationId,
            batchId,
            studentId);
        return ResponseEntity.ok(enrollment.get());
      } else {
        log.info(
            "evt=get_enrollment_not_found organizationId={} batchId={} studentId={}",
            organizationId,
            batchId,
            studentId);
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      log.error(
          "evt=get_enrollment_error organizationId={} batchId={} studentId={}",
          organizationId,
          batchId,
          studentId,
          e);
      throw e;
    }
  }

  @GetMapping
  @Operation(
      summary = "Get all enrollments",
      description = "Retrieve all enrollments with optional filtering within an organization")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<EnrollmentResponseDTO>> getAllEnrollments(
      @PathVariable String organizationId,
      @RequestParam(required = false) String batchId,
      @RequestParam(required = false) String studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info(
        "evt=get_all_enrollments_request organizationId={} batchId={} studentId={} page={} size={}"
            + " requestingUserId={}",
        organizationId,
        batchId,
        studentId,
        page,
        size,
        userId);
    try {
      PageResponseDTO<EnrollmentResponseDTO> response =
          enrollmentService.getAllEnrollments(
              Optional.ofNullable(batchId),
              Optional.ofNullable(studentId),
              page,
              size,
              userId,
              organizationId);
      log.info(
          "evt=get_all_enrollments_success organizationId={} totalElements={}",
          organizationId,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=get_all_enrollments_error organizationId={}", organizationId, e);
      throw e;
    }
  }

  @PutMapping("/{batchId}/{studentId}")
  @Operation(
      summary = "Update enrollment",
      description =
          "Update an existing enrollment within an organization. Only coaches can update"
              + " enrollments.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<EnrollmentResponseDTO> updateEnrollment(
      @PathVariable String organizationId,
      @PathVariable String batchId,
      @PathVariable String studentId,
      @RequestBody EnrollmentRequestDTO enrollmentRequest,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=update_enrollment_request organizationId={} batchId={} studentId={}"
            + " requestingUserId={}",
        organizationId,
        batchId,
        studentId,
        userId);
    try {
      Optional<EnrollmentResponseDTO> response =
          enrollmentService.updateEnrollment(
              batchId, studentId, enrollmentRequest, userId, organizationId);
      if (response.isPresent()) {
        log.info(
            "evt=update_enrollment_success organizationId={} batchId={} studentId={}",
            organizationId,
            batchId,
            studentId);
        return ResponseEntity.ok(response.get());
      } else {
        log.info(
            "evt=update_enrollment_not_found organizationId={} batchId={} studentId={}",
            organizationId,
            batchId,
            studentId);
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      log.error(
          "evt=update_enrollment_error organizationId={} batchId={} studentId={}",
          organizationId,
          batchId,
          studentId,
          e);
      throw e;
    }
  }

  @DeleteMapping("/{batchId}/{studentId}")
  @Operation(
      summary = "Delete enrollment",
      description =
          "Remove a student from a batch within an organization. Only coaches can delete"
              + " enrollments.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> deleteEnrollment(
      @PathVariable String organizationId,
      @PathVariable String batchId,
      @PathVariable String studentId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=delete_enrollment_request organizationId={} batchId={} studentId={}"
            + " requestingUserId={}",
        organizationId,
        batchId,
        studentId,
        userId);
    try {
      boolean deleted =
          enrollmentService.deleteEnrollment(batchId, studentId, userId, organizationId);
      if (deleted) {
        log.info(
            "evt=delete_enrollment_success organizationId={} batchId={} studentId={}",
            organizationId,
            batchId,
            studentId);
        return ResponseEntity.noContent().build();
      } else {
        log.info(
            "evt=delete_enrollment_not_found organizationId={} batchId={} studentId={}",
            organizationId,
            batchId,
            studentId);
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      log.error(
          "evt=delete_enrollment_error organizationId={} batchId={} studentId={}",
          organizationId,
          batchId,
          studentId,
          e);
      throw e;
    }
  }

  @GetMapping("/batch/{batchId}")
  @Operation(
      summary = "Get enrollments by batch",
      description = "Retrieve all enrollments for a specific batch within an organization")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<EnrollmentResponseDTO>> getEnrollmentsByBatch(
      @PathVariable String organizationId,
      @PathVariable String batchId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=get_enrollments_by_batch_request organizationId={} batchId={} page={} size={}"
            + " requestingUserId={}",
        organizationId,
        batchId,
        page,
        size,
        userId);
    try {
      PageResponseDTO<EnrollmentResponseDTO> response =
          enrollmentService.getEnrollmentsByBatch(batchId, page, size, userId, organizationId);
      log.info(
          "evt=get_enrollments_by_batch_success organizationId={} batchId={} totalElements={}",
          organizationId,
          batchId,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error(
          "evt=get_enrollments_by_batch_error organizationId={} batchId={}",
          organizationId,
          batchId,
          e);
      throw e;
    }
  }

  @GetMapping("/student/{studentId}")
  @Operation(
      summary = "Get enrollments by student",
      description = "Retrieve all enrollments for a specific student within an organization")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<EnrollmentResponseDTO>> getEnrollmentsByStudent(
      @PathVariable String organizationId,
      @PathVariable String studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=get_enrollments_by_student_request organizationId={} studentId={} page={} size={}"
            + " requestingUserId={}",
        organizationId,
        studentId,
        page,
        size,
        userId);
    try {
      PageResponseDTO<EnrollmentResponseDTO> response =
          enrollmentService.getEnrollmentsByStudent(studentId, page, size, userId, organizationId);
      log.info(
          "evt=get_enrollments_by_student_success organizationId={} studentId={} totalElements={}",
          organizationId,
          studentId,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error(
          "evt=get_enrollments_by_student_error organizationId={} studentId={}",
          organizationId,
          studentId,
          e);
      throw e;
    }
  }
}
