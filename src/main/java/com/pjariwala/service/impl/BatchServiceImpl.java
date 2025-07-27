package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.BatchRequestDTO;
import com.pjariwala.dto.BatchResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Batch;
import com.pjariwala.service.BatchService;
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

  @Override
  public BatchResponseDTO createBatch(BatchRequestDTO batchRequest) {
    log.info(
        "evt=create_batch_start batchName={} coachId={}",
        batchRequest.getBatchName(),
        batchRequest.getCoachId());

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
      int size) {
    log.info(
        "evt=get_all_batches_start page={} size={} status={} nameContains={} paymentType={}"
            + " coachId={}",
        page,
        size,
        status.orElse(null),
        nameContains.orElse(null),
        paymentType.orElse(null),
        coachId.orElse(null));

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
  public Optional<BatchResponseDTO> getBatchById(String batchId) {
    log.info("evt=get_batch_by_id_start batchId={}", batchId);
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
  public BatchResponseDTO updateBatch(String batchId, BatchRequestDTO batchRequest) {
    log.info(
        "evt=update_batch_start batchId={} batchName={}", batchId, batchRequest.getBatchName());

    validateBatchRequest(batchRequest);

    try {
      Batch existingBatch = dynamoDBMapper.load(Batch.class, batchId);
      if (existingBatch == null) {
        log.error("evt=update_batch_not_found batchId={}", batchId);
        throw UserException.userNotFound("Batch not found: " + batchId);
      }

      // Update fields
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
      if (batchRequest.getCoachId() != null) {
        existingBatch.setCoachId(batchRequest.getCoachId());
      }
      existingBatch.setUpdatedAt(LocalDateTime.now());

      dynamoDBMapper.save(existingBatch);
      log.info("evt=update_batch_success batchId={}", batchId);
      return convertToResponseDTO(existingBatch);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error("evt=update_batch_error batchId={}", batchId, e);
      throw UserException.databaseError("Failed to update batch: " + batchId, e);
    }
  }

  @Override
  public void deleteBatch(String batchId) {
    log.info("evt=delete_batch_start batchId={}", batchId);
    try {
      Batch existingBatch = dynamoDBMapper.load(Batch.class, batchId);
      if (existingBatch == null) {
        log.error("evt=delete_batch_not_found batchId={}", batchId);
        throw UserException.userNotFound("Batch not found: " + batchId);
      }

      // Soft delete by setting status to CANCELLED
      existingBatch.setBatchStatus(Batch.BatchStatus.CANCELLED);
      existingBatch.setUpdatedAt(LocalDateTime.now());
      dynamoDBMapper.save(existingBatch);

      log.info("evt=delete_batch_success batchId={}", batchId);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error("evt=delete_batch_error batchId={}", batchId, e);
      throw UserException.databaseError("Failed to delete batch: " + batchId, e);
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
  public PageResponseDTO<BatchResponseDTO> getBatchesByCoach(String coachId, int page, int size) {
    log.info("evt=get_batches_by_coach_start coachId={} page={} size={}", coachId, page, size);

    List<Batch> batches = getBatchesByCoachFromDb(coachId);
    return paginateResults(batches, page, size);
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

    log.debug("evt=validate_batch_request_success");
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
}
