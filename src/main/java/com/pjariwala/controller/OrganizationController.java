package com.pjariwala.controller;

import com.pjariwala.dto.OrganizationRequestDTO;
import com.pjariwala.dto.OrganizationResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/organizations")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(
    name = "Organization Management",
    description = "APIs for managing chess academies/organizations")
public class OrganizationController {

  @Autowired private OrganizationService organizationService;

  @PostMapping
  @Operation(
      summary = "Create a new organization",
      description =
          "Create a new chess academy/organization. Only SUPER_ADMIN users can create"
              + " organizations.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<OrganizationResponseDTO> createOrganization(
      @Valid @RequestBody OrganizationRequestDTO organizationRequest,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=create_organization_request organizationName={} ownerId={} requestingUserId={}",
        organizationRequest.getOrganizationName(),
        organizationRequest.getOwnerId(),
        userId);

    OrganizationResponseDTO response =
        organizationService.createOrganization(organizationRequest, userId);
    log.info("evt=create_organization_success organizationId={}", response.getOrganizationId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @Operation(
      summary = "Get all organizations",
      description =
          "Retrieve all organizations. Only SUPER_ADMIN users can view all organizations.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<OrganizationResponseDTO>> getAllOrganizations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=get_all_organizations_request page={} size={} requestingUserId={}",
        page,
        size,
        userId);

    PageResponseDTO<OrganizationResponseDTO> response =
        organizationService.getAllOrganizations(page, size, userId);
    log.info(
        "evt=get_all_organizations_success total={}", response.getPageInfo().getTotalElements());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{organizationId}")
  @Operation(
      summary = "Get organization by ID",
      description =
          "Retrieve a specific organization by its ID. Only organization owner or SUPER_ADMIN can"
              + " view.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<OrganizationResponseDTO> getOrganizationById(
      @PathVariable String organizationId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=get_organization_by_id_request organizationId={} requestingUserId={}",
        organizationId,
        userId);

    return organizationService
        .getOrganizationById(organizationId, userId)
        .map(
            organization -> {
              log.info("evt=get_organization_by_id_success organizationId={}", organizationId);
              return ResponseEntity.ok(organization);
            })
        .orElseGet(
            () -> {
              log.warn("evt=get_organization_by_id_not_found organizationId={}", organizationId);
              return ResponseEntity.notFound().build();
            });
  }

  @PutMapping("/{organizationId}")
  @Operation(
      summary = "Update organization",
      description =
          "Update an existing organization. Only organization owner or SUPER_ADMIN can update.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<OrganizationResponseDTO> updateOrganization(
      @PathVariable String organizationId,
      @Valid @RequestBody OrganizationRequestDTO organizationRequest,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=update_organization_request organizationId={} requestingUserId={}",
        organizationId,
        userId);

    return organizationService
        .updateOrganization(organizationId, organizationRequest, userId)
        .map(
            organization -> {
              log.info("evt=update_organization_success organizationId={}", organizationId);
              return ResponseEntity.ok(organization);
            })
        .orElseGet(
            () -> {
              log.warn("evt=update_organization_not_found organizationId={}", organizationId);
              return ResponseEntity.notFound().build();
            });
  }

  @DeleteMapping("/{organizationId}")
  @Operation(
      summary = "Delete organization",
      description =
          "Delete an existing organization. Only organization owner or SUPER_ADMIN can delete.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> deleteOrganization(
      @PathVariable String organizationId,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=delete_organization_request organizationId={} requestingUserId={}",
        organizationId,
        userId);

    boolean deleted = organizationService.deleteOrganization(organizationId, userId);
    if (deleted) {
      log.info("evt=delete_organization_success organizationId={}", organizationId);
      return ResponseEntity.ok().build();
    } else {
      log.warn("evt=delete_organization_not_found organizationId={}", organizationId);
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping("/{organizationId}/coaches")
  @Operation(
      summary = "Add coach to organization",
      description =
          "Add a new coach to an organization. Only SUPER_ADMIN can add coaches to organizations.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<UserInfo> addCoachToOrganization(
      @PathVariable String organizationId,
      @Valid @RequestBody SignupRequest coachRequest,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=add_coach_to_organization_request organizationId={} coachEmail={} requestingUserId={}",
        organizationId,
        coachRequest.getEmail(),
        userId);

    UserInfo coachUser =
        organizationService.addCoachToOrganization(organizationId, coachRequest, userId);
    log.info(
        "evt=add_coach_to_organization_success organizationId={} coachUserId={}",
        organizationId,
        coachUser.getUserId());
    return ResponseEntity.status(HttpStatus.CREATED).body(coachUser);
  }

  @GetMapping("/owner/{ownerId}")
  @Operation(
      summary = "Get organizations by owner",
      description =
          "Retrieve all organizations owned by a specific user. Users can only view their own"
              + " organizations or SUPER_ADMIN can view any.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<OrganizationResponseDTO>> getOrganizationsByOwner(
      @PathVariable String ownerId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
    log.info(
        "evt=get_organizations_by_owner_request ownerId={} page={} size={} requestingUserId={}",
        ownerId,
        page,
        size,
        userId);

    PageResponseDTO<OrganizationResponseDTO> response =
        organizationService.getOrganizationsByOwner(ownerId, page, size, userId);
    log.info(
        "evt=get_organizations_by_owner_success ownerId={} total={}",
        ownerId,
        response.getPageInfo().getTotalElements());
    return ResponseEntity.ok(response);
  }
}
