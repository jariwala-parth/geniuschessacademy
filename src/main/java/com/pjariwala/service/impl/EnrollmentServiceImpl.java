package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.EnrollmentRequestDTO;
import com.pjariwala.dto.EnrollmentResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.BatchStatus;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.AuthException;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Batch;
import com.pjariwala.model.Enrollment;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.EnrollmentService;
import com.pjariwala.service.UserService;
import com.pjariwala.util.ValidationUtil;
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
  @Autowired private UserService userService;
  @Autowired private ActivityLogService activityLogService;
  @Autowired private ValidationUtil validationUtil;

  @Override
  public EnrollmentResponseDTO createEnrollment(
      EnrollmentRequestDTO enrollmentRequest, String requestingUserId, String organizationId) {
    log.info(
        "evt=create_enrollment_start batchId={} studentId={} requestingUserId={} organizationId={}",
        enrollmentRequest.getBatchId(),
        enrollmentRequest.getStudentId(),
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Authorization: Only coaches can enroll students
    validationUtil.requireCoachPermission(requestingUserId, organizationId);

    validateEnrollmentRequest(enrollmentRequest);

    // Check if enrollment already exists
    if (isStudentEnrolled(
        enrollmentRequest.getBatchId(), enrollmentRequest.getStudentId(), organizationId)) {
      log.error(
          "evt=create_enrollment_already_exists batchId={} studentId={} organizationId={}",
          enrollmentRequest.getBatchId(),
          enrollmentRequest.getStudentId(),
          organizationId);
      throw UserException.userExists("Student is already enrolled in this batch");
    }

    // Verify batch and student exist
    validateBatchAndStudent(
        enrollmentRequest.getBatchId(), enrollmentRequest.getStudentId(), organizationId);

    Enrollment enrollment =
        Enrollment.builder()
            .organizationId(organizationId)
            .enrollmentId(
                enrollmentRequest.getBatchId()
                    + ":"
                    + enrollmentRequest.getStudentId()) // Composite enrollmentId
            .batchId(enrollmentRequest.getBatchId())
            .studentId(enrollmentRequest.getStudentId())
            .enrollmentDate(enrollmentRequest.getEnrollmentDate())
            .enrollmentStatus(enrollmentRequest.getEnrollmentStatus())
            .enrollmentPaymentStatus(enrollmentRequest.getEnrollmentPaymentStatus())
            .currentPaymentAmount(enrollmentRequest.getCurrentPaymentAmount())
            .notes(enrollmentRequest.getNotes())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    try {
      dynamoDBMapper.save(enrollment);
      log.info(
          "evt=create_enrollment_success batchId={} studentId={} organizationId={}",
          enrollment.getBatchId(),
          enrollment.getStudentId(),
          organizationId);

      // Log enrollment activity
      try {
        User coach = getUser(requestingUserId, organizationId);
        User student = getUser(enrollmentRequest.getStudentId(), organizationId);
        Batch batch = getBatch(enrollmentRequest.getBatchId(), organizationId);
        activityLogService.logEnrollment(
            coach.getUserId(),
            coach.getName(),
            student.getUserId(),
            student.getName(),
            batch.getBatchId(),
            batch.getBatchName(),
            organizationId);
      } catch (Exception e) {
        log.warn(
            "Failed to log enrollment activity for batch: {} student: {} organizationId={}",
            enrollmentRequest.getBatchId(),
            enrollmentRequest.getStudentId(),
            organizationId,
            e);
      }

      return convertToResponseDTO(enrollment);
    } catch (Exception e) {
      log.error(
          "evt=create_enrollment_error batchId={} studentId={} organizationId={} error={}",
          enrollmentRequest.getBatchId(),
          enrollmentRequest.getStudentId(),
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to create enrollment", e);
    }
  }

  @Override
  public Optional<EnrollmentResponseDTO> getEnrollment(
      String batchId, String studentId, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_enrollment_start batchId={} studentId={} requestingUserId={} organizationId={}",
        batchId,
        studentId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Validate user access - students can only view their own enrollments, coaches can view all
    validationUtil.validateUserAccess(requestingUserId, studentId, organizationId);

    try {
      String enrollmentId = batchId + ":" + studentId;
      Enrollment enrollment = dynamoDBMapper.load(Enrollment.class, organizationId, enrollmentId);
      if (enrollment != null && organizationId.equals(enrollment.getOrganizationId())) {
        log.info(
            "evt=get_enrollment_success batchId={} studentId={} organizationId={}",
            batchId,
            studentId,
            organizationId);
        return Optional.of(convertToResponseDTO(enrollment));
      } else {
        log.info(
            "evt=get_enrollment_not_found batchId={} studentId={} organizationId={}",
            batchId,
            studentId,
            organizationId);
        return Optional.empty();
      }
    } catch (Exception e) {
      log.error(
          "evt=get_enrollment_error batchId={} studentId={} organizationId={} error={}",
          batchId,
          studentId,
          organizationId,
          e.getMessage(),
          e);
      return Optional.empty();
    }
  }

  @Override
  public PageResponseDTO<EnrollmentResponseDTO> getAllEnrollments(
      Optional<String> batchId,
      Optional<String> studentId,
      int page,
      int size,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=get_all_enrollments_start batchId={} studentId={} page={} size={} requestingUserId={}"
            + " organizationId={}",
        batchId.orElse(null),
        studentId.orElse(null),
        page,
        size,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Authorization: Students can only see their own enrollments
    User requestingUser = getUser(requestingUserId, organizationId);
    if (!UserType.COACH.name().equals(requestingUser.getUserType())) {
      // Force studentId to be the requesting user for students
      if (studentId.isEmpty() || !studentId.get().equals(requestingUserId)) {
        log.warn(
            "evt=get_all_enrollments_access_denied userId={} requestedStudentId={}"
                + " organizationId={}",
            requestingUserId,
            studentId.orElse("none"),
            organizationId);
        throw new AuthException(
            "ACCESS_DENIED", "Students can only access their own enrollments", 403);
      }
    }

    try {
      List<Enrollment> allEnrollments = new ArrayList<>();

      if (batchId.isPresent() && studentId.isPresent()) {
        // Get specific enrollment
        Enrollment enrollment =
            dynamoDBMapper.load(Enrollment.class, batchId.get(), studentId.get());
        if (enrollment != null && organizationId.equals(enrollment.getOrganizationId())) {
          allEnrollments.add(enrollment);
        }
      } else if (batchId.isPresent()) {
        // Get enrollments by batch
        allEnrollments = getEnrollmentsByBatchFromDb(batchId.get(), organizationId);
      } else if (studentId.isPresent()) {
        // Get enrollments by student
        allEnrollments = getEnrollmentsByStudentFromDb(studentId.get(), organizationId);
      } else {
        // Get all enrollments (only for coaches)
        if (UserType.COACH.name().equals(requestingUser.getUserType())) {
          Map<String, AttributeValue> eav = new HashMap<>();
          eav.put(":organizationId", new AttributeValue().withS(organizationId));

          DynamoDBScanExpression scanExpression =
              new DynamoDBScanExpression()
                  .withFilterExpression("organizationId = :organizationId")
                  .withExpressionAttributeValues(eav);
          PaginatedScanList<Enrollment> scanResult =
              dynamoDBMapper.scan(Enrollment.class, scanExpression);
          allEnrollments = new ArrayList<>(scanResult);
        }
      }

      // Apply pagination
      int totalElements = allEnrollments.size();
      int startIndex = page * size;
      int endIndex = Math.min(startIndex + size, totalElements);

      List<Enrollment> paginatedEnrollments =
          startIndex < totalElements
              ? allEnrollments.subList(startIndex, endIndex)
              : new ArrayList<>();

      List<EnrollmentResponseDTO> enrollmentDTOs =
          paginatedEnrollments.stream()
              .map(this::convertToResponseDTO)
              .collect(Collectors.toList());

      PageResponseDTO.PageInfoDTO pageInfo =
          new PageResponseDTO.PageInfoDTO(
              page, size, (totalElements + size - 1) / size, totalElements);

      log.info(
          "evt=get_all_enrollments_success totalElements={} organizationId={}",
          totalElements,
          organizationId);
      return new PageResponseDTO<>(enrollmentDTOs, pageInfo);
    } catch (Exception e) {
      log.error(
          "evt=get_all_enrollments_error organizationId={} error={}",
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve enrollments", e);
    }
  }

  @Override
  public Optional<EnrollmentResponseDTO> updateEnrollment(
      String batchId,
      String studentId,
      EnrollmentRequestDTO enrollmentRequest,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=update_enrollment_start batchId={} studentId={} requestingUserId={} organizationId={}",
        batchId,
        studentId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Authorization: Only coaches can update enrollments
    validationUtil.requireCoachPermission(requestingUserId, organizationId);

    try {
      String enrollmentId = batchId + ":" + studentId;
      Enrollment existingEnrollment =
          dynamoDBMapper.load(Enrollment.class, organizationId, enrollmentId);
      if (existingEnrollment == null
          || !organizationId.equals(existingEnrollment.getOrganizationId())) {
        log.info(
            "evt=update_enrollment_not_found batchId={} studentId={} organizationId={}",
            batchId,
            studentId,
            organizationId);
        return Optional.empty();
      }

      validateEnrollmentRequest(enrollmentRequest);

      // Update enrollment fields
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
      log.info(
          "evt=update_enrollment_success batchId={} studentId={} organizationId={}",
          batchId,
          studentId,
          organizationId);
      return Optional.of(convertToResponseDTO(existingEnrollment));
    } catch (Exception e) {
      log.error(
          "evt=update_enrollment_error batchId={} studentId={} organizationId={} error={}",
          batchId,
          studentId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to update enrollment", e);
    }
  }

  @Override
  public boolean deleteEnrollment(
      String batchId, String studentId, String requestingUserId, String organizationId) {
    log.info(
        "evt=delete_enrollment_start batchId={} studentId={} requestingUserId={} organizationId={}",
        batchId,
        studentId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Authorization: Only coaches can delete enrollments
    validationUtil.requireCoachPermission(requestingUserId, organizationId);

    try {
      String enrollmentId = batchId + ":" + studentId;
      Enrollment existingEnrollment =
          dynamoDBMapper.load(Enrollment.class, organizationId, enrollmentId);
      if (existingEnrollment == null
          || !organizationId.equals(existingEnrollment.getOrganizationId())) {
        log.info(
            "evt=delete_enrollment_not_found batchId={} studentId={} organizationId={}",
            batchId,
            studentId,
            organizationId);
        return false;
      }

      dynamoDBMapper.delete(existingEnrollment);
      log.info(
          "evt=delete_enrollment_success batchId={} studentId={} organizationId={}",
          batchId,
          studentId,
          organizationId);

      // Log enrollment removal activity
      try {
        User coach = getUser(requestingUserId, organizationId);
        User student = getUser(studentId, organizationId);
        activityLogService.logAction(
            ActionType.REMOVE_ENROLLMENT,
            coach.getUserId(),
            coach.getName(),
            UserType.COACH,
            String.format("Removed %s from batch", student.getName()),
            EntityType.ENROLLMENT,
            batchId + ":" + studentId,
            String.format("%s -> Batch", student.getName()),
            organizationId);
      } catch (Exception e) {
        log.warn(
            "Failed to log enrollment removal activity for batch: {} student: {} organizationId={}",
            batchId,
            studentId,
            organizationId,
            e);
      }

      return true;
    } catch (Exception e) {
      log.error(
          "evt=delete_enrollment_error batchId={} studentId={} organizationId={} error={}",
          batchId,
          studentId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to delete enrollment", e);
    }
  }

  @Override
  public PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByBatch(
      String batchId, int page, int size, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_enrollments_by_batch_start batchId={} page={} size={} requestingUserId={}"
            + " organizationId={}",
        batchId,
        page,
        size,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Only coaches can view batch enrollments
    validationUtil.requireCoachPermission(requestingUserId, organizationId);

    try {
      List<Enrollment> enrollments = getEnrollmentsByBatchFromDb(batchId, organizationId);
      return paginateResults(enrollments, page, size);
    } catch (Exception e) {
      log.error(
          "evt=get_enrollments_by_batch_error batchId={} organizationId={} error={}",
          batchId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve enrollments by batch", e);
    }
  }

  @Override
  public PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByStudent(
      String studentId, int page, int size, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_enrollments_by_student_start studentId={} page={} size={} requestingUserId={}"
            + " organizationId={}",
        studentId,
        page,
        size,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Validate user access - students can only view their own enrollments, coaches can view any
    validationUtil.validateUserAccess(requestingUserId, studentId, organizationId);

    try {
      List<Enrollment> enrollments = getEnrollmentsByStudentFromDb(studentId, organizationId);
      return paginateResults(enrollments, page, size);
    } catch (Exception e) {
      log.error(
          "evt=get_enrollments_by_student_error studentId={} organizationId={} error={}",
          studentId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve enrollments by student", e);
    }
  }

  @Override
  public List<EnrollmentResponseDTO> createBulkEnrollments(
      List<EnrollmentRequestDTO> enrollmentRequests,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=create_bulk_enrollments_start count={} requestingUserId={} organizationId={}",
        enrollmentRequests.size(),
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Authorization: Only coaches can enroll students
    validationUtil.requireCoachPermission(requestingUserId, organizationId);

    List<EnrollmentResponseDTO> results = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    for (EnrollmentRequestDTO request : enrollmentRequests) {
      try {
        // Validate each request
        validateEnrollmentRequest(request);

        // Check if enrollment already exists
        if (isStudentEnrolled(request.getBatchId(), request.getStudentId(), organizationId)) {
          errors.add(
              String.format(
                  "Student %s already enrolled in batch %s",
                  request.getStudentId(), request.getBatchId()));
          continue;
        }

        // Verify batch and student exist
        validateBatchAndStudent(request.getBatchId(), request.getStudentId(), organizationId);

        Enrollment enrollment =
            Enrollment.builder()
                .organizationId(organizationId)
                .enrollmentId(
                    request.getBatchId() + ":" + request.getStudentId()) // Composite enrollmentId
                .batchId(request.getBatchId())
                .studentId(request.getStudentId())
                .enrollmentDate(request.getEnrollmentDate())
                .enrollmentStatus(request.getEnrollmentStatus())
                .enrollmentPaymentStatus(request.getEnrollmentPaymentStatus())
                .currentPaymentAmount(request.getCurrentPaymentAmount())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        dynamoDBMapper.save(enrollment);
        results.add(convertToResponseDTO(enrollment));

        // Log enrollment activity
        try {
          User coach = getUser(requestingUserId, organizationId);
          User student = getUser(request.getStudentId(), organizationId);
          Batch batch = getBatch(request.getBatchId(), organizationId);
          activityLogService.logEnrollment(
              coach.getUserId(),
              coach.getName(),
              student.getUserId(),
              student.getName(),
              batch.getBatchId(),
              batch.getBatchName(),
              organizationId);
        } catch (Exception e) {
          log.warn(
              "Failed to log bulk enrollment activity for batch: {} student: {} organizationId={}",
              request.getBatchId(),
              request.getStudentId(),
              organizationId,
              e);
        }

      } catch (Exception e) {
        errors.add(
            String.format(
                "Failed to enroll student %s in batch %s: %s",
                request.getStudentId(), request.getBatchId(), e.getMessage()));
      }
    }

    if (!errors.isEmpty()) {
      log.warn(
          "evt=create_bulk_enrollments_errors organizationId={} errors={}", organizationId, errors);
    }

    log.info(
        "evt=create_bulk_enrollments_success organizationId={} successful={} errors={}",
        organizationId,
        results.size(),
        errors.size());
    return results;
  }

  @Override
  public int countActiveEnrollmentsByBatch(String batchId, String organizationId) {
    try {
      List<Enrollment> enrollments = getEnrollmentsByBatchFromDb(batchId, organizationId);
      return (int)
          enrollments.stream().filter(e -> "ENROLLED".equals(e.getEnrollmentStatus())).count();
    } catch (Exception e) {
      log.error(
          "evt=count_active_enrollments_error batchId={} organizationId={} error={}",
          batchId,
          organizationId,
          e.getMessage(),
          e);
      return 0;
    }
  }

  @Override
  public List<String> getEnrolledBatchIdsForStudent(String studentId, String organizationId) {
    try {
      List<Enrollment> enrollments = getEnrollmentsByStudentFromDb(studentId, organizationId);
      return enrollments.stream()
          .filter(e -> "ENROLLED".equals(e.getEnrollmentStatus()))
          .map(Enrollment::getBatchId)
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error(
          "evt=get_enrolled_batch_ids_error studentId={} organizationId={} error={}",
          studentId,
          organizationId,
          e.getMessage(),
          e);
      return new ArrayList<>();
    }
  }

  @Override
  public boolean isStudentEnrolled(String batchId, String studentId, String organizationId) {
    try {
      // Use scan to find enrollment by batchId, studentId, and organizationId
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":batchId", new AttributeValue().withS(batchId));
      eav.put(":studentId", new AttributeValue().withS(studentId));
      eav.put(":organizationId", new AttributeValue().withS(organizationId));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression(
                  "batchId = :batchId AND studentId = :studentId AND organizationId ="
                      + " :organizationId")
              .withExpressionAttributeValues(eav);

      PaginatedScanList<Enrollment> results = dynamoDBMapper.scan(Enrollment.class, scanExpression);
      return !results.isEmpty() && "ENROLLED".equals(results.get(0).getEnrollmentStatus().name());
    } catch (Exception e) {
      log.error(
          "evt=is_student_enrolled_error batchId={} studentId={} organizationId={} error={}",
          batchId,
          studentId,
          organizationId,
          e.getMessage(),
          e);
      return false;
    }
  }

  // Helper methods

  private List<Enrollment> getEnrollmentsByBatchFromDb(String batchId, String organizationId) {
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":batchId", new AttributeValue().withS(batchId));
    eav.put(":organizationId", new AttributeValue().withS(organizationId));

    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("batchId = :batchId AND organizationId = :organizationId")
            .withExpressionAttributeValues(eav);

    PaginatedScanList<Enrollment> results = dynamoDBMapper.scan(Enrollment.class, scanExpression);
    return new ArrayList<>(results);
  }

  private List<Enrollment> getEnrollmentsByStudentFromDb(String studentId, String organizationId) {
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":studentId", new AttributeValue().withS(studentId));
    eav.put(":organizationId", new AttributeValue().withS(organizationId));

    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("studentId = :studentId AND organizationId = :organizationId")
            .withExpressionAttributeValues(eav);

    PaginatedScanList<Enrollment> results = dynamoDBMapper.scan(Enrollment.class, scanExpression);
    return new ArrayList<>(results);
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

    return new PageResponseDTO<>(enrollmentDTOs, pageInfo);
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

    // Note: organizationId is not included in the DTO as it's internal
    return dto;
  }

  private void validateEnrollmentRequest(EnrollmentRequestDTO request) {
    log.debug("evt=validate_enrollment_request_start");

    if (request.getBatchId() == null || request.getBatchId().trim().isEmpty()) {
      throw UserException.validationError("Batch ID is required");
    }

    if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
      throw UserException.validationError("Student ID is required");
    }

    if (request.getEnrollmentDate() == null) {
      throw UserException.validationError("Enrollment date is required");
    }

    if (request.getEnrollmentStatus() == null) {
      throw UserException.validationError("Enrollment status is required");
    }

    if (request.getEnrollmentPaymentStatus() == null) {
      throw UserException.validationError("Enrollment payment status is required");
    }

    if (request.getCurrentPaymentAmount() == null || request.getCurrentPaymentAmount() < 0) {
      throw UserException.validationError("Current payment amount must be non-negative");
    }

    log.debug("evt=validate_enrollment_request_success");
  }

  private void validateBatchAndStudent(String batchId, String studentId, String organizationId) {
    log.debug(
        "evt=validate_batch_and_student_start batchId={} studentId={} organizationId={}",
        batchId,
        studentId,
        organizationId);

    try {
      // Check if batch exists
      Batch batch = dynamoDBMapper.load(Batch.class, organizationId, batchId);
      if (batch == null) {
        log.error(
            "evt=validate_batch_not_found batchId={} organizationId={}", batchId, organizationId);
        throw UserException.userNotFound("Batch not found: " + batchId);
      }

      if (batch.getBatchStatus() == BatchStatus.CANCELLED) {
        log.error(
            "evt=validate_batch_cancelled batchId={} organizationId={}", batchId, organizationId);
        throw UserException.validationError("Cannot enroll in cancelled batch: " + batchId);
      }

      // Check if student exists
      User student = getUser(studentId, organizationId);
      if (student == null) {
        log.error(
            "evt=validate_student_not_found studentId={} organizationId={}",
            studentId,
            organizationId);
        throw UserException.userNotFound("Student not found: " + studentId);
      }

      if (student.getIsActive() != null && !student.getIsActive()) {
        log.error(
            "evt=validate_student_inactive studentId={} organizationId={}",
            studentId,
            organizationId);
        throw UserException.validationError("Cannot enroll inactive student: " + studentId);
      }

      // Check if batch is full using dynamic count
      int currentStudents = countActiveEnrollmentsByBatch(batchId, organizationId);
      if (batch.getBatchSize() != null && currentStudents >= batch.getBatchSize()) {
        log.error(
            "evt=validate_batch_full batchId={} current={} max={} organizationId={}",
            batchId,
            currentStudents,
            batch.getBatchSize(),
            organizationId);
        throw UserException.validationError(
            "Batch is full (capacity: " + batch.getBatchSize() + ")");
      }

      log.debug(
          "evt=validate_batch_and_student_success batchId={} studentId={} organizationId={}",
          batchId,
          studentId,
          organizationId);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=validate_batch_and_student_error batchId={} studentId={} organizationId={} error={}",
          batchId,
          studentId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to validate batch and student", e);
    }
  }

  private User getUser(String userId, String organizationId) {
    try {
      User user = userService.getUserById(userId).orElse(null);
      if (user == null) {
        throw UserException.userNotFound("User not found: " + userId);
      }

      if (!organizationId.equals(user.getOrganizationId())) {
        throw UserException.validationError("User does not belong to this organization");
      }

      return user;
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=get_user_error userId={} organizationId={} error={}",
          userId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve user", e);
    }
  }

  private Batch getBatch(String batchId, String organizationId) {
    try {
      Batch batch = dynamoDBMapper.load(Batch.class, organizationId, batchId);
      if (batch == null) {
        throw UserException.userNotFound("Batch not found: " + batchId);
      }

      if (!organizationId.equals(batch.getOrganizationId())) {
        throw UserException.validationError("Batch does not belong to this organization");
      }

      return batch;
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=get_batch_error batchId={} organizationId={} error={}",
          batchId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve batch", e);
    }
  }
}
