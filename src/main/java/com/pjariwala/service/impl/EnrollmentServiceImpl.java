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
import com.pjariwala.enums.ActionType;
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

  @Override
  public EnrollmentResponseDTO createEnrollment(
      EnrollmentRequestDTO enrollmentRequest, String requestingUserId) {
    log.info(
        "evt=create_enrollment_start batchId={} studentId={} requestingUserId={}",
        enrollmentRequest.getBatchId(),
        enrollmentRequest.getStudentId(),
        requestingUserId);

    // Authorization: Only coaches can enroll students
    requireCoachPermission(requestingUserId);

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
            .currentPaymentAmount(enrollmentRequest.getCurrentPaymentAmount())
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

      // Log enrollment activity
      try {
        User coach = getUser(requestingUserId);
        User student = getUser(enrollment.getStudentId());
        activityLogService.logEnrollment(
            coach.getUserId(), coach.getName(),
            student.getUserId(), student.getName(),
            enrollment.getBatchId(), "Batch" // We'll get the actual batch name if needed
            );
      } catch (Exception e) {
        log.warn(
            "Failed to log enrollment activity for batch: {} student: {}",
            enrollment.getBatchId(),
            enrollment.getStudentId(),
            e);
      }

      return convertToResponseDTO(enrollment);
    } catch (Exception e) {
      log.error("evt=create_enrollment_error", e);
      throw UserException.databaseError("Failed to create enrollment", e);
    }
  }

  @Override
  public Optional<EnrollmentResponseDTO> getEnrollment(
      String batchId, String studentId, String requestingUserId) {
    log.info(
        "evt=get_enrollment_start batchId={} studentId={} requestingUserId={}",
        batchId,
        studentId,
        requestingUserId);

    // Authorization: Students can only view their own enrollments, coaches can view all
    requireStudentAccessOrCoach(requestingUserId, studentId);

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
  public PageResponseDTO<EnrollmentResponseDTO> getAllEnrollments(
      Optional<String> batchId,
      Optional<String> studentId,
      int page,
      int size,
      String requestingUserId) {
    log.info(
        "evt=get_all_enrollments_start batchId={} studentId={} page={} size={} requestingUserId={}",
        batchId.orElse(null),
        studentId.orElse(null),
        page,
        size,
        requestingUserId);

    // Authorization: Students can only see their own enrollments
    User requestingUser = getUser(requestingUserId);
    if (!"COACH".equals(requestingUser.getUserType())) {
      // Force studentId to be the requesting user for students
      if (studentId.isEmpty() || !studentId.get().equals(requestingUserId)) {
        log.warn(
            "evt=get_all_enrollments_access_denied userId={} requestedStudentId={}",
            requestingUserId,
            studentId.orElse("none"));
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
        if (enrollment != null) {
          allEnrollments.add(enrollment);
        }
      } else if (batchId.isPresent()) {
        // Get enrollments by batch
        allEnrollments = getEnrollmentsByBatchFromDb(batchId.get());
      } else if (studentId.isPresent()) {
        // Get enrollments by student
        allEnrollments = getEnrollmentsByStudentFromDb(studentId.get());
      } else {
        // Get all enrollments (only for coaches)
        if ("COACH".equals(requestingUser.getUserType())) {
          DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
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

      log.info("evt=get_all_enrollments_success totalElements={}", totalElements);
      return new PageResponseDTO<>(enrollmentDTOs, pageInfo);
    } catch (Exception e) {
      log.error("evt=get_all_enrollments_error", e);
      throw UserException.databaseError("Failed to retrieve enrollments", e);
    }
  }

  @Override
  public Optional<EnrollmentResponseDTO> updateEnrollment(
      String batchId,
      String studentId,
      EnrollmentRequestDTO enrollmentRequest,
      String requestingUserId) {
    log.info(
        "evt=update_enrollment_start batchId={} studentId={} requestingUserId={}",
        batchId,
        studentId,
        requestingUserId);

    // Authorization: Only coaches can update enrollments
    requireCoachPermission(requestingUserId);

    try {
      Enrollment existingEnrollment = dynamoDBMapper.load(Enrollment.class, batchId, studentId);
      if (existingEnrollment == null) {
        log.info("evt=update_enrollment_not_found batchId={} studentId={}", batchId, studentId);
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
      log.info("evt=update_enrollment_success batchId={} studentId={}", batchId, studentId);
      return Optional.of(convertToResponseDTO(existingEnrollment));
    } catch (Exception e) {
      log.error("evt=update_enrollment_error batchId={} studentId={}", batchId, studentId, e);
      throw UserException.databaseError("Failed to update enrollment", e);
    }
  }

  @Override
  public boolean deleteEnrollment(String batchId, String studentId, String requestingUserId) {
    log.info(
        "evt=delete_enrollment_start batchId={} studentId={} requestingUserId={}",
        batchId,
        studentId,
        requestingUserId);

    // Authorization: Only coaches can delete enrollments
    requireCoachPermission(requestingUserId);

    try {
      Enrollment existingEnrollment = dynamoDBMapper.load(Enrollment.class, batchId, studentId);
      if (existingEnrollment == null) {
        log.info("evt=delete_enrollment_not_found batchId={} studentId={}", batchId, studentId);
        return false;
      }

      dynamoDBMapper.delete(existingEnrollment);
      log.info("evt=delete_enrollment_success batchId={} studentId={}", batchId, studentId);

      // Log enrollment removal activity
      try {
        User coach = getUser(requestingUserId);
        User student = getUser(studentId);
        activityLogService.logAction(
            ActionType.REMOVE_ENROLLMENT,
            coach.getUserId(),
            coach.getName(),
            UserType.COACH,
            String.format("Removed %s from batch", student.getName()),
            EntityType.ENROLLMENT,
            batchId + ":" + studentId,
            String.format("%s -> Batch", student.getName()));
      } catch (Exception e) {
        log.warn(
            "Failed to log enrollment removal activity for batch: {} student: {}",
            batchId,
            studentId,
            e);
      }

      return true;
    } catch (Exception e) {
      log.error("evt=delete_enrollment_error batchId={} studentId={}", batchId, studentId, e);
      throw UserException.databaseError("Failed to delete enrollment", e);
    }
  }

  @Override
  public PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByBatch(
      String batchId, int page, int size, String requestingUserId) {
    log.info(
        "evt=get_enrollments_by_batch_start batchId={} page={} size={} requestingUserId={}",
        batchId,
        page,
        size,
        requestingUserId);

    // Authorization: Coaches can see all enrollments, students can only see their own if they're in
    // the batch
    User requestingUser = getUser(requestingUserId);

    try {
      List<Enrollment> enrollments = getEnrollmentsByBatchFromDb(batchId);

      // Filter for non-coach users
      if (!"COACH".equals(requestingUser.getUserType())) {
        enrollments =
            enrollments.stream()
                .filter(enrollment -> enrollment.getStudentId().equals(requestingUserId))
                .collect(Collectors.toList());
      }

      // Apply pagination
      int totalElements = enrollments.size();
      int startIndex = page * size;
      int endIndex = Math.min(startIndex + size, totalElements);

      List<Enrollment> paginatedEnrollments =
          startIndex < totalElements
              ? enrollments.subList(startIndex, endIndex)
              : new ArrayList<>();

      List<EnrollmentResponseDTO> enrollmentDTOs =
          paginatedEnrollments.stream()
              .map(this::convertToResponseDTO)
              .collect(Collectors.toList());

      PageResponseDTO.PageInfoDTO pageInfo =
          new PageResponseDTO.PageInfoDTO(
              page, size, (totalElements + size - 1) / size, totalElements);

      log.info(
          "evt=get_enrollments_by_batch_success batchId={} totalElements={}",
          batchId,
          totalElements);
      return new PageResponseDTO<>(enrollmentDTOs, pageInfo);
    } catch (Exception e) {
      log.error("evt=get_enrollments_by_batch_error batchId={}", batchId, e);
      throw UserException.databaseError("Failed to retrieve enrollments by batch", e);
    }
  }

  @Override
  public PageResponseDTO<EnrollmentResponseDTO> getEnrollmentsByStudent(
      String studentId, int page, int size, String requestingUserId) {
    log.info(
        "evt=get_enrollments_by_student_start studentId={} page={} size={} requestingUserId={}",
        studentId,
        page,
        size,
        requestingUserId);

    // Authorization: Students can only view their own enrollments, coaches can view any
    requireStudentAccessOrCoach(requestingUserId, studentId);

    try {
      List<Enrollment> enrollments = getEnrollmentsByStudentFromDb(studentId);

      // Apply pagination
      int totalElements = enrollments.size();
      int startIndex = page * size;
      int endIndex = Math.min(startIndex + size, totalElements);

      List<Enrollment> paginatedEnrollments =
          startIndex < totalElements
              ? enrollments.subList(startIndex, endIndex)
              : new ArrayList<>();

      List<EnrollmentResponseDTO> enrollmentDTOs =
          paginatedEnrollments.stream()
              .map(this::convertToResponseDTO)
              .collect(Collectors.toList());

      PageResponseDTO.PageInfoDTO pageInfo =
          new PageResponseDTO.PageInfoDTO(
              page, size, (totalElements + size - 1) / size, totalElements);

      log.info(
          "evt=get_enrollments_by_student_success studentId={} totalElements={}",
          studentId,
          totalElements);
      return new PageResponseDTO<>(enrollmentDTOs, pageInfo);
    } catch (Exception e) {
      log.error("evt=get_enrollments_by_student_error studentId={}", studentId, e);
      throw UserException.databaseError("Failed to retrieve enrollments by student", e);
    }
  }

  @Override
  public List<EnrollmentResponseDTO> createBulkEnrollments(
      List<EnrollmentRequestDTO> enrollmentRequests, String requestingUserId) {
    log.info(
        "evt=create_bulk_enrollments_start count={} requestingUserId={}",
        enrollmentRequests.size(),
        requestingUserId);

    // Authorization: Only coaches can enroll students
    requireCoachPermission(requestingUserId);

    List<EnrollmentResponseDTO> results = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    for (EnrollmentRequestDTO request : enrollmentRequests) {
      try {
        // Validate each request
        validateEnrollmentRequest(request);

        // Check if enrollment already exists
        if (isStudentEnrolled(request.getBatchId(), request.getStudentId())) {
          errors.add(
              String.format(
                  "Student %s already enrolled in batch %s",
                  request.getStudentId(), request.getBatchId()));
          continue;
        }

        // Verify batch and student exist
        validateBatchAndStudent(request.getBatchId(), request.getStudentId());

        Enrollment enrollment =
            Enrollment.builder()
                .batchId(request.getBatchId())
                .studentId(request.getStudentId())
                .enrollmentDate(
                    request.getEnrollmentDate() != null
                        ? request.getEnrollmentDate()
                        : java.time.LocalDate.now())
                .enrollmentStatus(
                    request.getEnrollmentStatus() != null
                        ? request.getEnrollmentStatus()
                        : Enrollment.EnrollmentStatus.ENROLLED)
                .enrollmentPaymentStatus(
                    request.getEnrollmentPaymentStatus() != null
                        ? request.getEnrollmentPaymentStatus()
                        : Enrollment.PaymentStatus.PENDING)
                .currentPaymentAmount(request.getCurrentPaymentAmount())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        dynamoDBMapper.save(enrollment);
        results.add(convertToResponseDTO(enrollment));

        log.info(
            "evt=bulk_enrollment_success batchId={} studentId={}",
            enrollment.getBatchId(),
            enrollment.getStudentId());

      } catch (Exception e) {
        log.error(
            "evt=bulk_enrollment_error batchId={} studentId={}",
            request.getBatchId(),
            request.getStudentId(),
            e);
        errors.add(
            String.format(
                "Failed to enroll student %s in batch %s: %s",
                request.getStudentId(), request.getBatchId(), e.getMessage()));
      }
    }

    log.info(
        "evt=create_bulk_enrollments_complete successful={} failed={}",
        results.size(),
        errors.size());

    // Log bulk enrollment activity
    try {
      User coach = getUser(requestingUserId);
      activityLogService.logAction(
          ActionType.BULK_ENROLLMENT,
          coach.getUserId(),
          coach.getName(),
          UserType.COACH,
          String.format("Bulk enrolled %d students", results.size()),
          EntityType.ENROLLMENT,
          null,
          null);
    } catch (Exception e) {
      log.warn("Failed to log bulk enrollment activity for coach: {}", requestingUserId, e);
    }

    if (!errors.isEmpty()) {
      log.warn("evt=create_bulk_enrollments_partial_failure errors={}", String.join("; ", errors));
      // For bulk operations, we return partial success rather than throwing
      // The caller can check the result count vs request count
    }

    return results;
  }

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

  private void validateEnrollmentRequest(EnrollmentRequestDTO request) {
    log.debug("evt=validate_enrollment_request_start");

    if (request.getBatchId() == null || request.getBatchId().trim().isEmpty()) {
      throw UserException.validationError("Batch ID is required");
    }
    if (request.getStudentId() == null || request.getStudentId().trim().isEmpty()) {
      throw UserException.validationError("Student ID is required");
    }

    // Validate batch and student exist
    validateBatchAndStudent(request.getBatchId(), request.getStudentId());

    log.debug("evt=validate_enrollment_request_success");
  }

  private void validateBatchAndStudent(String batchId, String studentId) {
    log.debug("evt=validate_batch_and_student_start batchId={} studentId={}", batchId, studentId);

    try {
      // Check if batch exists
      Batch batch = dynamoDBMapper.load(Batch.class, batchId);
      if (batch == null) {
        log.error("evt=validate_batch_not_found batchId={}", batchId);
        throw UserException.userNotFound("Batch not found: " + batchId);
      }

      if (batch.getBatchStatus() == Batch.BatchStatus.CANCELLED) {
        log.error("evt=validate_batch_cancelled batchId={}", batchId);
        throw UserException.validationError("Cannot enroll in cancelled batch: " + batchId);
      }

      // Check if student exists
      User student = dynamoDBMapper.load(User.class, studentId, "STUDENT");
      if (student == null) {
        log.error("evt=validate_student_not_found studentId={}", studentId);
        throw UserException.userNotFound("Student not found: " + studentId);
      }

      if (student.getIsActive() != null && !student.getIsActive()) {
        log.error("evt=validate_student_inactive studentId={}", studentId);
        throw UserException.validationError("Cannot enroll inactive student: " + studentId);
      }

      // Check if batch is full
      if (batch.getCurrentStudents() != null
          && batch.getBatchSize() != null
          && batch.getCurrentStudents() >= batch.getBatchSize()) {
        log.error(
            "evt=validate_batch_full batchId={} current={} max={}",
            batchId,
            batch.getCurrentStudents(),
            batch.getBatchSize());
        throw UserException.validationError(
            "Batch is full (capacity: " + batch.getBatchSize() + ")");
      }

      log.debug(
          "evt=validate_batch_and_student_success batchId={} studentId={}", batchId, studentId);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=validate_batch_and_student_error batchId={} studentId={}", batchId, studentId, e);
      throw UserException.databaseError("Failed to validate batch and student", e);
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

  /** Get user by ID and throw exception if not found */
  private User getUser(String userId) {
    // Try both COACH and STUDENT types since we don't know which type the user is
    User user = userService.getUserById(userId, "COACH");
    if (user == null) {
      user = userService.getUserById(userId, "STUDENT");
    }
    if (user == null) {
      log.error("evt=get_user_error userId={} msg=user_not_found", userId);
      throw AuthException.invalidToken();
    }
    return user;
  }

  /** Require that the requesting user is a coach */
  private void requireCoachPermission(String userId) {
    User user = getUser(userId);
    if (!"COACH".equals(user.getUserType())) {
      log.error(
          "evt=require_coach_permission_error userId={} userType={} msg=access_denied",
          userId,
          user.getUserType());
      throw new AuthException("ACCESS_DENIED", "Only coaches can perform this action", 403);
    }
    log.debug("evt=require_coach_permission_success userId={}", userId);
  }

  /**
   * Require that the requesting user can access student data (either the student themselves or a
   * coach)
   */
  private void requireStudentAccessOrCoach(String requestingUserId, String studentId) {
    User requestingUser = getUser(requestingUserId);

    // Coaches can access any student data
    if ("COACH".equals(requestingUser.getUserType())) {
      log.debug(
          "evt=require_student_access_success userId={} role=coach studentId={}",
          requestingUserId,
          studentId);
      return;
    }

    // Students can only access their own data
    if (!requestingUserId.equals(studentId)) {
      log.error(
          "evt=require_student_access_error userId={} studentId={} msg=access_denied",
          requestingUserId,
          studentId);
      throw new AuthException("ACCESS_DENIED", "You can only access your own data", 403);
    }

    log.debug("evt=require_student_access_success userId={} role=student", requestingUserId);
  }

  private List<Enrollment> getEnrollmentsByBatchFromDb(String batchId) {
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":batchId", new AttributeValue().withS(batchId));

    DynamoDBQueryExpression<Enrollment> queryExpression =
        new DynamoDBQueryExpression<Enrollment>()
            .withKeyConditionExpression("batchId = :batchId")
            .withExpressionAttributeValues(eav);

    PaginatedQueryList<Enrollment> queryResult =
        dynamoDBMapper.query(Enrollment.class, queryExpression);
    return new ArrayList<>(queryResult);
  }

  private List<Enrollment> getEnrollmentsByStudentFromDb(String studentId) {
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
    return new ArrayList<>(queryResult);
  }
}
