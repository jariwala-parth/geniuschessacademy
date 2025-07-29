package com.pjariwala.service;

import com.pjariwala.dto.OrganizationRequestDTO;
import com.pjariwala.dto.OrganizationResponseDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.dto.UserInfo;
import java.util.Optional;

public interface OrganizationService {

  /**
   * Create a new organization
   *
   * @param request Organization creation request
   * @param requestingUserId ID of the user creating the organization
   * @return Created organization response
   */
  OrganizationResponseDTO createOrganization(
      OrganizationRequestDTO request, String requestingUserId);

  /**
   * Get organization by ID
   *
   * @param organizationId Organization ID
   * @param requestingUserId ID of the requesting user
   * @return Organization response if found
   */
  Optional<OrganizationResponseDTO> getOrganizationById(
      String organizationId, String requestingUserId);

  /**
   * Update organization
   *
   * @param organizationId Organization ID
   * @param request Organization update request
   * @param requestingUserId ID of the requesting user
   * @return Updated organization response if found
   */
  Optional<OrganizationResponseDTO> updateOrganization(
      String organizationId, OrganizationRequestDTO request, String requestingUserId);

  /**
   * Delete organization
   *
   * @param organizationId Organization ID
   * @param requestingUserId ID of the requesting user
   * @return true if deleted successfully
   */
  boolean deleteOrganization(String organizationId, String requestingUserId);

  /**
   * Get all organizations (for admin users)
   *
   * @param page Page number
   * @param size Page size
   * @param requestingUserId ID of the requesting user
   * @return Paginated list of organizations
   */
  PageResponseDTO<OrganizationResponseDTO> getAllOrganizations(
      int page, int size, String requestingUserId);

  /**
   * Get organizations by owner
   *
   * @param ownerId Owner user ID
   * @param page Page number
   * @param size Page size
   * @param requestingUserId ID of the requesting user
   * @return Paginated list of organizations
   */
  PageResponseDTO<OrganizationResponseDTO> getOrganizationsByOwner(
      String ownerId, int page, int size, String requestingUserId);

  /**
   * Check if organization exists
   *
   * @param organizationId Organization ID
   * @return true if organization exists
   */
  boolean organizationExists(String organizationId);

  /**
   * Generate a unique organization ID
   *
   * @return Unique organization ID
   */
  String generateOrganizationId();

  /**
   * Add a coach to an organization (SUPER_ADMIN only)
   *
   * @param organizationId Organization ID
   * @param coachRequest Coach creation request
   * @param requestingUserId ID of the requesting SUPER_ADMIN
   * @return Created coach user info
   */
  UserInfo addCoachToOrganization(
      String organizationId, SignupRequest coachRequest, String requestingUserId);
}
