package com.pjariwala.controller;

import com.pjariwala.dto.BatchRequestDTO;
import com.pjariwala.dto.BatchResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.model.Batch;
import com.pjariwala.service.BatchService;
import com.pjariwala.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batches")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Batch Management", description = "APIs for managing chess batches")
public class BatchController {

  @Autowired private BatchService batchService;

  @Autowired private AuthUtil authUtil;

  @PostMapping
  @Operation(
      summary = "Create a new batch",
      description = "Create a new chess batch. Only coaches can create batches.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<BatchResponseDTO> createBatch(
      @RequestBody BatchRequestDTO batchRequest,
      @Parameter(hidden = true) @RequestHeader("Authorization") String authorization) {
    log.info(
        "evt=create_batch_request batchName={} coachId={}",
        batchRequest.getBatchName(),
        batchRequest.getCoachId());
    try {
      // Only coaches can create batches
      authUtil.requireCoach(authorization);

      BatchResponseDTO response = batchService.createBatch(batchRequest);
      log.info("evt=create_batch_success batchId={}", response.getBatchId());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
      log.error("evt=create_batch_error batchName={}", batchRequest.getBatchName(), e);
      throw e;
    }
  }

  @GetMapping
  public ResponseEntity<PageResponseDTO<BatchResponseDTO>> getAllBatches(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String nameContains,
      @RequestParam(required = false) String paymentType,
      @RequestParam(required = false) String coachId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info(
        "evt=get_all_batches_request received page={} size={} status={} nameContains={}"
            + " paymentType={} coachId={}",
        page,
        size,
        status,
        nameContains,
        paymentType,
        coachId);
    try {
      Optional<Batch.BatchStatus> statusEnum = Optional.empty();
      if (status != null) {
        try {
          statusEnum = Optional.of(Batch.BatchStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn("evt=get_all_batches_request invalid_status status={}", status);
        }
      }

      Optional<Batch.PaymentType> paymentTypeEnum = Optional.empty();
      if (paymentType != null) {
        try {
          paymentTypeEnum = Optional.of(Batch.PaymentType.valueOf(paymentType.toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn("evt=get_all_batches_request invalid_payment_type paymentType={}", paymentType);
        }
      }

      PageResponseDTO<BatchResponseDTO> response =
          batchService.getAllBatches(
              statusEnum,
              Optional.ofNullable(nameContains),
              paymentTypeEnum,
              Optional.ofNullable(coachId),
              page,
              size);
      log.info(
          "evt=get_all_batches_response success totalElements={}",
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=get_all_batches_response error", e);
      throw e;
    }
  }

  @GetMapping("/{batchId}")
  public ResponseEntity<BatchResponseDTO> getBatchById(@PathVariable String batchId) {
    log.info("evt=get_batch_by_id_request received batchId={}", batchId);
    try {
      Optional<BatchResponseDTO> batch = batchService.getBatchById(batchId);
      if (batch.isPresent()) {
        log.info("evt=get_batch_by_id_response success batchId={}", batchId);
        return ResponseEntity.ok(batch.get());
      } else {
        log.info("evt=get_batch_by_id_response not_found batchId={}", batchId);
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      log.error("evt=get_batch_by_id_response error batchId={}", batchId, e);
      throw e;
    }
  }

  @PutMapping("/{batchId}")
  public ResponseEntity<BatchResponseDTO> updateBatch(
      @PathVariable String batchId, @RequestBody BatchRequestDTO batchRequest) {
    log.info(
        "evt=update_batch_request received batchId={} batchName={}",
        batchId,
        batchRequest.getBatchName());
    try {
      BatchResponseDTO response = batchService.updateBatch(batchId, batchRequest);
      log.info("evt=update_batch_response success batchId={}", batchId);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=update_batch_response error batchId={}", batchId, e);
      throw e;
    }
  }

  @DeleteMapping("/{batchId}")
  public ResponseEntity<Void> deleteBatch(@PathVariable String batchId) {
    log.info("evt=delete_batch_request received batchId={}", batchId);
    try {
      batchService.deleteBatch(batchId);
      log.info("evt=delete_batch_response success batchId={}", batchId);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("evt=delete_batch_response error batchId={}", batchId, e);
      throw e;
    }
  }

  @GetMapping("/coach/{coachId}")
  public ResponseEntity<PageResponseDTO<BatchResponseDTO>> getBatchesByCoach(
      @PathVariable String coachId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info(
        "evt=get_batches_by_coach_request received coachId={} page={} size={}",
        coachId,
        page,
        size);
    try {
      PageResponseDTO<BatchResponseDTO> response =
          batchService.getBatchesByCoach(coachId, page, size);
      log.info(
          "evt=get_batches_by_coach_response success coachId={} totalElements={}",
          coachId,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=get_batches_by_coach_response error coachId={}", coachId, e);
      throw e;
    }
  }

  @GetMapping("/status/{status}")
  public ResponseEntity<PageResponseDTO<BatchResponseDTO>> getBatchesByStatus(
      @PathVariable String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info(
        "evt=get_batches_by_status_request received status={} page={} size={}", status, page, size);
    try {
      Batch.BatchStatus statusEnum;
      try {
        statusEnum = Batch.BatchStatus.valueOf(status.toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn("evt=get_batches_by_status_request invalid_status status={}", status);
        return ResponseEntity.badRequest().build();
      }

      PageResponseDTO<BatchResponseDTO> response =
          batchService.getBatchesByStatus(statusEnum, page, size);
      log.info(
          "evt=get_batches_by_status_response success status={} totalElements={}",
          status,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=get_batches_by_status_response error status={}", status, e);
      throw e;
    }
  }
}
