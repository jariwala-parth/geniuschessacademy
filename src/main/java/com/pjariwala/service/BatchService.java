package com.pjariwala.service;

import com.pjariwala.dto.BatchRequestDTO;
import com.pjariwala.dto.BatchResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.enums.BatchStatus;
import com.pjariwala.enums.PaymentType;
import java.util.Optional;

public interface BatchService {

  /** Create a new batch - only coaches can create batches */
  BatchResponseDTO createBatch(
      BatchRequestDTO batchRequest, String requestingUserId, String organizationId);

  /** Get all batches with optional filtering and pagination */
  PageResponseDTO<BatchResponseDTO> getAllBatches(
      Optional<BatchStatus> status,
      Optional<String> nameContains,
      Optional<PaymentType> paymentType,
      Optional<String> coachId,
      int page,
      int size,
      String requestingUserId,
      String organizationId);

  /** Get batch by ID */
  Optional<BatchResponseDTO> getBatchById(
      String batchId, String requestingUserId, String organizationId);

  /** Update an existing batch - only coaches can update batches */
  Optional<BatchResponseDTO> updateBatch(
      String batchId, BatchRequestDTO batchRequest, String requestingUserId, String organizationId);

  /** Delete a batch - only coaches can delete batches */
  boolean deleteBatch(String batchId, String requestingUserId, String organizationId);

  /** Check if a batch exists by ID */
  boolean batchExists(String batchId, String organizationId);

  /** Get batches by coach ID */
  PageResponseDTO<BatchResponseDTO> getBatchesByCoach(
      String coachId, int page, int size, String requestingUserId, String organizationId);

  /** Get batches by status */
  PageResponseDTO<BatchResponseDTO> getBatchesByStatus(
      BatchStatus status, int page, int size, String organizationId);

  /** Generate unique batch ID */
  String generateBatchId();
}
