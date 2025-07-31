package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.constants.SystemConstants;
import com.pjariwala.dto.OrganizationRequestDTO;
import com.pjariwala.dto.OrganizationResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.AuthException;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Organization;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.AuthService;
import com.pjariwala.service.OrganizationService;
import com.pjariwala.service.SuperAdminAuthorizationService;
import com.pjariwala.service.UserService;
import com.pjariwala.util.ValidationUtil;
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
public class OrganizationServiceImpl implements OrganizationService {

  @Autowired private DynamoDBMapper dynamoDBMapper;
  @Autowired private ActivityLogService activityLogService;
  @Autowired private UserService userService;
  @Autowired private AuthService authService;
  @Autowired private ValidationUtil validationUtil;
  @Autowired private SuperAdminAuthorizationService superAdminAuthService;

  @Override
  public OrganizationResponseDTO createOrganization(
      OrganizationRequestDTO request, String requestingUserId) {
    log.info(
        "evt=create_organization_start organizationName={} ownerId={} requestingUserId={}",
        request.getOrganizationName(),
        request.getOwnerId(),
        requestingUserId);

    // Authorization: Only SUPER_ADMIN users can create organizations
    validationUtil.requireSuperAdminPermission(
        requestingUserId, SystemConstants.SYSTEM_ORGANIZATION_ID);

    validateOrganizationRequest(request);

    Organization organization =
        Organization.builder()
            .organizationId(generateOrganizationId())
            .organizationName(request.getOrganizationName())
            .description(request.getDescription())
            .ownerId(request.getOwnerId())
            .contactEmail(request.getContactEmail())
            .contactPhone(request.getContactPhone())
            .address(request.getAddress())
            .timezone(
                request.getTimezone() != null
                    ? request.getTimezone()
                    : SystemConstants.DEFAULT_TIMEZONE)
            .isActive(true)
            .subscriptionPlan(
                request.getSubscriptionPlan() != null
                    ? request.getSubscriptionPlan()
                    : SystemConstants.DEFAULT_SUBSCRIPTION_PLAN)
            .maxUsers(
                request.getMaxUsers() != null
                    ? request.getMaxUsers()
                    : SystemConstants.DEFAULT_MAX_USERS)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    try {
      dynamoDBMapper.save(organization);
      log.info(
          "evt=create_organization_success organizationId={} organizationName={}",
          organization.getOrganizationId(),
          organization.getOrganizationName());

      // Log organization creation activity
      try {
        User admin = getUser(requestingUserId);
        String actionDescription = "Created organization: " + organization.getOrganizationName();

        // Add super admin indicator if applicable
        if (superAdminAuthService.isGlobalSuperAdmin(requestingUserId)) {
          actionDescription = "SUPER_ADMIN " + actionDescription;
        }

        activityLogService.logAction(
            ActionType.SYSTEM_ACTION,
            admin.getUserId(),
            admin.getName(),
            UserType.SUPER_ADMIN,
            actionDescription,
            EntityType.SYSTEM,
            organization.getOrganizationId(),
            organization.getOrganizationName(),
            SystemConstants.SYSTEM_ORGANIZATION_ID);
      } catch (Exception e) {
        log.warn(
            "Failed to log organization creation activity for organization: {} organizationName={}",
            organization.getOrganizationId(),
            organization.getOrganizationName(),
            e);
      }

      return convertToResponseDTO(organization);
    } catch (Exception e) {
      log.error(
          "evt=create_organization_error organizationName={} requestingUserId={} error={}",
          request.getOrganizationName(),
          requestingUserId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to create organization", e);
    }
  }

  @Override
  public Optional<OrganizationResponseDTO> getOrganizationById(
      String organizationId, String requestingUserId) {
    log.info(
        "evt=get_organization_by_id organizationId={} requestingUserId={}",
        organizationId,
        requestingUserId);

    try {
      Organization organization = dynamoDBMapper.load(Organization.class, organizationId);

      if (organization == null) {
        log.warn("evt=get_organization_by_id_not_found organizationId={}", organizationId);
        return Optional.empty();
      }

      // Authorization: Only organization owner or SUPER_ADMIN can view organization details
      if (!isOrganizationOwner(organization, requestingUserId)
          && !isSuperAdminUser(requestingUserId)) {
        log.warn(
            "evt=get_organization_by_id_unauthorized organizationId={} requestingUserId={}",
            organizationId,
            requestingUserId);
        throw new AuthException(
            "FORBIDDEN", "You don't have permission to view this organization", 403);
      }

      log.info(
          "evt=get_organization_by_id_success organizationId={} requestingUserId={}",
          organizationId,
          requestingUserId);
      return Optional.of(convertToResponseDTO(organization));
    } catch (Exception e) {
      log.error(
          "evt=get_organization_by_id_error organizationId={} requestingUserId={} error={}",
          organizationId,
          requestingUserId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve organization", e);
    }
  }

  @Override
  public Optional<OrganizationResponseDTO> updateOrganization(
      String organizationId, OrganizationRequestDTO request, String requestingUserId) {
    log.info(
        "evt=update_organization_start organizationId={} requestingUserId={}",
        organizationId,
        requestingUserId);

    try {
      Organization existingOrganization = dynamoDBMapper.load(Organization.class, organizationId);

      if (existingOrganization == null) {
        log.warn("evt=update_organization_not_found organizationId={}", organizationId);
        return Optional.empty();
      }

      // Authorization: Only organization owner or SUPER_ADMIN can update organization
      if (!isOrganizationOwner(existingOrganization, requestingUserId)
          && !isSuperAdminUser(requestingUserId)) {
        log.warn(
            "evt=update_organization_unauthorized organizationId={} requestingUserId={}",
            organizationId,
            requestingUserId);
        throw new AuthException(
            "FORBIDDEN", "You don't have permission to update this organization", 403);
      }

      validateOrganizationRequest(request);

      // Update organization fields
      existingOrganization.setOrganizationName(request.getOrganizationName());
      existingOrganization.setDescription(request.getDescription());
      existingOrganization.setContactEmail(request.getContactEmail());
      existingOrganization.setContactPhone(request.getContactPhone());
      existingOrganization.setAddress(request.getAddress());
      if (request.getTimezone() != null) {
        existingOrganization.setTimezone(request.getTimezone());
      }
      if (request.getSubscriptionPlan() != null) {
        existingOrganization.setSubscriptionPlan(request.getSubscriptionPlan());
      }
      if (request.getMaxUsers() != null) {
        existingOrganization.setMaxUsers(request.getMaxUsers());
      }
      existingOrganization.setUpdatedAt(LocalDateTime.now());

      dynamoDBMapper.save(existingOrganization);
      log.info(
          "evt=update_organization_success organizationId={} requestingUserId={}",
          organizationId,
          requestingUserId);

      // Log organization update activity
      try {
        User user = getUser(requestingUserId);
        activityLogService.logAction(
            ActionType.SYSTEM_ACTION,
            user.getUserId(),
            user.getName(),
            UserType.SUPER_ADMIN,
            "Updated organization: " + existingOrganization.getOrganizationName(),
            EntityType.SYSTEM,
            organizationId,
            existingOrganization.getOrganizationName(),
            SystemConstants.SYSTEM_ORGANIZATION_ID);
      } catch (Exception e) {
        log.warn(
            "Failed to log organization update activity for organization: {} organizationName={}",
            organizationId,
            existingOrganization.getOrganizationName(),
            e);
      }

      return Optional.of(convertToResponseDTO(existingOrganization));
    } catch (Exception e) {
      log.error(
          "evt=update_organization_error organizationId={} requestingUserId={} error={}",
          organizationId,
          requestingUserId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to update organization", e);
    }
  }

  @Override
  public boolean deleteOrganization(String organizationId, String requestingUserId) {
    log.info(
        "evt=delete_organization_start organizationId={} requestingUserId={}",
        organizationId,
        requestingUserId);

    try {
      Organization organization = dynamoDBMapper.load(Organization.class, organizationId);

      if (organization == null) {
        log.warn("evt=delete_organization_not_found organizationId={}", organizationId);
        return false;
      }

      // Authorization: Only organization owner or SUPER_ADMIN can delete organization
      if (!isOrganizationOwner(organization, requestingUserId)
          && !isSuperAdminUser(requestingUserId)) {
        log.warn(
            "evt=delete_organization_unauthorized organizationId={} requestingUserId={}",
            organizationId,
            requestingUserId);
        throw new AuthException(
            "FORBIDDEN", "You don't have permission to delete this organization", 403);
      }

      dynamoDBMapper.delete(organization);
      log.info(
          "evt=delete_organization_success organizationId={} requestingUserId={}",
          organizationId,
          requestingUserId);

      // Log organization deletion activity
      try {
        User user = getUser(requestingUserId);
        activityLogService.logAction(
            ActionType.SYSTEM_ACTION,
            user.getUserId(),
            user.getName(),
            UserType.SUPER_ADMIN,
            "Deleted organization: " + organization.getOrganizationName(),
            EntityType.SYSTEM,
            organizationId,
            organization.getOrganizationName(),
            SystemConstants.SYSTEM_ORGANIZATION_ID);
      } catch (Exception e) {
        log.warn(
            "Failed to log organization deletion activity for organization: {} organizationName={}",
            organizationId,
            organization.getOrganizationName(),
            e);
      }

      return true;
    } catch (Exception e) {
      log.error(
          "evt=delete_organization_error organizationId={} requestingUserId={} error={}",
          organizationId,
          requestingUserId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to delete organization", e);
    }
  }

  @Override
  public PageResponseDTO<OrganizationResponseDTO> getAllOrganizations(
      int page, int size, String requestingUserId) {
    log.info(
        "evt=get_all_organizations_request page={} size={} requestingUserId={}",
        page,
        size,
        requestingUserId);

    // Authorization: Only SUPER_ADMIN users can view all organizations
    validationUtil.requireSuperAdminPermission(
        requestingUserId, SystemConstants.SYSTEM_ORGANIZATION_ID);

    try {
      DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
      PaginatedScanList<Organization> scanResult =
          dynamoDBMapper.scan(Organization.class, scanExpression);

      List<Organization> organizations = new ArrayList<>(scanResult);

      // Apply pagination
      PageResponseDTO<OrganizationResponseDTO> paginatedResult =
          paginateResults(organizations, page, size);

      log.info(
          "evt=get_all_organizations_success total={} page={} size={} requestingUserId={}",
          organizations.size(),
          page,
          size,
          requestingUserId);
      return paginatedResult;
    } catch (Exception e) {
      log.error(
          "evt=get_all_organizations_error requestingUserId={} error={}",
          requestingUserId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve organizations", e);
    }
  }

  @Override
  public PageResponseDTO<OrganizationResponseDTO> getOrganizationsByOwner(
      String ownerId, int page, int size, String requestingUserId) {
    log.info(
        "evt=get_organizations_by_owner ownerId={} page={} size={} requestingUserId={}",
        ownerId,
        page,
        size,
        requestingUserId);

    // Authorization: Only the owner or SUPER_ADMIN can view organizations by owner
    if (!ownerId.equals(requestingUserId) && !isSuperAdminUser(requestingUserId)) {
      log.warn(
          "evt=get_organizations_by_owner_unauthorized ownerId={} requestingUserId={}",
          ownerId,
          requestingUserId);
      throw new AuthException(
          "FORBIDDEN", "You don't have permission to view these organizations", 403);
    }

    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":ownerId", new AttributeValue().withS(ownerId));

      DynamoDBQueryExpression<Organization> queryExpression =
          new DynamoDBQueryExpression<Organization>()
              .withIndexName("ownerId-index")
              .withConsistentRead(false)
              .withKeyConditionExpression("ownerId = :ownerId")
              .withExpressionAttributeValues(eav);

      List<Organization> organizations = dynamoDBMapper.query(Organization.class, queryExpression);

      // Apply pagination
      PageResponseDTO<OrganizationResponseDTO> paginatedResult =
          paginateResults(organizations, page, size);

      log.info(
          "evt=get_organizations_by_owner_success ownerId={} total={} page={} size={}"
              + " requestingUserId={}",
          ownerId,
          organizations.size(),
          page,
          size,
          requestingUserId);
      return paginatedResult;
    } catch (Exception e) {
      log.error(
          "evt=get_organizations_by_owner_error ownerId={} requestingUserId={} error={}",
          ownerId,
          requestingUserId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve organizations by owner", e);
    }
  }

  @Override
  public boolean organizationExists(String organizationId) {
    try {
      Organization organization = dynamoDBMapper.load(Organization.class, organizationId);
      return organization != null;
    } catch (Exception e) {
      log.error(
          "evt=organization_exists_error organizationId={} error={}",
          organizationId,
          e.getMessage(),
          e);
      return false;
    }
  }

  @Override
  public String generateOrganizationId() {
    return "ORG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
  }

  @Override
  public UserInfo addCoachToOrganization(
      String organizationId, SignupRequest coachRequest, String requestingUserId) {
    log.info(
        "evt=add_coach_to_organization_start organizationId={} coachEmail={} requestingUserId={}",
        organizationId,
        coachRequest.getEmail(),
        requestingUserId);

    // Authorization: Only SUPER_ADMIN can add coaches to organizations
    validationUtil.requireSuperAdminPermission(
        requestingUserId, SystemConstants.SYSTEM_ORGANIZATION_ID);

    // Validate organization exists
    if (!organizationExists(organizationId)) {
      log.warn("evt=add_coach_to_organization_org_not_found organizationId={}", organizationId);
      throw UserException.validationError("Organization not found");
    }

    // Validate coach request
    if (coachRequest.getUserType() == null
        || !UserType.COACH.name().equals(coachRequest.getUserType())) {
      log.warn(
          "evt=add_coach_to_organization_invalid_user_type userType={}",
          coachRequest.getUserType());
      throw UserException.validationError("User type must be COACH");
    }

    try {
      // Create coach user with organization context
      coachRequest.setOrganizationId(organizationId);
      UserInfo coachInfo = authService.addStudent(coachRequest, requestingUserId);

      log.info(
          "evt=add_coach_to_organization_success organizationId={} coachEmail={}"
              + " requestingUserId={}",
          organizationId,
          coachRequest.getEmail(),
          requestingUserId);

      // Log coach addition activity
      try {
        User admin = getUser(requestingUserId);
        activityLogService.logAction(
            ActionType.SYSTEM_ACTION,
            admin.getUserId(),
            admin.getName(),
            UserType.SUPER_ADMIN,
            "Added coach to organization: " + coachRequest.getEmail(),
            EntityType.USER,
            coachInfo.getUserId(),
            coachInfo.getName(),
            SystemConstants.SYSTEM_ORGANIZATION_ID);
      } catch (Exception e) {
        log.warn(
            "Failed to log coach addition activity for organization: {} coachEmail={}",
            organizationId,
            coachRequest.getEmail(),
            e);
      }

      return coachInfo;
    } catch (Exception e) {
      log.error(
          "evt=add_coach_to_organization_error organizationId={} coachEmail={} requestingUserId={}"
              + " error={}",
          organizationId,
          coachRequest.getEmail(),
          requestingUserId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to add coach to organization", e);
    }
  }

  // Helper methods

  private void validateOrganizationRequest(OrganizationRequestDTO request) {
    if (request.getOrganizationName() == null || request.getOrganizationName().trim().isEmpty()) {
      throw UserException.validationError("Organization name is required");
    }

    if (request.getOwnerId() == null || request.getOwnerId().trim().isEmpty()) {
      throw UserException.validationError("Owner ID is required");
    }

    // Validate that the owner exists
    Optional<User> owner = userService.getUserById(request.getOwnerId());
    if (owner.isEmpty()) {
      throw UserException.validationError("Owner user does not exist");
    }

    if (request.getContactEmail() != null && !request.getContactEmail().trim().isEmpty()) {
      // Basic email validation
      if (!request.getContactEmail().contains("@")) {
        throw UserException.validationError("Invalid email format");
      }
    }
  }

  private OrganizationResponseDTO convertToResponseDTO(Organization organization) {
    return OrganizationResponseDTO.builder()
        .organizationId(organization.getOrganizationId())
        .organizationName(organization.getOrganizationName())
        .description(organization.getDescription())
        .ownerId(organization.getOwnerId())
        .contactEmail(organization.getContactEmail())
        .contactPhone(organization.getContactPhone())
        .address(organization.getAddress())
        .timezone(organization.getTimezone())
        .isActive(organization.getIsActive())
        .subscriptionPlan(organization.getSubscriptionPlan())
        .maxUsers(organization.getMaxUsers())
        .createdAt(organization.getCreatedAt())
        .updatedAt(organization.getUpdatedAt())
        .build();
  }

  private PageResponseDTO<OrganizationResponseDTO> paginateResults(
      List<Organization> organizations, int page, int size) {
    int totalElements = organizations.size();
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, totalElements);

    List<Organization> paginatedOrganizations =
        startIndex < totalElements
            ? organizations.subList(startIndex, endIndex)
            : new ArrayList<>();

    List<OrganizationResponseDTO> organizationDTOs =
        paginatedOrganizations.stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());

    PageResponseDTO.PageInfoDTO pageInfo = new PageResponseDTO.PageInfoDTO();
    pageInfo.setCurrentPage(page);
    pageInfo.setPageSize(size);
    pageInfo.setTotalElements((long) totalElements);
    pageInfo.setTotalPages((int) Math.ceil((double) totalElements / size));

    PageResponseDTO<OrganizationResponseDTO> response = new PageResponseDTO<>();
    response.setContent(organizationDTOs);
    response.setPageInfo(pageInfo);

    return response;
  }

  private boolean isOrganizationOwner(Organization organization, String userId) {
    return organization.getOwnerId().equals(userId);
  }

  private boolean isSuperAdminUser(String userId) {
    try {
      User user = getUser(userId);
      return UserType.SUPER_ADMIN.name().equals(user.getUserType());
    } catch (Exception e) {
      log.error("evt=is_super_admin_user_error userId={} error={}", userId, e.getMessage(), e);
      return false;
    }
  }

  private User getUser(String userId) {
    try {
      User user = userService.getUserById(userId).orElse(null);
      if (user == null) {
        throw UserException.userNotFound("User not found: " + userId);
      }
      return user;
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error("evt=get_user_error userId={} error={}", userId, e.getMessage(), e);
      throw UserException.databaseError("Failed to retrieve user", e);
    }
  }
}
