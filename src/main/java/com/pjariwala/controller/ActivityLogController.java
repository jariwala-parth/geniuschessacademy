package com.pjariwala.controller;

import com.pjariwala.dto.ActivityLogDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activity-logs")
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Activity Log Management", description = "APIs for managing activity logs")
public class ActivityLogController {

  @Autowired private ActivityLogService activityLogService;

  @GetMapping("/recent")
  @Operation(
      summary = "Get recent activities",
      description =
          "Retrieve recent activities across the system. Only coaches can access this endpoint.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<PageResponseDTO<ActivityLogDTO>> getRecentActivities(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size) {

    // Only coaches can view all activities
    if (!"COACH".equals(userType)) {
      return ResponseEntity.status(403).build();
    }

    try {
      PageResponseDTO<ActivityLogDTO> activities =
          activityLogService.getRecentActivities(page, size);
      return ResponseEntity.ok(activities);
    } catch (Exception e) {
      log.error("evt=get_recent_activities_error userId={} error={}", userId, e.getMessage(), e);
      return ResponseEntity.status(500).build();
    }
  }

  @GetMapping("/user/{targetUserId}")
  @Operation(
      summary = "Get activities for a specific user",
      description =
          "Retrieve activities for a specific user. Coaches can view any user's activities,"
              + " students can only view their own.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<ActivityLogDTO>> getUserActivities(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @PathVariable String targetUserId,
      @RequestParam(value = "limit", defaultValue = "10") int limit) {

    // Students can only view their own activities
    if ("STUDENT".equals(userType) && !userId.equals(targetUserId)) {
      return ResponseEntity.status(403).build();
    }

    try {
      List<ActivityLogDTO> activities =
          activityLogService.getRecentActivitiesByUser(targetUserId, limit);
      return ResponseEntity.ok(activities);
    } catch (Exception e) {
      log.error(
          "evt=get_user_activities_error userId={} targetUserId={} error={}",
          userId,
          targetUserId,
          e.getMessage(),
          e);
      return ResponseEntity.status(500).build();
    }
  }

  @GetMapping("/my-activities")
  @Operation(
      summary = "Get my activities",
      description = "Retrieve activities for the current user.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<ActivityLogDTO>> getMyActivities(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @RequestParam(value = "limit", defaultValue = "10") int limit) {

    try {
      List<ActivityLogDTO> activities = activityLogService.getRecentActivitiesByUser(userId, limit);
      return ResponseEntity.ok(activities);
    } catch (Exception e) {
      log.error("evt=get_my_activities_error userId={} error={}", userId, e.getMessage(), e);
      return ResponseEntity.status(500).build();
    }
  }

  @GetMapping("/by-action/{actionType}")
  @Operation(
      summary = "Get activities by action type",
      description =
          "Retrieve activities filtered by action type. Only coaches can access this endpoint.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<ActivityLogDTO>> getActivitiesByActionType(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @PathVariable ActionType actionType,
      @RequestParam(value = "limit", defaultValue = "20") int limit) {

    // Only coaches can view activities by type
    if (!"COACH".equals(userType)) {
      return ResponseEntity.status(403).build();
    }

    try {
      List<ActivityLogDTO> activities =
          activityLogService.getActivitiesByActionType(actionType, limit);
      return ResponseEntity.ok(activities);
    } catch (Exception e) {
      log.error(
          "evt=get_activities_by_type_error userId={} actionType={} error={}",
          userId,
          actionType,
          e.getMessage(),
          e);
      return ResponseEntity.status(500).build();
    }
  }

  @GetMapping("/entity/{entityType}/{entityId}")
  @Operation(
      summary = "Get activities for a specific entity",
      description =
          "Retrieve activities for a specific entity (e.g., batch, student). Only coaches can"
              + " access this endpoint.")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<ActivityLogDTO>> getActivitiesByEntity(
      @Parameter(hidden = true) @RequestAttribute("userId") String userId,
      @Parameter(hidden = true) @RequestAttribute("userType") String userType,
      @PathVariable EntityType entityType,
      @PathVariable String entityId,
      @RequestParam(value = "limit", defaultValue = "10") int limit) {

    // Only coaches can view entity activities
    if (!"COACH".equals(userType)) {
      return ResponseEntity.status(403).build();
    }

    try {
      List<ActivityLogDTO> activities =
          activityLogService.getActivitiesByEntity(entityType, entityId, limit);
      return ResponseEntity.ok(activities);
    } catch (Exception e) {
      log.error(
          "evt=get_activities_by_entity_error userId={} entityType={} entityId={} error={}",
          userId,
          entityType,
          entityId,
          e.getMessage(),
          e);
      return ResponseEntity.status(500).build();
    }
  }
}
