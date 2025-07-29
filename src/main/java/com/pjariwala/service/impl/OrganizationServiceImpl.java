package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.OrganizationRequestDTO;
import com.pjariwala.dto.OrganizationResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.AuthException;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Organization;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.AuthService;
import com.pjariwala.service.OrganizationService;
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
public class OrganizationServiceImpl implements OrganizationService {

  @Autowired private DynamoDBMapper dynamoDBMapper;

  @Autowired private ActivityLogService activityLogService;
  @Autowired private UserService userService;
  @Autowired private AuthService authService;

  @Override
  public OrganizationResponseDTO createOrganization(
      OrganizationRequestDTO request, String requestingUserId) {
    log.info(
        "evt=create_organization_start organizationName={} ownerId={} requestingUserId={}",
        request.getOrganizationName(),
        request.getOwnerId(),
        requestingUserId);

    // Authorization: Only SUPER_ADMIN users can create organizations
    requireSuperAdminPermission(requestingUserId);

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
            .timezone(request.getTimezone() != null ? request.getTimezone() : "Asia/Kolkata")
            .isActive(true)
            .subscriptionPlan(
                request.getSubscriptionPlan() != null ? request.getSubscriptionPlan() : "BASIC")
            .maxUsers(request.getMaxUsers() != null ? request.getMaxUsers() : 100)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    try {
      dynamoDBMapper.save(organization);
      log.info(
          "evt=create_organization_success organizationId={}", organization.getOrganizationId());

      // Log organization creation activity
      try {
        activityLogService.logAction(
            ActionType.SYSTEM_ACTION,
            requestingUserId,
            "Admin",
            UserType.COACH,
            "Created organization: " + organization.getOrganizationName());
      } catch (Exception e) {
        log.warn(
            "Failed to log organization creation activity for organization: {}",
            organization.getOrganizationId(),
            e);
      }

      return convertToResponseDTO(organization);
    } catch (Exception e) {
      log.error(
          "evt=create_organization_error organizationName={}", request.getOrganizationName(), e);
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

      log.info("evt=get_organization_by_id_success organizationId={}", organizationId);
      return Optional.of(convertToResponseDTO(organization));
    } catch (Exception e) {
      log.error("evt=get_organization_by_id_error organizationId={}", organizationId, e);
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
      log.info("evt=update_organization_success organizationId={}", organizationId);

      // Log organization update activity
      try {
        activityLogService.logAction(
            ActionType.SYSTEM_ACTION,
            requestingUserId,
            "Admin",
            UserType.COACH,
            "Updated organization: " + existingOrganization.getOrganizationName());
      } catch (Exception e) {
        log.warn(
            "Failed to log organization update activity for organization: {}", organizationId, e);
      }

      return Optional.of(convertToResponseDTO(existingOrganization));
    } catch (Exception e) {
      log.error("evt=update_organization_error organizationId={}", organizationId, e);
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
      log.info("evt=delete_organization_success organizationId={}", organizationId);

      // Log organization deletion activity
      try {
        activityLogService.logAction(
            ActionType.SYSTEM_ACTION,
            requestingUserId,
            "Admin",
            UserType.COACH,
            "Deleted organization: " + organization.getOrganizationName());
      } catch (Exception e) {
        log.warn(
            "Failed to log organization deletion activity for organization: {}", organizationId, e);
      }

      return true;
    } catch (Exception e) {
      log.error("evt=delete_organization_error organizationId={}", organizationId, e);
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
    requireSuperAdminPermission(requestingUserId);

    try {
      DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
      PaginatedScanList<Organization> scanResult =
          dynamoDBMapper.scan(Organization.class, scanExpression);

      List<Organization> organizations = new ArrayList<>(scanResult);

      // Apply pagination
      PageResponseDTO<OrganizationResponseDTO> paginatedResult =
          paginateResults(organizations, page, size);

      log.info(
          "evt=get_all_organizations_success total={} page={} size={}",
          organizations.size(),
          page,
          size);
      return paginatedResult;
    } catch (Exception e) {
      log.error("evt=get_all_organizations_error", e);
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

    // Authorization: Users can only view their own organizations or SUPER_ADMIN can view any
    if (!ownerId.equals(requestingUserId) && !isSuperAdminUser(requestingUserId)) {
      log.warn(
          "evt=get_organizations_by_owner_unauthorized ownerId={} requestingUserId={}",
          ownerId,
          requestingUserId);
      throw new AuthException(
          "FORBIDDEN", "You don't have permission to view these organizations", 403);
    }

    try {
      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      expressionAttributeValues.put(":ownerId", new AttributeValue().withS(ownerId));

      DynamoDBQueryExpression<Organization> queryExpression =
          new DynamoDBQueryExpression<Organization>()
              .withIndexName("ownerId-index")
              .withConsistentRead(false)
              .withKeyConditionExpression("ownerId = :ownerId")
              .withExpressionAttributeValues(expressionAttributeValues);

      List<Organization> organizations = dynamoDBMapper.query(Organization.class, queryExpression);

      // Apply pagination
      PageResponseDTO<OrganizationResponseDTO> paginatedResult =
          paginateResults(organizations, page, size);

      log.info(
          "evt=get_organizations_by_owner_success ownerId={} total={} page={} size={}",
          ownerId,
          organizations.size(),
          page,
          size);
      return paginatedResult;
    } catch (Exception e) {
      log.error("evt=get_organizations_by_owner_error ownerId={}", ownerId, e);
      throw UserException.databaseError("Failed to retrieve organizations by owner", e);
    }
  }

  @Override
  public boolean organizationExists(String organizationId) {
    try {
      Organization organization = dynamoDBMapper.load(Organization.class, organizationId);
      return organization != null;
    } catch (Exception e) {
      log.error("evt=organization_exists_error organizationId={}", organizationId, e);
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
    requireSuperAdminPermission(requestingUserId);

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
      UserInfo coachUser = authService.addStudent(coachRequest, requestingUserId);

      // Update the user's organizationId (this would need to be implemented in UserService)
      // For now, we'll assume the user is created with the correct organization context

      log.info(
          "evt=add_coach_to_organization_success organizationId={} coachUserId={}",
          organizationId,
          coachUser.getUserId());

      // Log the activity
      try {
        activityLogService.logAction(
            ActionType.CREATE_STUDENT,
            requestingUserId,
            "Super Admin",
            UserType.SUPER_ADMIN,
            "Added coach to organization: " + coachRequest.getName());
      } catch (Exception e) {
        log.warn("Failed to log coach addition activity for organization: {}", organizationId, e);
      }

      return coachUser;
    } catch (Exception e) {
      log.error(
          "evt=add_coach_to_organization_error organizationId={} coachEmail={}",
          organizationId,
          coachRequest.getEmail(),
          e);
      throw UserException.databaseError("Failed to add coach to organization", e);
    }
  }

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
    int totalPages = (int) Math.ceil((double) totalElements / size);

    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, totalElements);

    List<OrganizationResponseDTO> pageContent = new ArrayList<>();
    if (startIndex < totalElements) {
      pageContent =
          organizations.subList(startIndex, endIndex).stream()
              .map(this::convertToResponseDTO)
              .collect(Collectors.toList());
    }

    PageResponseDTO.PageInfoDTO pageInfo =
        new PageResponseDTO.PageInfoDTO(page, size, totalPages, totalElements);
    return new PageResponseDTO<>(pageContent, pageInfo);
  }

