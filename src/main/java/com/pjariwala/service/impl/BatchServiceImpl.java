package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.BatchRequestDTO;
import com.pjariwala.dto.BatchResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.BatchStatus;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.PaymentType;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.AuthException;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Batch;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.BatchService;
import com.pjariwala.service.EnrollmentService;
import com.pjariwala.service.UserService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BatchServiceImpl implements BatchService {

  @Autowired private DynamoDBMapper dynamoDBMapper;
  @Autowired private ActivityLogService activityLogService;
  @Autowired private UserService userService;
  @Autowired private EnrollmentService enrollmentService;

  @Override
  public BatchResponseDTO createBatch(
      BatchRequestDTO batchRequest, String requestingUserId, String organizationId) {
    log.info(
        "evt=create_batch_start batchName={} coachId={} requestingUserId={} organizationId={}",
        batchRequest.getBatchName(),
        batchRequest.getCoachId(),
        requestingUserId,
        organizationId);

    // Validate organization access
    validateOrganizationAccess(requestingUserId, organizationId);

    // Authorization: Only coaches can create batches
    requireCoachPermission(requestingUserId, organizationId);

    validateBatchRequest(batchRequest);

    Batch batch =
        Batch.builder()
            .organizationId(organizationId)
            .batchId(generateBatchId())
            .batchName(batchRequest.getBatchName())
            .batchSize(batchRequest.getBatchSize())
            // currentStudents is now calculated dynamically
            .startDate(batchRequest.getStartDate())
            .endDate(batchRequest.getEndDate())
            .batchTiming(convertTimingToEntity(batchRequest.getBatchTiming()))
            .paymentType(batchRequest.getPaymentType())
            .fixedMonthlyFee(batchRequest.getFixedMonthlyFee())
            .perSessionFee(batchRequest.getPerSessionFee())
            .occurrenceType(batchRequest.getOccurrenceType())
            .batchStatus(
                batchRequest.getBatchStatus() != null
                    ? batchRequest.getBatchStatus()
                    : BatchStatus.UPCOMING)
            .notes(batchRequest.getNotes())
            .coachId(batchRequest.getCoachId())
            .timezone(
                batchRequest.getTimezone() != null ? batchRequest.getTimezone() : "Asia/Kolkata")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    try {
      dynamoDBMapper.save(batch);
      log.info(
          "evt=create_batch_success batchId={} organizationId={}",
          batch.getBatchId(),
          organizationId);

      // Log batch creation activity
      try {
        User coach = getUser(batchRequest.getCoachId(), organizationId);
        activityLogService.logBatchCreation(
            coach.getUserId(),
            coach.getName(),
            batch.getBatchId(),
            batch.getBatchName(),
            organizationId);
      } catch (Exception e) {
        log.warn(
            "Failed to log batch creation activity for batch: {} organizationId={}",
            batch.getBatchId(),
            organizationId,
            e);
      }

      return convertToResponseDTO(batch);
    } catch (Exception e) {
      log.error(
          "evt=create_batch_error batchName={} organizationId={}",
          batchRequest.getBatchName(),
          organizationId,
          e);
      throw UserException.databaseError("Failed to create batch", e);
    }
  }

  @Override
  public PageResponseDTO<BatchResponseDTO> getAllBatches(
      Optional<BatchStatus> status,
      Optional<String> nameContains,
      Optional<PaymentType> paymentType,
      Optional<String> coachId,
      int page,
      int size,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=get_all_batches_start requestingUserId={} organizationId={} page={} size={}",
        requestingUserId,
        organizationId,
        page,
        size);

    // Validate organization access
    validateOrganizationAccess(requestingUserId, organizationId);

    try {
      // Get all batches for the organization
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":organizationId", new AttributeValue().withS(organizationId));

      DynamoDBQueryExpression<Batch> queryExpression =
          new DynamoDBQueryExpression<Batch>()
              .withKeyConditionExpression("organizationId = :organizationId")
              .withExpressionAttributeValues(eav);

      List<Batch> allBatches = dynamoDBMapper.query(Batch.class, queryExpression);

      // Apply filters
      List<Batch> filteredBatches = applyFilters(allBatches, status, nameContains, paymentType);

      // Apply coach filter if specified
      if (coachId.isPresent()) {
        filteredBatches =
            filteredBatches.stream()
                .filter(batch -> coachId.get().equals(batch.getCoachId()))
                .collect(Collectors.toList());
      }

      PageResponseDTO<BatchResponseDTO> result = paginateResults(filteredBatches, page, size);
      log.info(
          "evt=get_all_batches_success requestingUserId={} organizationId={} totalBatches={}"
              + " filteredBatches={}",
          requestingUserId,
          organizationId,
          allBatches.size(),
          filteredBatches.size());
      return result;

    } catch (Exception e) {
      log.error(
          "evt=get_all_batches_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve batches", e);
    }
  }

  @Override
  public Optional<BatchResponseDTO> getBatchById(
      String batchId, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_batch_by_id batchId={} requestingUserId={} organizationId={}",
        batchId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validateOrganizationAccess(requestingUserId, organizationId);

    try {
      Batch batch = dynamoDBMapper.load(Batch.class, organizationId, batchId);
      if (batch == null) {
        log.warn("evt=batch_not_found batchId={} organizationId={}", batchId, organizationId);
        return Optional.empty();
      }

      log.info(
          "evt=get_batch_by_id_success batchId={} requestingUserId={} organizationId={}",
          batchId,
          requestingUserId,
          organizationId);
      return Optional.of(convertToResponseDTO(batch));
    } catch (Exception e) {
      log.error(
          "evt=get_batch_by_id_error batchId={} requestingUserId={} organizationId={} error={}",
          batchId,
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve batch", e);
    }
  }

  @Override
  public Optional<BatchResponseDTO> updateBatch(
      String batchId,
      BatchRequestDTO batchRequest,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=update_batch_start batchId={} requestingUserId={} organizationId={}",
        batchId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validateOrganizationAccess(requestingUserId, organizationId);

    // Authorization: Only coaches can update batches
    requireCoachPermission(requestingUserId, organizationId);

    validateBatchRequest(batchRequest);

    try {
      Batch existingBatch = dynamoDBMapper.load(Batch.class, organizationId, batchId);
      if (existingBatch == null) {
        log.warn(
            "evt=update_batch_not_found batchId={} organizationId={}", batchId, organizationId);
        return Optional.empty();
      }

      // Update batch fields
      existingBatch.setBatchName(batchRequest.getBatchName());
      existingBatch.setBatchSize(batchRequest.getBatchSize());
      existingBatch.setStartDate(batchRequest.getStartDate());
      existingBatch.setEndDate(batchRequest.getEndDate());
      existingBatch.setBatchTiming(convertTimingToEntity(batchRequest.getBatchTiming()));
      existingBatch.setPaymentType(batchRequest.getPaymentType());
      existingBatch.setFixedMonthlyFee(batchRequest.getFixedMonthlyFee());
      existingBatch.setPerSessionFee(batchRequest.getPerSessionFee());
      existingBatch.setOccurrenceType(batchRequest.getOccurrenceType());
      existingBatch.setBatchStatus(batchRequest.getBatchStatus());
      existingBatch.setNotes(batchRequest.getNotes());
      existingBatch.setCoachId(batchRequest.getCoachId());
      existingBatch.setTimezone(batchRequest.getTimezone());
      existingBatch.setUpdatedAt(LocalDateTime.now());

      dynamoDBMapper.save(existingBatch);

      // Log batch update activity
      try {
        User coach = getUser(batchRequest.getCoachId(), organizationId);
        activityLogService.logAction(
            ActionType.UPDATE_BATCH,
            coach.getUserId(),
            coach.getName(),
            UserType.COACH,
            "Updated batch: " + batchRequest.getBatchName(),
            EntityType.BATCH,
            batchId,
            batchRequest.getBatchName(),
            organizationId);
      } catch (Exception e) {
        log.warn(
            "Failed to log batch update activity for batch: {} organizationId={}",
            batchId,
            organizationId,
            e);
      }

      log.info(
          "evt=update_batch_success batchId={} requestingUserId={} organizationId={}",
          batchId,
          requestingUserId,
          organizationId);
      return Optional.of(convertToResponseDTO(existingBatch));
    } catch (Exception e) {
      log.error(
          "evt=update_batch_error batchId={} requestingUserId={} organizationId={} error={}",
          batchId,
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to update batch", e);
    }
  }

  @Override
  public boolean deleteBatch(String batchId, String requestingUserId, String organizationId) {
    log.info(
        "evt=delete_batch_start batchId={} requestingUserId={} organizationId={}",
        batchId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validateOrganizationAccess(requestingUserId, organizationId);

    // Authorization: Only coaches can delete batches
    requireCoachPermission(requestingUserId, organizationId);

    try {
      Batch batch = dynamoDBMapper.load(Batch.class, organizationId, batchId);
      if (batch == null) {
        log.warn(
            "evt=delete_batch_not_found batchId={} organizationId={}", batchId, organizationId);
        return false;
      }

      // Check if batch has active enrollments
      // This would require checking enrollment service
      // For now, we'll allow deletion but log a warning
      log.warn(
          "evt=delete_batch_with_enrollments batchId={} organizationId={} - should check"
              + " enrollments",
          batchId,
          organizationId);

      dynamoDBMapper.delete(batch);

      // Log batch deletion activity
      try {
        User coach = getUser(batch.getCoachId(), organizationId);
        activityLogService.logAction(
            ActionType.DELETE_BATCH,
            coach.getUserId(),
            coach.getName(),
            UserType.COACH,
            "Deleted batch: " + batch.getBatchName(),
            EntityType.BATCH,
            batchId,
            batch.getBatchName(),
            organizationId);
      } catch (Exception e) {
        log.warn(
            "Failed to log batch deletion activity for batch: {} organizationId={}",
            batchId,
            organizationId,
            e);
      }

      log.info(
          "evt=delete_batch_success batchId={} requestingUserId={} organizationId={}",
          batchId,
          requestingUserId,
          organizationId);
      return true;
    } catch (Exception e) {
      log.error(
          "evt=delete_batch_error batchId={} requestingUserId={} organizationId={} error={}",
          batchId,
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to delete batch", e);
    }
  }

  @Override
  public boolean batchExists(String batchId, String organizationId) {
    try {
      Batch batch = dynamoDBMapper.load(Batch.class, organizationId, batchId);
      return batch != null;
    } catch (Exception e) {
      log.error(
          "evt=batch_exists_error batchId={} organizationId={} error={}",
          batchId,
          organizationId,
          e.getMessage(),
          e);
      return false;
    }
  }

  @Override
  public PageResponseDTO<BatchResponseDTO> getBatchesByCoach(
      String coachId, int page, int size, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_batches_by_coach coachId={} requestingUserId={} organizationId={} page={} size={}",
        coachId,
        requestingUserId,
        organizationId,
        page,
        size);

    // Validate organization access
    validateOrganizationAccess(requestingUserId, organizationId);

    try {
      List<Batch> batches = getBatchesByCoachFromDb(coachId, organizationId);
      PageResponseDTO<BatchResponseDTO> result = paginateResults(batches, page, size);

      log.info(
          "evt=get_batches_by_coach_success coachId={} requestingUserId={} organizationId={}"
              + " count={}",
          coachId,
          requestingUserId,
          organizationId,
          batches.size());
      return result;
    } catch (Exception e) {
      log.error(
          "evt=get_batches_by_coach_error coachId={} requestingUserId={} organizationId={}"
              + " error={}",
          coachId,
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve batches by coach", e);
    }
  }

  @Override
  public PageResponseDTO<BatchResponseDTO> getBatchesByStatus(
      BatchStatus status, int page, int size, String organizationId) {
    log.info(
        "evt=get_batches_by_status status={} organizationId={} page={} size={}",
        status,
        organizationId,
        page,
        size);

    try {
      List<Batch> batches = getBatchesByStatusFromDb(status, organizationId);
      PageResponseDTO<BatchResponseDTO> result = paginateResults(batches, page, size);

      log.info(
          "evt=get_batches_by_status_success status={} organizationId={} count={}",
          status,
          organizationId,
          batches.size());
      return result;
    } catch (Exception e) {
      log.error(
          "evt=get_batches_by_status_error status={} organizationId={} error={}",
          status,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve batches by status", e);
    }
  }

  @Override
  public String generateBatchId() {
    return "BATCH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  private void validateBatchRequest(BatchRequestDTO request) {
    if (request.getBatchName() == null || request.getBatchName().trim().isEmpty()) {
      throw UserException.validationError("Batch name is required");
    }

    if (request.getBatchSize() == null || request.getBatchSize() <= 0) {
      throw UserException.validationError("Batch size must be greater than 0");
    }

    if (request.getStartDate() == null) {
      throw UserException.validationError("Start date is required");
    }

    if (request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
      throw UserException.validationError("Start date cannot be after end date");
    }

    if (request.getPaymentType() == null) {
      throw UserException.validationError("Payment type is required");
    }

    if (request.getPaymentType() == PaymentType.FIXED_MONTHLY
        && request.getFixedMonthlyFee() == null) {
      throw UserException.validationError(
          "Fixed monthly fee is required for fixed monthly payment type");
    }

    if (request.getPaymentType() == PaymentType.PER_SESSION && request.getPerSessionFee() == null) {
      throw UserException.validationError(
          "Per session fee is required for per session payment type");
    }

    if (request.getCoachId() == null || request.getCoachId().trim().isEmpty()) {
      throw UserException.validationError("Coach ID is required");
    }

    validateBatchTiming(request.getBatchTiming());
  }

  private void validateCoachExists(String coachId, String organizationId) {
    try {
      User coach = userService.getUserById(coachId).orElse(null);
      if (coach == null) {
        throw UserException.validationError("Coach not found");
      }

      if (!organizationId.equals(coach.getOrganizationId())) {
        throw UserException.validationError("Coach does not belong to this organization");
      }

      if (!UserType.COACH.name().equals(coach.getUserType())) {
        throw UserException.validationError("User is not a coach");
      }
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=validate_coach_exists_error coachId={} organizationId={} error={}",
          coachId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to validate coach", e);
    }
  }

  private void validateBatchTiming(BatchRequestDTO.BatchTimingDTO timing) {
    if (timing == null) {
      throw UserException.validationError("Batch timing is required");
    }

    if (timing.getDaysOfWeek() == null || timing.getDaysOfWeek().isEmpty()) {
      throw UserException.validationError("Days of week are required");
    }

    for (String day : timing.getDaysOfWeek()) {
      if (!isValidDayOfWeek(day)) {
        throw UserException.validationError("Invalid day of week: " + day);
      }
    }

    if (timing.getStartTime() == null) {
      throw UserException.validationError("Start time is required");
    }

    if (timing.getEndTime() == null) {
      throw UserException.validationError("End time is required");
    }
  }

  private boolean isValidDayOfWeek(String day) {
    String[] validDays = {
      "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    };
    for (String validDay : validDays) {
      if (validDay.equals(day.toUpperCase())) {
        return true;
      }
    }
    return false;
  }

  private Batch.BatchTiming convertTimingToEntity(BatchRequestDTO.BatchTimingDTO timingDTO) {
    if (timingDTO == null) {
      return null;
    }
    Batch.BatchTiming timing = new Batch.BatchTiming();
    timing.setDaysOfWeek(timingDTO.getDaysOfWeek());
    timing.setStartTime(timingDTO.getStartTime());
    timing.setEndTime(timingDTO.getEndTime());
    return timing;
  }

  private BatchResponseDTO.BatchTimingDTO convertTimingToDTO(Batch.BatchTiming timing) {
    if (timing == null) {
      return null;
    }
    return new BatchResponseDTO.BatchTimingDTO(
        timing.getDaysOfWeek(), timing.getStartTime(), timing.getEndTime());
  }

  private BatchResponseDTO convertToResponseDTO(Batch batch) {
    // Calculate current students dynamically by counting active enrollments
    int currentStudents = 0;
    try {
      currentStudents =
          enrollmentService.countActiveEnrollmentsByBatch(
              batch.getBatchId(), batch.getOrganizationId());
    } catch (Exception e) {
      log.warn(
          "Failed to get current students count for batch: {} organizationId={}",
          batch.getBatchId(),
          batch.getOrganizationId(),
          e);
    }

    return new BatchResponseDTO(
        batch.getBatchId(),
        batch.getBatchName(),
        batch.getBatchSize(),
        currentStudents,
        batch.getStartDate(),
        batch.getEndDate(),
        convertTimingToDTO(batch.getBatchTiming()),
        batch.getPaymentType(),
        batch.getFixedMonthlyFee(),
        batch.getPerSessionFee(),
        batch.getOccurrenceType(),
        batch.getBatchStatus(),
        batch.getNotes(),
        batch.getCoachId(),
        batch.getTimezone(),
        batch.getCreatedAt(),
        batch.getUpdatedAt());
  }

  private List<Batch> getBatchesByCoachFromDb(String coachId, String organizationId) {
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":organizationId", new AttributeValue().withS(organizationId));
    eav.put(":coachId", new AttributeValue().withS(coachId));

    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("organizationId = :organizationId AND coachId = :coachId")
            .withExpressionAttributeValues(eav);

    return dynamoDBMapper.scan(Batch.class, scanExpression);
  }

  private List<Batch> getBatchesByStatusFromDb(BatchStatus status, String organizationId) {
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":organizationId", new AttributeValue().withS(organizationId));
    eav.put(":status", new AttributeValue().withS(status.name()));

    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("organizationId = :organizationId AND batchStatus = :status")
            .withExpressionAttributeValues(eav);

    return dynamoDBMapper.scan(Batch.class, scanExpression);
  }

  private List<Batch> applyFilters(
      List<Batch> batches,
      Optional<BatchStatus> status,
      Optional<String> nameContains,
      Optional<PaymentType> paymentType) {
    return batches.stream()
        .filter(batch -> status.isEmpty() || status.get() == batch.getBatchStatus())
        .filter(
            batch ->
                nameContains.isEmpty()
                    || batch
                        .getBatchName()
                        .toLowerCase()
                        .contains(nameContains.get().toLowerCase()))
        .filter(batch -> paymentType.isEmpty() || paymentType.get() == batch.getPaymentType())
        .collect(Collectors.toList());
  }

  private PageResponseDTO<BatchResponseDTO> paginateResults(
      List<Batch> batches, int page, int size) {
    int totalElements = batches.size();
    int totalPages = (int) Math.ceil((double) totalElements / size);
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, totalElements);

    List<Batch> paginatedBatches =
        startIndex < totalElements ? batches.subList(startIndex, endIndex) : new ArrayList<>();

    List<BatchResponseDTO> content =
        paginatedBatches.stream().map(this::convertToResponseDTO).collect(Collectors.toList());

    PageResponseDTO.PageInfoDTO pageInfo = new PageResponseDTO.PageInfoDTO();
    pageInfo.setCurrentPage(page);
    pageInfo.setPageSize(size);
    pageInfo.setTotalPages(totalPages);
    pageInfo.setTotalElements((long) totalElements);

    return new PageResponseDTO<>(content, pageInfo);
  }

  private User getUser(String userId, String organizationId) {
    try {
      User user = userService.getUserById(userId).orElse(null);
      if (user == null) {
        throw UserException.validationError("User not found: " + userId);
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

  /** Validate that the requesting user has access to the organization */
  private void validateOrganizationAccess(String requestingUserId, String organizationId) {
    try {
      User user = userService.getUserById(requestingUserId).orElse(null);
      if (user == null) {
        log.error(
            "evt=validate_org_access_user_not_found requestingUserId={} organizationId={}",
            requestingUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "User not found", 403);
      }

      if (!organizationId.equals(user.getOrganizationId())) {
        log.error(
            "evt=validate_org_access_denied requestingUserId={} userOrgId={} requestedOrgId={}",
            requestingUserId,
            user.getOrganizationId(),
            organizationId);
        throw new AuthException("ACCESS_DENIED", "You can only access your own organization", 403);
      }

      log.debug(
          "evt=validate_org_access_success requestingUserId={} organizationId={}",
          requestingUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=validate_org_access_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate organization access", 403);
    }
  }

  /** Require that the requesting user is a coach */
  private void requireCoachPermission(String requestingUserId, String organizationId) {
    try {
      User user = userService.getUserById(requestingUserId).orElse(null);
      if (user == null) {
        log.error(
            "evt=require_coach_permission_user_not_found requestingUserId={} organizationId={}",
            requestingUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "User not found", 403);
      }

      if (!UserType.COACH.name().equals(user.getUserType())) {
        log.error(
            "evt=require_coach_permission_denied requestingUserId={} userType={} organizationId={}",
            requestingUserId,
            user.getUserType(),
            organizationId);
        throw new AuthException("ACCESS_DENIED", "Only coaches can perform this action", 403);
      }

      log.debug(
          "evt=require_coach_permission_granted requestingUserId={} organizationId={}",
          requestingUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=require_coach_permission_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate coach permission", 403);
    }
  }
}
