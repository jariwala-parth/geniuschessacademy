package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.BatchRequestDTO;
import com.pjariwala.dto.BatchResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.AuthException;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Batch;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.BatchService;
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

  @Override
  public BatchResponseDTO createBatch(BatchRequestDTO batchRequest, String requestingUserId) {
    log.info(
        "evt=create_batch_start batchName={} coachId={} requestingUserId={}",
        batchRequest.getBatchName(),
        batchRequest.getCoachId(),
        requestingUserId);

    // Authorization: Only coaches can create batches
    requireCoachPermission(requestingUserId);

    validateBatchRequest(batchRequest);

    Batch batch =
        Batch.builder()
            .batchId(generateBatchId())
            .batchName(batchRequest.getBatchName())
            .batchSize(batchRequest.getBatchSize())
            .currentStudents(0) // Initial count
            .startDate(batchRequest.getStartDate())
            .endDate(batchRequest.getEndDate())
            .batchTiming(convertTimingToEntity(batchRequest.getBatchTiming()))
            .paymentType(batchRequest.getPaymentType())
            .monthlyFee(batchRequest.getMonthlyFee())
            .oneTimeFee(batchRequest.getOneTimeFee())
            .occurrenceType(batchRequest.getOccurrenceType())
            .batchStatus(
                batchRequest.getBatchStatus() != null
                    ? batchRequest.getBatchStatus()
                    : Batch.BatchStatus.UPCOMING)
            .notes(batchRequest.getNotes())
            .coachId(batchRequest.getCoachId())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    try {
      dynamoDBMapper.save(batch);
      log.info("evt=create_batch_success batchId={}", batch.getBatchId());

      // Log batch creation activity
      try {
        User coach = getUser(batchRequest.getCoachId());
        activityLogService.logBatchCreation(
            coach.getUserId(), coach.getName(), batch.getBatchId(), batch.getBatchName());
      } catch (Exception e) {
        log.warn("Failed to log batch creation activity for batch: {}", batch.getBatchId(), e);
      }

      return convertToResponseDTO(batch);
    } catch (Exception e) {
      log.error("evt=create_batch_error batchName={}", batchRequest.getBatchName(), e);
      throw UserException.databaseError("Failed to create batch", e);
    }
  }

  @Override
  public PageResponseDTO<BatchResponseDTO> getAllBatches(
      Optional<Batch.BatchStatus> status,
      Optional<String> nameContains,
      Optional<Batch.PaymentType> paymentType,
      Optional<String> coachId,
      int page,
      int size,
      String requestingUserId) {
    log.info(
        "evt=get_all_batches_start page={} size={} status={} nameContains={} paymentType={}"
            + " coachId={} requestingUserId={}",
        page,
        size,
        status.orElse(null),
        nameContains.orElse(null),
        paymentType.orElse(null),
        coachId.orElse(null),
        requestingUserId);

    // Authorization: Students can only see their enrolled batches, coaches can see all
    User requestingUser = getUser(requestingUserId);

    try {
      List<Batch> allBatches;

      if (coachId.isPresent()) {
        // Use GSI for coach-based queries
        allBatches = getBatchesByCoachFromDb(coachId.get());
      } else if (status.isPresent()) {
        // Use GSI for status-based queries
        allBatches = getBatchesByStatusFromDb(status.get());
      } else {
        // Full scan
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        PaginatedScanList<Batch> scanResult = dynamoDBMapper.scan(Batch.class, scanExpression);
        allBatches = new ArrayList<>(scanResult);
      }

      // Apply authorization filter if user is not a coach
      if (!"COACH".equals(requestingUser.getUserType())) {
        // Students can only see their enrolled batches - this would require enrollment service
        // For now, students can see all batches but with limited information
        log.debug("evt=get_all_batches student_limited_view userId={}", requestingUserId);
      }

      // Apply additional filters
      List<Batch> filteredBatches = applyFilters(allBatches, status, nameContains, paymentType);

      // Apply pagination
      int totalElements = filteredBatches.size();
      int startIndex = page * size;
      int endIndex = Math.min(startIndex + size, totalElements);

      List<Batch> paginatedBatches =
          startIndex < totalElements
              ? filteredBatches.subList(startIndex, endIndex)
              : new ArrayList<>();

      List<BatchResponseDTO> batchDTOs =
          paginatedBatches.stream().map(this::convertToResponseDTO).collect(Collectors.toList());

      PageResponseDTO.PageInfoDTO pageInfo =
          new PageResponseDTO.PageInfoDTO(
              page, size, (totalElements + size - 1) / size, totalElements);

      log.info("evt=get_all_batches_success totalElements={}", totalElements);
      return new PageResponseDTO<>(batchDTOs, pageInfo);
    } catch (Exception e) {
      log.error("evt=get_all_batches_error", e);
      throw UserException.databaseError("Failed to retrieve batches", e);
    }
  }

  @Override
  public Optional<BatchResponseDTO> getBatchById(String batchId, String requestingUserId) {
    log.info("evt=get_batch_by_id_start batchId={} requestingUserId={}", batchId, requestingUserId);

    // Note: No strict authorization here - both students and coaches can view batch details

    try {
      Batch batch = dynamoDBMapper.load(Batch.class, batchId);
      if (batch != null) {
        log.info("evt=get_batch_by_id_success batchId={}", batchId);
        return Optional.of(convertToResponseDTO(batch));
      } else {
        log.info("evt=get_batch_by_id_not_found batchId={}", batchId);
        return Optional.empty();
      }
    } catch (Exception e) {
      log.error("evt=get_batch_by_id_error batchId={}", batchId, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<BatchResponseDTO> updateBatch(
      String batchId, BatchRequestDTO batchRequest, String requestingUserId) {
    log.info("evt=update_batch_start batchId={} requestingUserId={}", batchId, requestingUserId);

    // Authorization: Only coaches can update batches
    requireCoachPermission(requestingUserId);

    try {
      Batch existingBatch = dynamoDBMapper.load(Batch.class, batchId);
      if (existingBatch == null) {
        log.info("evt=update_batch_not_found batchId={}", batchId);
        return Optional.empty();
      }

      validateBatchRequest(batchRequest);

      // Update batch fields
      existingBatch.setBatchName(batchRequest.getBatchName());
      existingBatch.setBatchSize(batchRequest.getBatchSize());
      existingBatch.setStartDate(batchRequest.getStartDate());
      existingBatch.setEndDate(batchRequest.getEndDate());
      existingBatch.setBatchTiming(convertTimingToEntity(batchRequest.getBatchTiming()));
      existingBatch.setPaymentType(batchRequest.getPaymentType());
      existingBatch.setMonthlyFee(batchRequest.getMonthlyFee());
      existingBatch.setOneTimeFee(batchRequest.getOneTimeFee());
      existingBatch.setOccurrenceType(batchRequest.getOccurrenceType());
      if (batchRequest.getBatchStatus() != null) {
        existingBatch.setBatchStatus(batchRequest.getBatchStatus());
      }
      existingBatch.setNotes(batchRequest.getNotes());
      existingBatch.setCoachId(batchRequest.getCoachId());
      existingBatch.setUpdatedAt(LocalDateTime.now());

      dynamoDBMapper.save(existingBatch);

      log.info("evt=update_batch_success batchId={}", batchId);

      // Log batch update activity
      try {
        User coach = getUser(requestingUserId);
        activityLogService.logAction(
            ActionType.UPDATE_BATCH,
            coach.getUserId(),
            coach.getName(),
            UserType.COACH,
            String.format("Updated batch: %s", existingBatch.getBatchName()),
            EntityType.BATCH,
            existingBatch.getBatchId(),
            existingBatch.getBatchName());
      } catch (Exception e) {
        log.warn("Failed to log batch update activity for batch: {}", batchId, e);
      }

      return Optional.of(convertToResponseDTO(existingBatch));
    } catch (Exception e) {
      log.error("evt=update_batch_error batchId={}", batchId, e);
      throw UserException.databaseError("Failed to update batch", e);
    }
  }

  @Override
  public boolean deleteBatch(String batchId, String requestingUserId) {
    log.info("evt=delete_batch_start batchId={} requestingUserId={}", batchId, requestingUserId);

    // Authorization: Only coaches can delete batches
    requireCoachPermission(requestingUserId);

    try {
      Batch existingBatch = dynamoDBMapper.load(Batch.class, batchId);
      if (existingBatch == null) {
        log.info("evt=delete_batch_not_found batchId={}", batchId);
        return false;
      }

      // Soft delete by setting status to CANCELLED
      existingBatch.setBatchStatus(Batch.BatchStatus.CANCELLED);
      existingBatch.setUpdatedAt(LocalDateTime.now());
      dynamoDBMapper.save(existingBatch);

      log.info("evt=delete_batch_success batchId={}", batchId);

      // Log batch deletion activity
      try {
        User coach = getUser(requestingUserId);
        activityLogService.logAction(
            ActionType.DELETE_BATCH,
            coach.getUserId(),
            coach.getName(),
            UserType.COACH,
            String.format("Deleted batch: %s", existingBatch.getBatchName()),
            EntityType.BATCH,
            existingBatch.getBatchId(),
            existingBatch.getBatchName());
      } catch (Exception e) {
        log.warn("Failed to log batch deletion activity for batch: {}", batchId, e);
      }

      return true;
    } catch (Exception e) {
      log.error("evt=delete_batch_error batchId={}", batchId, e);
      throw UserException.databaseError("Failed to delete batch", e);
    }
  }

  @Override
  public boolean batchExists(String batchId) {
    try {
      Batch batch = dynamoDBMapper.load(Batch.class, batchId);
      return batch != null && batch.getBatchStatus() != Batch.BatchStatus.CANCELLED;
    } catch (Exception e) {
      log.error("evt=batch_exists_error batchId={}", batchId, e);
      return false;
    }
  }

  @Override
  public PageResponseDTO<BatchResponseDTO> getBatchesByCoach(
      String coachId, int page, int size, String requestingUserId) {
    log.info(
        "evt=get_batches_by_coach_start coachId={} page={} size={} requestingUserId={}",
        coachId,
        page,
        size,
        requestingUserId);

    // Authorization: Anyone can view batches by coach (public information)

    try {
      List<Batch> batches = getBatchesByCoachFromDb(coachId);

      // Apply pagination
      int totalElements = batches.size();
      int startIndex = page * size;
      int endIndex = Math.min(startIndex + size, totalElements);

      List<Batch> paginatedBatches =
          startIndex < totalElements ? batches.subList(startIndex, endIndex) : new ArrayList<>();

      List<BatchResponseDTO> batchDTOs =
          paginatedBatches.stream().map(this::convertToResponseDTO).collect(Collectors.toList());

      PageResponseDTO.PageInfoDTO pageInfo =
          new PageResponseDTO.PageInfoDTO(
              page, size, (totalElements + size - 1) / size, totalElements);

      log.info(
          "evt=get_batches_by_coach_success coachId={} totalElements={}", coachId, totalElements);
      return new PageResponseDTO<>(batchDTOs, pageInfo);
    } catch (Exception e) {
      log.error("evt=get_batches_by_coach_error coachId={}", coachId, e);
      throw UserException.databaseError("Failed to retrieve batches by coach", e);
    }
  }

  @Override
  public PageResponseDTO<BatchResponseDTO> getBatchesByStatus(
      Batch.BatchStatus status, int page, int size) {
    log.info("evt=get_batches_by_status_start status={} page={} size={}", status, page, size);

    List<Batch> batches = getBatchesByStatusFromDb(status);
    return paginateResults(batches, page, size);
  }

  @Override
  public String generateBatchId() {
    return "BATCH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  // Helper methods

  private void validateBatchRequest(BatchRequestDTO request) {
    log.debug("evt=validate_batch_request_start");

    if (request.getBatchName() == null || request.getBatchName().trim().isEmpty()) {
      throw UserException.validationError("Batch name is required");
    }
    if (request.getBatchSize() == null || request.getBatchSize() <= 0) {
      throw UserException.validationError("Batch size must be greater than 0");
    }
    if (request.getStartDate() == null) {
      throw UserException.validationError("Start date is required");
    }
    if (request.getEndDate() == null) {
      throw UserException.validationError("End date is required");
    }
    if (request.getEndDate().isBefore(request.getStartDate())) {
      throw UserException.validationError("End date must be after start date");
    }
    if (request.getPaymentType() == null) {
      throw UserException.validationError("Payment type is required");
    }
    if (request.getPaymentType() == Batch.PaymentType.MONTHLY && request.getMonthlyFee() == null) {
      throw UserException.validationError("Monthly fee is required for MONTHLY payment type");
    }
    if (request.getPaymentType() == Batch.PaymentType.ONE_TIME && request.getOneTimeFee() == null) {
      throw UserException.validationError("One-time fee is required for ONE_TIME payment type");
    }
    if (request.getBatchTiming() == null) {
      throw UserException.validationError("Batch timing is required");
    }
    if (request.getCoachId() == null || request.getCoachId().trim().isEmpty()) {
      throw UserException.validationError("Coach ID is required");
    }

    // Validate coach exists and is actually a coach
    validateCoachExists(request.getCoachId());

    // Validate batch timing details
    validateBatchTiming(request.getBatchTiming());

    log.debug("evt=validate_batch_request_success");
  }

  private void validateCoachExists(String coachId) {
    log.debug("evt=validate_coach_exists_start coachId={}", coachId);

    try {
      User coach =
          userService
              .getUserById(coachId)
              .orElseThrow(
                  () -> {
                    log.error("evt=validate_coach_exists_not_found coachId={}", coachId);
                    return UserException.validationError("Coach not found with ID: " + coachId);
                  });

      if (!"COACH".equals(coach.getUserType())) {
        log.error(
            "evt=validate_coach_exists_not_coach coachId={} userType={}",
            coachId,
            coach.getUserType());
        throw UserException.validationError("User with ID " + coachId + " is not a coach");
      }

      if (coach.getIsActive() != null && !coach.getIsActive()) {
        log.error("evt=validate_coach_exists_inactive coachId={}", coachId);
        throw UserException.validationError("Coach with ID " + coachId + " is not active");
      }

      log.debug("evt=validate_coach_exists_success coachId={}", coachId);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error("evt=validate_coach_exists_error coachId={}", coachId, e);
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
    if (timing.getStartTime() == null) {
      throw UserException.validationError("Start time is required");
    }
    if (timing.getEndTime() == null) {
      throw UserException.validationError("End time is required");
    }
    if (timing.getEndTime().isBefore(timing.getStartTime())
        || timing.getEndTime().equals(timing.getStartTime())) {
      throw UserException.validationError("End time must be after start time");
    }

    // Validate days of week
    for (String day : timing.getDaysOfWeek()) {
      if (!isValidDayOfWeek(day)) {
        throw UserException.validationError("Invalid day of week: " + day);
      }
    }
  }

  private boolean isValidDayOfWeek(String day) {
    return day != null
        && List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
            .contains(day.toUpperCase());
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
    return new BatchResponseDTO(
        batch.getBatchId(),
        batch.getBatchName(),
        batch.getBatchSize(),
        batch.getCurrentStudents(),
        batch.getStartDate(),
        batch.getEndDate(),
        convertTimingToDTO(batch.getBatchTiming()),
        batch.getPaymentType(),
        batch.getMonthlyFee(),
        batch.getOneTimeFee(),
        batch.getOccurrenceType(),
        batch.getBatchStatus(),
        batch.getNotes(),
        batch.getCoachId(),
        batch.getCreatedAt(),
        batch.getUpdatedAt());
  }

  private List<Batch> getBatchesByCoachFromDb(String coachId) {
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":coachId", new AttributeValue().withS(coachId));

    DynamoDBQueryExpression<Batch> queryExpression =
        new DynamoDBQueryExpression<Batch>()
            .withIndexName("coachId-index")
            .withConsistentRead(false)
            .withKeyConditionExpression("coachId = :coachId")
            .withExpressionAttributeValues(eav);

    return new ArrayList<>(dynamoDBMapper.query(Batch.class, queryExpression));
  }

  private List<Batch> getBatchesByStatusFromDb(Batch.BatchStatus status) {
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":status", new AttributeValue().withS(status.toString()));

    DynamoDBQueryExpression<Batch> queryExpression =
        new DynamoDBQueryExpression<Batch>()
            .withIndexName("batchStatus-startDate-index")
            .withConsistentRead(false)
            .withKeyConditionExpression("batchStatus = :status")
            .withExpressionAttributeValues(eav);

    return new ArrayList<>(dynamoDBMapper.query(Batch.class, queryExpression));
  }

  private List<Batch> applyFilters(
      List<Batch> batches,
      Optional<Batch.BatchStatus> status,
      Optional<String> nameContains,
      Optional<Batch.PaymentType> paymentType) {

    return batches.stream()
        .filter(
            batch -> {
              if (status.isPresent() && !batch.getBatchStatus().equals(status.get())) {
                return false;
              }
              if (nameContains.isPresent()
                  && !batch
                      .getBatchName()
                      .toLowerCase()
                      .contains(nameContains.get().toLowerCase())) {
                return false;
              }
              if (paymentType.isPresent() && !batch.getPaymentType().equals(paymentType.get())) {
                return false;
              }
              return true;
            })
        .collect(Collectors.toList());
  }

  private PageResponseDTO<BatchResponseDTO> paginateResults(
      List<Batch> batches, int page, int size) {
    int totalElements = batches.size();
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, totalElements);

    List<Batch> paginatedBatches =
        startIndex < totalElements ? batches.subList(startIndex, endIndex) : new ArrayList<>();

    List<BatchResponseDTO> batchDTOs =
        paginatedBatches.stream().map(this::convertToResponseDTO).collect(Collectors.toList());

    PageResponseDTO.PageInfoDTO pageInfo =
        new PageResponseDTO.PageInfoDTO(
            page, size, (totalElements + size - 1) / size, totalElements);

    return new PageResponseDTO<>(batchDTOs, pageInfo);
  }

  /** Get user by ID and throw exception if not found */
  private User getUser(String userId) {
    return userService
        .getUserById(userId)
        .orElseThrow(
            () -> {
              log.error("evt=get_user_error userId={} msg=user_not_found", userId);
              return AuthException.invalidToken();
            });
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
}