  private void requireSuperAdminPermission(String userId) {
    User user = getUser(userId);
    if (user == null || !UserType.SUPER_ADMIN.name().equals(user.getUserType())) {
      log.warn("evt=require_super_admin_permission_denied userId={}", userId);
      throw new AuthException("FORBIDDEN", "Super Admin permission required", 403);
    }
  }

  private void requireAdminPermission(String userId) {
    User user = getUser(userId);
    if (user == null || !Boolean.TRUE.equals(user.getIsAdmin())) {
      log.warn("evt=require_admin_permission_denied userId={}", userId);
      throw new AuthException("FORBIDDEN", "Admin permission required", 403);
    }
  }

  private boolean isSuperAdminUser(String userId) {
    try {
      User user = getUser(userId);
      return user != null && UserType.SUPER_ADMIN.name().equals(user.getUserType());
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isAdminUser(String userId) {
    try {
      User user = getUser(userId);
      return user != null && Boolean.TRUE.equals(user.getIsAdmin());
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isOrganizationOwner(Organization organization, String userId) {
    return organization.getOwnerId().equals(userId);
  }

  private User getUser(String userId) {
    try {
      return userService.getUserById(userId).orElse(null);
    } catch (Exception e) {
      log.error("evt=get_user_error userId={}", userId, e);
      return null;
    }
  }
}
