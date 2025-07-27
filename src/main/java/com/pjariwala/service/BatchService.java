package com.pjariwala.service;

import com.pjariwala.dto.BatchRequestDTO;
import com.pjariwala.dto.BatchResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.model.Batch;
import java.util.Optional;

public interface BatchService {

  /** Create a new batch */
  BatchResponseDTO createBatch(BatchRequestDTO batchRequest);

  /** Get all batches with optional filtering and pagination */
  PageResponseDTO<BatchResponseDTO> getAllBatches(
      Optional<Batch.BatchStatus> status,
      Optional<String> nameContains,
      Optional<Batch.PaymentType> paymentType,
      Optional<String> coachId,
      int page,
      int size);

  /** Get batch by ID */
  Optional<BatchResponseDTO> getBatchById(String batchId);

  /** Update an existing batch */
  BatchResponseDTO updateBatch(String batchId, BatchRequestDTO batchRequest);

  /** Delete a batch (soft delete by setting status to CANCELLED) */
  void deleteBatch(String batchId);

  /** Check if a batch exists by ID */
  boolean batchExists(String batchId);

  /** Get batches by coach ID */
  PageResponseDTO<BatchResponseDTO> getBatchesByCoach(String coachId, int page, int size);

  /** Get batches by status */
  PageResponseDTO<BatchResponseDTO> getBatchesByStatus(
      Batch.BatchStatus status, int page, int size);

  /** Generate unique batch ID */
  String generateBatchId();
}
