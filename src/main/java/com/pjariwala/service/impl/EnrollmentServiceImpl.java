package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.EnrollmentRequestDTO;
import com.pjariwala.dto.EnrollmentResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Batch;
import com.pjariwala.model.Enrollment;
import com.pjariwala.model.User;
import com.pjariwala.service.EnrollmentService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

  @Autowired private DynamoDBMapper dynamoDBMapper;

  @Override
  public EnrollmentResponseDTO createEnrollment(EnrollmentRequestDTO enrollmentRequest) {
    log.info(
        "evt=create_enrollment_start batchId={} studentId={}",
        enrollmentRequest.getBatchId(),
        enrollmentRequest.getStudentId());

    validateEnrollmentRequest(enrollmentRequest);

    // Check if enrollment already exists
    if (isStudentEnrolled(enrollmentRequest.getBatchId(), enrollmentRequest.getStudentId())) {
      log.error(
          "evt=create_enrollment_already_exists batchId={} studentId={}",
          enrollmentRequest.getBatchId(),
          enrollmentRequest.getStudentId());
      throw UserException.userExists("Student is already enrolled in this batch");
    }

    // Verify batch and student exist
    validateBatchAndStudent(enrollmentRequest.getBatchId(), enrollmentRequest.getStudentId());

    Enrollment enrollment =
        Enrollment.builder()
            .batchId(enrollmentRequest.getBatchId())
            .studentId(enrollmentRequest.getStudentId())
            .enrollmentDate(
                enrollmentRequest.getEnrollmentDate() != null
                    ? enrollmentRequest.getEnrollmentDate()
                    : java.time.LocalDate.now())
            .enrollmentStatus(
                enrollmentRequest.getEnrollmentStatus() != null
                    ? enrollmentRequest.getEnrollmentStatus()
                    : Enrollment.EnrollmentStatus.ENROLLED)
            .enrollmentPaymentStatus(
                enrollmentRequest.getEnrollmentPaymentStatus() != null
                    ? enrollmentRequest.getEnrollmentPaymentStatus()
                    : Enrollment.PaymentStatus.PENDING)
            .currentPaymentAmount(
                enrollmentRequest.getCurrentPaymentAmount() != null
                    ? enrollmentRequest.getCurrentPaymentAmount()
                    : 0.0)
            .notes(enrollmentRequest.getNotes())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    try {
      dynamoDBMapper.save(enrollment);
      log.info(
          "evt=create_enrollment_success batchId={} studentId={}",
          enrollment.getBatchId(),
          enrollment.getStudentId());
      return convertToResponseDTO(enrollment);
    } catch (Exception e) {
      log.error(
          "evt=create_enrollment_error batchId={} studentId={}",
          enrollmentRequest.getBatchId(),
          enrollmentRequest.getStudentId(),
          e);
      throw UserException.databaseError("Failed to create enrollment", e);
    }
  }

  @Override
  public Optional<EnrollmentResponseDTO> getEnrollment(String batchId, String studentId) {
    log.info("evt=get_enrollment_start batchId={} studentId={}", batchId, studentId);
    try {
      Enrollment enrollment = dynamoDBMapper.load(Enrollment.class, batchId, studentId);
      if (enrollment != null) {
        log.info("evt=get_enrollment_success batchId={} studentId={}", batchId, studentId);
        return Optional.of(convertToResponseDTO(enrollment));
      } else {
        log.info("evt=get_enrollment_not_found batchId={} studentId={}", batchId, studentId);
        return Optional.empty();
      }
    } catch (Exception e) {
      log.error("evt=get_enrollment_error batchId={} studentId={}", batchId, studentId, e);
      return Optional.empty();
    }
  }

  @Override
  public PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByBatch(
      String batchId, int page, int size) {
    log.info("evt=get_enrollments_by_batch_start batchId={} page={} size={}", batchId, page, size);
    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":batchId", new AttributeValue().withS(batchId));

      DynamoDBQueryExpression<Enrollment> queryExpression =
          new DynamoDBQueryExpression<Enrollment>()
              .withKeyConditionExpression("batchId = :batchId")
              .withExpressionAttributeValues(eav);

      PaginatedQueryList<Enrollment> queryResult =
          dynamoDBMapper.query(Enrollment.class, queryExpression);
      List<Enrollment> enrollments = new ArrayList<>(queryResult);

      return paginateResults(enrollments, page, size);
    } catch (Exception e) {
      log.error("evt=get_enrollments_by_batch_error batchId={}", batchId, e);
      throw UserException.databaseError("Failed to retrieve enrollments for batch", e);
    }
  }

  @Override
  public PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByStudent(
      String studentId, int page, int size) {
    log.info(
        "evt=get_enrollments_by_student_start studentId={} page={} size={}", studentId, page, size);
    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":studentId", new AttributeValue().withS(studentId));

      DynamoDBQueryExpression<Enrollment> queryExpression =
          new DynamoDBQueryExpression<Enrollment>()
              .withIndexName("studentId-batchId-index")
              .withConsistentRead(false)
              .withKeyConditionExpression("studentId = :studentId")
              .withExpressionAttributeValues(eav);

      PaginatedQueryList<Enrollment> queryResult =
          dynamoDBMapper.query(Enrollment.class, queryExpression);
      List<Enrollment> enrollments = new ArrayList<>(queryResult);

      return paginateResults(enrollments, page, size);
    } catch (Exception e) {
      log.error("evt=get_enrollments_by_student_error studentId={}", studentId, e);
      throw UserException.databaseError("Failed to retrieve enrollments for student", e);
    }
  }

  @Override
  public EnrollmentResponseDTO updateEnrollment(
      String batchId, String studentId, EnrollmentRequestDTO enrollmentRequest) {
    log.info("evt=update_enrollment_start batchId={} studentId={}", batchId, studentId);

    try {
      Enrollment existingEnrollment = dynamoDBMapper.load(Enrollment.class, batchId, studentId);
      if (existingEnrollment == null) {
        log.error("evt=update_enrollment_not_found batchId={} studentId={}", batchId, studentId);
        throw UserException.userNotFound("Enrollment not found");
      }

      // Update fields
      if (enrollmentRequest.getEnrollmentDate() != null) {
        existingEnrollment.setEnrollmentDate(enrollmentRequest.getEnrollmentDate());
      }
      if (enrollmentRequest.getEnrollmentStatus() != null) {
        existingEnrollment.setEnrollmentStatus(enrollmentRequest.getEnrollmentStatus());
      }
      if (enrollmentRequest.getEnrollmentPaymentStatus() != null) {
        existingEnrollment.setEnrollmentPaymentStatus(
            enrollmentRequest.getEnrollmentPaymentStatus());
      }
      if (enrollmentRequest.getCurrentPaymentAmount() != null) {
        existingEnrollment.setCurrentPaymentAmount(enrollmentRequest.getCurrentPaymentAmount());
      }
      if (enrollmentRequest.getNotes() != null) {
        existingEnrollment.setNotes(enrollmentRequest.getNotes());
      }
      existingEnrollment.setUpdatedAt(LocalDateTime.now());

      dynamoDBMapper.save(existingEnrollment);
      log.info("evt=update_enrollment_success batchId={} studentId={}", batchId, studentId);
      return convertToResponseDTO(existingEnrollment);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error("evt=update_enrollment_error batchId={} studentId={}", batchId, studentId, e);
      throw UserException.databaseError("Failed to update enrollment", e);
    }
  }

  @Override
  public void deleteEnrollment(String batchId, String studentId) {
    log.info("evt=delete_enrollment_start batchId={} studentId={}", batchId, studentId);
    try {
      Enrollment existingEnrollment = dynamoDBMapper.load(Enrollment.class, batchId, studentId);
      if (existingEnrollment == null) {
        log.error("evt=delete_enrollment_not_found batchId={} studentId={}", batchId, studentId);
        throw UserException.userNotFound("Enrollment not found");
      }

      dynamoDBMapper.delete(existingEnrollment);
      log.info("evt=delete_enrollment_success batchId={} studentId={}", batchId, studentId);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error("evt=delete_enrollment_error batchId={} studentId={}", batchId, studentId, e);
      throw UserException.databaseError("Failed to delete enrollment", e);
    }
  }

  @Override
  public boolean isStudentEnrolled(String batchId, String studentId) {
    try {
      Enrollment enrollment = dynamoDBMapper.load(Enrollment.class, batchId, studentId);
      return enrollment != null
          && enrollment.getEnrollmentStatus() == Enrollment.EnrollmentStatus.ENROLLED;
    } catch (Exception e) {
      log.error("evt=is_student_enrolled_error batchId={} studentId={}", batchId, studentId, e);
      return false;
    }
  }

  @Override
  public PageResponseDTO<EnrollmentResponseDTO> getAllEnrollments(
      Optional<Enrollment.EnrollmentStatus> status,
      Optional<Enrollment.PaymentStatus> paymentStatus,
      int page,
      int size) {
    log.info(
        "evt=get_all_enrollments_start page={} size={} status={} paymentStatus={}",
        page,
        size,
        status.orElse(null),
        paymentStatus.orElse(null));

    try {
      DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();

      // Build filter expression
      List<String> filterConditions = new ArrayList<>();
      Map<String, AttributeValue> eav = new HashMap<>();

      if (status.isPresent()) {
        filterConditions.add("enrollmentStatus = :status");
        eav.put(":status", new AttributeValue().withS(status.get().toString()));
      }

      if (paymentStatus.isPresent()) {
        filterConditions.add("paymentStatus = :paymentStatus");
        eav.put(":paymentStatus", new AttributeValue().withS(paymentStatus.get().toString()));
      }

      if (!filterConditions.isEmpty()) {
        scanExpression.withFilterExpression(String.join(" AND ", filterConditions));
        scanExpression.withExpressionAttributeValues(eav);
      }

      PaginatedScanList<Enrollment> scanResult =
          dynamoDBMapper.scan(Enrollment.class, scanExpression);
      List<Enrollment> enrollments = new ArrayList<>(scanResult);

      return paginateResults(enrollments, page, size);
    } catch (Exception e) {
      log.error("evt=get_all_enrollments_error", e);
      throw UserException.databaseError("Failed to retrieve enrollments", e);
    }
  }

  @Override
  public EnrollmentResponseDTO updatePaymentStatus(
      String batchId,
      String studentId,
      Enrollment.PaymentStatus paymentStatus,
      Double paymentAmount) {
    log.info(
        "evt=update_payment_status_start batchId={} studentId={} paymentStatus={} amount={}",
        batchId,
        studentId,
        paymentStatus,
        paymentAmount);

    try {
      Enrollment existingEnrollment = dynamoDBMapper.load(Enrollment.class, batchId, studentId);
      if (existingEnrollment == null) {
        log.error(
            "evt=update_payment_status_not_found batchId={} studentId={}", batchId, studentId);
        throw UserException.userNotFound("Enrollment not found");
      }

      existingEnrollment.setEnrollmentPaymentStatus(paymentStatus);
      if (paymentAmount != null) {
        existingEnrollment.setCurrentPaymentAmount(paymentAmount);
      }
      existingEnrollment.setUpdatedAt(LocalDateTime.now());

      dynamoDBMapper.save(existingEnrollment);
      log.info("evt=update_payment_status_success batchId={} studentId={}", batchId, studentId);
      return convertToResponseDTO(existingEnrollment);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error("evt=update_payment_status_error batchId={} studentId={}", batchId, studentId, e);
      throw UserException.databaseError("Failed to update payment status", e);
    }
  }

  @Override
  public PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByPaymentStatus(
      Enrollment.PaymentStatus paymentStatus, int page, int size) {
    log.info(
        "evt=get_enrollments_by_payment_status_start paymentStatus={} page={} size={}",
        paymentStatus,
        page,
        size);

    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":paymentStatus", new AttributeValue().withS(paymentStatus.toString()));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression("paymentStatus = :paymentStatus")
              .withExpressionAttributeValues(eav);

      PaginatedScanList<Enrollment> scanResult =
          dynamoDBMapper.scan(Enrollment.class, scanExpression);
      List<Enrollment> enrollments = new ArrayList<>(scanResult);

      return paginateResults(enrollments, page, size);
    } catch (Exception e) {
      log.error("evt=get_enrollments_by_payment_status_error paymentStatus={}", paymentStatus, e);
      throw UserException.databaseError("Failed to retrieve enrollments by payment status", e);
    }
  }

  // Helper methods

  private void validateEnrollmentRequest(EnrollmentRequestDTO request) {
    log.debug("evt=validate_enrollment_request_start");

    if (request.getBatchId() == null || request.getBatchId().trim().isEmpty()) {
      throw UserException.validationError("Batch ID is required");
    }
    if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
      throw UserException.validationError("Student ID is required");
    }

    log.debug("evt=validate_enrollment_request_success");
  }

  private void validateBatchAndStudent(String batchId, String studentId) {
    // Check if batch exists
    Batch batch = dynamoDBMapper.load(Batch.class, batchId);
    if (batch == null || batch.getBatchStatus() == Batch.BatchStatus.CANCELLED) {
      throw UserException.userNotFound("Batch not found or cancelled: " + batchId);
    }

    // Check if student exists
    User student = dynamoDBMapper.load(User.class, studentId, "STUDENT");
    if (student == null || !student.getIsActive()) {
      throw UserException.userNotFound("Student not found or inactive: " + studentId);
    }

    // Check if batch is full
    if (batch.getCurrentStudents() != null
        && batch.getBatchSize() != null
        && batch.getCurrentStudents() >= batch.getBatchSize()) {
      throw UserException.validationError("Batch is full");
    }
  }

  private EnrollmentResponseDTO convertToResponseDTO(Enrollment enrollment) {
    EnrollmentResponseDTO dto = new EnrollmentResponseDTO();
    dto.setBatchId(enrollment.getBatchId());
    dto.setStudentId(enrollment.getStudentId());
    dto.setEnrollmentDate(enrollment.getEnrollmentDate());
    dto.setEnrollmentStatus(enrollment.getEnrollmentStatus());
    dto.setEnrollmentPaymentStatus(enrollment.getEnrollmentPaymentStatus());
    dto.setCurrentPaymentAmount(enrollment.getCurrentPaymentAmount());
    dto.setNotes(enrollment.getNotes());
    dto.setCreatedAt(enrollment.getCreatedAt());
    dto.setUpdatedAt(enrollment.getUpdatedAt());

    // Enrich with batch and student names (optional - could be optimized)
    try {
      Batch batch = dynamoDBMapper.load(Batch.class, enrollment.getBatchId());
      if (batch != null) {
        dto.setBatchName(batch.getBatchName());
      }

      User student = dynamoDBMapper.load(User.class, enrollment.getStudentId(), "STUDENT");
      if (student != null) {
        dto.setStudentName(student.getName());
      }
    } catch (Exception e) {
      log.warn("evt=enrich_enrollment_response_warning", e);
      // Continue without enrichment
    }

    return dto;
  }

  private PageResponseDTO<EnrollmentResponseDTO> paginateResults(
      List<Enrollment> enrollments, int page, int size) {
    int totalElements = enrollments.size();
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, totalElements);

    List<Enrollment> paginatedEnrollments =
        startIndex < totalElements ? enrollments.subList(startIndex, endIndex) : new ArrayList<>();

    List<EnrollmentResponseDTO> enrollmentDTOs =
        paginatedEnrollments.stream().map(this::convertToResponseDTO).collect(Collectors.toList());

    PageResponseDTO.PageInfoDTO pageInfo =
        new PageResponseDTO.PageInfoDTO(
            page, size, (totalElements + size - 1) / size, totalElements);

    log.info("evt=paginate_enrollment_results_success totalElements={}", totalElements);
    return new PageResponseDTO<>(enrollmentDTOs, pageInfo);
  }
}
