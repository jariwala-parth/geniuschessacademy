package com.pjariwala.controller;

import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.model.User;
import com.pjariwala.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

  @Autowired private UserService userService;

  @GetMapping("/students")
  @Operation(
      summary = "Get students with pagination and search",
      description =
          "Retrieve students with optional search and pagination. Only coaches can access this"
              + " endpoint.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<UserInfo>> getAllStudents(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @RequestParam(value = "search", required = false) String searchTerm,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "100") int size) {

    log.info(
        "Received get-students request from user: {} search: {} page: {} size: {}",
        userId,
        searchTerm,
        page,
        size);

    // Validate that the requesting user is a coach
    if (!"COACH".equals(userType)) {
      log.error("Non-coach user {} attempted to get students", userId);
      return ResponseEntity.status(403).build();
    }

    try {
      PageResponseDTO<UserInfo> result =
          userService.getUsersByTypeWithSearch("STUDENT", searchTerm, page, size);

      log.info(
          "Retrieved {} students successfully (page {}, total: {})",
          result.getContent().size(),
          page,
          result.getPageInfo().getTotalElements());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Failed to retrieve students", e);
      return ResponseEntity.status(500).build();
    }
  }

  @GetMapping("/coaches")
  @Operation(
      summary = "Get coaches with pagination and search",
      description =
          "Retrieve coaches with optional search and pagination. Only coaches can access this"
              + " endpoint.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<UserInfo>> getAllCoaches(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @RequestParam(value = "search", required = false) String searchTerm,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "100") int size) {

    log.info(
        "Received get-coaches request from user: {} search: {} page: {} size: {}",
        userId,
        searchTerm,
        page,
        size);

    // Validate that the requesting user is a coach
    if (!"COACH".equals(userType)) {
      log.error("Non-coach user {} attempted to get coaches", userId);
      return ResponseEntity.status(403).build();
    }

    try {
      PageResponseDTO<UserInfo> result =
          userService.getUsersByTypeWithSearch("COACH", searchTerm, page, size);

      log.info(
          "Retrieved {} coaches successfully (page {}, total: {})",
          result.getContent().size(),
          page,
          result.getPageInfo().getTotalElements());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Failed to retrieve coaches", e);
      return ResponseEntity.status(500).build();
    }
  }

  private UserInfo convertToUserInfo(User user) {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(user.getUserId());
    userInfo.setEmail(user.getEmail());
    userInfo.setName(user.getName());
    userInfo.setUserType(user.getUserType());
    userInfo.setPhoneNumber(user.getPhoneNumber());
    return userInfo;
  }
}
