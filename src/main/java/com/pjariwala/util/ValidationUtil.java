package com.pjariwala.util;

import com.pjariwala.enums.UserType;
import com.pjariwala.exception.AuthException;
import com.pjariwala.model.User;
import com.pjariwala.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ValidationUtil {

  @Autowired private UserService userService;

  /** Validate that the requesting user has access to the organization */
  public void validateOrganizationAccess(String requestingUserId, String organizationId) {
    try {
      User user = userService.getUserById(requestingUserId).orElse(null);
      if (user == null) {
        log.error(
            "evt=validate_org_access_user_not_found requestingUserId={} organizationId={}",
            requestingUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "User not found", 403);
      }

      if (!organizationId.equals(user.getOrganizationId())) {
        log.error(
            "evt=validate_org_access_denied requestingUserId={} userOrgId={} requestedOrgId={}",
            requestingUserId,
            user.getOrganizationId(),
            organizationId);
        throw new AuthException("ACCESS_DENIED", "You can only access your own organization", 403);
      }

      log.debug(
          "evt=validate_org_access_success requestingUserId={} organizationId={}",
          requestingUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=validate_org_access_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate organization access", 403);
    }
  }

  /** Validate that the requesting user can access the target user's data */
  public void validateUserAccess(
      String requestingUserId, String targetUserId, String organizationId) {
    try {
      User requestingUser = userService.getUserById(requestingUserId).orElse(null);
      if (requestingUser == null) {
        log.error(
            "evt=validate_user_access_requesting_user_not_found requestingUserId={} targetUserId={}"
                + " organizationId={}",
            requestingUserId,
            targetUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "Requesting user not found", 403);
      }

      // Coaches can access any user's data within their organization
      if (UserType.COACH.name().equals(requestingUser.getUserType())) {
        log.debug(
            "evt=validate_user_access_coach_granted requestingUserId={} targetUserId={}"
                + " organizationId={}",
            requestingUserId,
            targetUserId,
            organizationId);
        return;
      }

      // Students can only access their own data
      if (!requestingUserId.equals(targetUserId)) {
        log.error(
            "evt=validate_user_access_student_denied requestingUserId={} targetUserId={}"
                + " organizationId={}",
            requestingUserId,
            targetUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "You can only access your own data", 403);
      }

      log.debug(
          "evt=validate_user_access_student_own_data requestingUserId={} targetUserId={}"
              + " organizationId={}",
          requestingUserId,
          targetUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=validate_user_access_error requestingUserId={} targetUserId={} organizationId={}"
              + " error={}",
          requestingUserId,
          targetUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate user access", 403);
    }
  }

  /** Require that the requesting user is a coach */
  public void requireCoachPermission(String requestingUserId, String organizationId) {
    try {
      User user = userService.getUserById(requestingUserId).orElse(null);
      if (user == null) {
        log.error(
            "evt=require_coach_permission_user_not_found requestingUserId={} organizationId={}",
            requestingUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "User not found", 403);
      }

      if (!UserType.COACH.name().equals(user.getUserType())) {
        log.error(
            "evt=require_coach_permission_denied requestingUserId={} userType={} organizationId={}",
            requestingUserId,
            user.getUserType(),
            organizationId);
        throw new AuthException("ACCESS_DENIED", "Only coaches can perform this action", 403);
      }

      log.debug(
          "evt=require_coach_permission_granted requestingUserId={} organizationId={}",
          requestingUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=require_coach_permission_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate coach permission", 403);
    }
  }

  /** Require that the requesting user is a super admin */
  public void requireSuperAdminPermission(String requestingUserId, String organizationId) {
    try {
      User user = userService.getUserById(requestingUserId).orElse(null);
      if (user == null) {
        log.error(
            "evt=require_super_admin_permission_user_not_found requestingUserId={}"
                + " organizationId={}",
            requestingUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "User not found", 403);
      }

      if (!UserType.SUPER_ADMIN.name().equals(user.getUserType())) {
        log.error(
            "evt=require_super_admin_permission_denied requestingUserId={} userType={}"
                + " organizationId={}",
            requestingUserId,
            user.getUserType(),
            organizationId);
        throw new AuthException("ACCESS_DENIED", "Only super admins can perform this action", 403);
      }

      log.debug(
          "evt=require_super_admin_permission_granted requestingUserId={} organizationId={}",
          requestingUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=require_super_admin_permission_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate super admin permission", 403);
    }
  }
}
