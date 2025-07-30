package com.pjariwala.controller;

import com.pjariwala.dto.BatchRequestDTO;
import com.pjariwala.dto.BatchResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.enums.BatchStatus;
import com.pjariwala.enums.PaymentType;
import com.pjariwala.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/batches")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(
    name = "Batch Management",
    description = "APIs for managing chess batches within organizations")
public class BatchController {

  @Autowired private BatchService batchService;

  @PostMapping
  @Operation(
      summary = "Create a new batch",
      description =
          "Create a new chess batch within an organization. Only coaches can create batches.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<BatchResponseDTO> createBatch(
      @PathVariable String organizationId,
      @Valid @RequestBody BatchRequestDTO batchRequest,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info(
        "evt=create_batch_request organizationId={} batchName={} coachId={} requestingUserId={}",
        organizationId,
        batchRequest.getBatchName(),
        batchRequest.getCoachId(),
        userId);

    BatchResponseDTO response = batchService.createBatch(batchRequest, userId, organizationId);
    log.info(
        "evt=create_batch_success organizationId={} batchId={}",
        organizationId,
        response.getBatchId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @Operation(
      summary = "Get all batches",
      description =
          "Retrieve all batches within an organization with optional filtering and pagination")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<BatchResponseDTO>> getAllBatches(
      @PathVariable String organizationId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String nameContains,
      @RequestParam(required = false) String paymentType,
      @RequestParam(required = false) String coachId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info(
        "evt=get_all_batches_request organizationId={} page={} size={} status={} nameContains={}"
            + " paymentType={} coachId={} requestingUserId={}",
        organizationId,
        page,
        size,
        status,
        nameContains,
        paymentType,
        coachId,
        userId);
    try {
      Optional<BatchStatus> statusEnum = Optional.empty();
      if (status != null) {
        try {
          statusEnum = Optional.of(BatchStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn("evt=get_all_batches_request invalid_status status={}", status);
        }
      }

      Optional<PaymentType> paymentTypeEnum = Optional.empty();
      if (paymentType != null) {
        try {
          paymentTypeEnum = Optional.of(PaymentType.valueOf(paymentType.toUpperCase()));
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
              size,
              userId,
              organizationId);

      log.info(
          "evt=get_all_batches_success organizationId={} totalElements={}",
          organizationId,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=get_all_batches_error organizationId={}", organizationId, e);
      throw e;
    }
  }

  @GetMapping("/{batchId}")
  @Operation(
      summary = "Get batch by ID",
      description = "Retrieve a specific batch by its ID within an organization")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<BatchResponseDTO> getBatchById(
      @PathVariable String organizationId,
      @PathVariable String batchId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info(
        "evt=get_batch_by_id_request organizationId={} batchId={} requestingUserId={}",
        organizationId,
        batchId,
        userId);

    Optional<BatchResponseDTO> batch = batchService.getBatchById(batchId, userId, organizationId);
    if (batch.isPresent()) {
      log.info("evt=get_batch_by_id_success organizationId={} batchId={}", organizationId, batchId);
      return ResponseEntity.ok(batch.get());
    } else {
      log.info(
          "evt=get_batch_by_id_not_found organizationId={} batchId={}", organizationId, batchId);
      return ResponseEntity.notFound().build();
    }
  }

  @PutMapping("/{batchId}")
  @Operation(
      summary = "Update batch",
      description =
          "Update an existing batch within an organization. Only coaches can update batches.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<BatchResponseDTO> updateBatch(
      @PathVariable String organizationId,
      @PathVariable String batchId,
      @Valid @RequestBody BatchRequestDTO batchRequest,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info(
        "evt=update_batch_request organizationId={} batchId={} requestingUserId={}",
        organizationId,
        batchId,
        userId);

    Optional<BatchResponseDTO> response =
        batchService.updateBatch(batchId, batchRequest, userId, organizationId);
    if (response.isPresent()) {
      log.info("evt=update_batch_success organizationId={} batchId={}", organizationId, batchId);
      return ResponseEntity.ok(response.get());
    } else {
      log.info("evt=update_batch_not_found organizationId={} batchId={}", organizationId, batchId);
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{batchId}")
  @Operation(
      summary = "Delete batch",
      description =
          "Delete an existing batch within an organization. Only coaches can delete batches.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> deleteBatch(
      @PathVariable String organizationId,
      @PathVariable String batchId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info(
        "evt=delete_batch_request organizationId={} batchId={} requestingUserId={}",
        organizationId,
        batchId,
        userId);

    boolean deleted = batchService.deleteBatch(batchId, userId, organizationId);
    if (deleted) {
      log.info("evt=delete_batch_success organizationId={} batchId={}", organizationId, batchId);
      return ResponseEntity.noContent().build();
    } else {
      log.info("evt=delete_batch_not_found organizationId={} batchId={}", organizationId, batchId);
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/coach/{coachId}")
  @Operation(
      summary = "Get batches by coach",
      description = "Retrieve all batches for a specific coach within an organization")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<BatchResponseDTO>> getBatchesByCoach(
      @PathVariable String organizationId,
      @PathVariable String coachId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info(
        "evt=get_batches_by_coach_request organizationId={} coachId={} page={} size={}"
            + " requestingUserId={}",
        organizationId,
        coachId,
        page,
        size,
        userId);
    try {
      PageResponseDTO<BatchResponseDTO> response =
          batchService.getBatchesByCoach(coachId, page, size, userId, organizationId);
      log.info(
          "evt=get_batches_by_coach_success organizationId={} coachId={} totalElements={}",
          organizationId,
          coachId,
          response.getPageInfo().getTotalElements());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error(
          "evt=get_batches_by_coach_error organizationId={} coachId={}",
          organizationId,
          coachId,
          e);
      throw e;
    }
  }
}
