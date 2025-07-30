package com.pjariwala.controller;

import com.pjariwala.dto.UserOrganizationsResponse;
import com.pjariwala.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "User Management", description = "APIs for managing current user")
public class UserMeController {

  @Autowired private UserService userService;

  @GetMapping("/organizations")
  @Operation(
      summary = "Get user organizations and roles",
      description =
          "Retrieve all organizations where the authenticated user has access and their roles in"
              + " each organization")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<UserOrganizationsResponse> getUserOrganizations(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId) {

    log.info("evt=get_user_organizations_request userId={}", userId);

    try {
      UserOrganizationsResponse response = userService.getUserOrganizations(userId);

      log.info(
          "evt=get_user_organizations_success userId={} count={}",
          userId,
          response.getOrganizations().size());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("evt=get_user_organizations_error userId={} error={}", userId, e.getMessage(), e);
      throw e;
    }
  }
}
