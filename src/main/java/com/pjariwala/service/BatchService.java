package com.pjariwala.service;

import com.pjariwala.dto.BatchRequestDTO;
import com.pjariwala.dto.BatchResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.model.Batch;
import java.util.Optional;

public interface BatchService {

  /** Create a new batch - only coaches can create batches */
  BatchResponseDTO createBatch(BatchRequestDTO batchRequest, String requestingUserId);

  /** Get all batches with optional filtering and pagination */
  PageResponseDTO<BatchResponseDTO> getAllBatches(
      Optional<Batch.BatchStatus> status,
      Optional<String> nameContains,
      Optional<Batch.PaymentType> paymentType,
      Optional<String> coachId,
      int page,
      int size,
      String requestingUserId);

  /** Get batch by ID */
  Optional<BatchResponseDTO> getBatchById(String batchId, String requestingUserId);

  /** Update an existing batch - only coaches can update batches */
  Optional<BatchResponseDTO> updateBatch(
      String batchId, BatchRequestDTO batchRequest, String requestingUserId);

  /** Delete a batch - only coaches can delete batches */
  boolean deleteBatch(String batchId, String requestingUserId);

  /** Check if a batch exists by ID */
  boolean batchExists(String batchId);

  /** Get batches by coach ID */
  PageResponseDTO<BatchResponseDTO> getBatchesByCoach(
      String coachId, int page, int size, String requestingUserId);

  /** Get batches by status */
  PageResponseDTO<BatchResponseDTO> getBatchesByStatus(
      Batch.BatchStatus status, int page, int size);

  /** Generate unique batch ID */
  String generateBatchId();
}
