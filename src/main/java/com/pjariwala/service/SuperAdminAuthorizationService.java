package com.pjariwala.service;

import com.pjariwala.config.SuperAdminConfig;
import com.pjariwala.constants.SystemConstants;
import com.pjariwala.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SuperAdminAuthorizationService {

  @Autowired private SuperAdminConfig superAdminConfig;

  @Autowired private UserService userService;

  /** Check if a user is a global super admin */
  public boolean isGlobalSuperAdmin(String userId) {
    try {
      User user = userService.getUserById(userId).orElse(null);

      if (user == null) {
        return false;
      }

      // Check if user is SUPER_ADMIN in system organization
      return "SUPER_ADMIN".equals(user.getUserType())
          && SystemConstants.SYSTEM_ORGANIZATION_ID.equals(user.getOrganizationId());
    } catch (Exception e) {
      log.error("Error checking if user is global super admin: userId={}", userId, e);
      return false;
    }
  }

  /** Check if a user can access an organization (bypasses normal access checks) */
  public boolean canAccessOrganization(String userId, String organizationId) {
    // If user is global super admin, they can access any organization
    if (isGlobalSuperAdmin(userId)) {
      log.info(
          "SUPER_ADMIN bypassing organization access check: userId={}, organizationId={}",
          userId,
          organizationId);
      return true;
    }

    // Otherwise, use normal access checks
    return false;
  }

  /** Check if a user can modify an organization (bypasses normal access checks) */
  public boolean canModifyOrganization(String userId, String organizationId) {
    // If super admin controls are enabled and user is global super admin
    if (superAdminConfig.isSuperAdminControlsOrganisations() && isGlobalSuperAdmin(userId)) {
      log.info(
          "SUPER_ADMIN bypassing organization access check: userId={}, organizationId={}",
          userId,
          organizationId);
      return true;
    }

    // Otherwise, use normal access checks
    return false;
  }

  /** Check if a user can view all organizations (super admin always can) */
  public boolean canViewAllOrganizations(String userId) {
    return isGlobalSuperAdmin(userId);
  }

  /** Log super admin action for audit trail */
  public void logSuperAdminAction(String userId, String action, String details) {
    if (isGlobalSuperAdmin(userId)) {
      log.info(
          "SUPER_ADMIN ACTION - userId={}, action={}, details={}, controlsEnabled={}",
          userId,
          action,
          details,
          superAdminConfig.isSuperAdminControlsOrganisations());
    }
  }
}
