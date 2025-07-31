package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.ActivityLogDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.enums.ActionResult;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
import com.pjariwala.model.ActivityLog;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.SuperAdminAuthorizationService;
import com.pjariwala.util.ValidationUtil;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ActivityLogServiceImpl implements ActivityLogService {

  @Autowired private DynamoDBMapper dynamoDBMapper;
  @Autowired private ValidationUtil validationUtil;
  @Autowired private SuperAdminAuthorizationService superAdminAuthService;

  @Override
  public void logAction(
      ActionType actionType,
      String userId,
      String userName,
      UserType userType,
      String description,
      String organizationId) {
    logAction(
        actionType,
        userId,
        userName,
        userType,
        description,
        null,
        null,
        null,
        null,
        ActionResult.SUCCESS,
        null,
        null,
        null,
        null,
        organizationId);
  }

  @Override
  public void logAction(
      ActionType actionType,
      String userId,
      String userName,
      UserType userType,
      String description,
      EntityType entityType,
      String entityId,
      String entityName,
      String organizationId) {
    logAction(
        actionType,
        userId,
        userName,
        userType,
        description,
        entityType,
        entityId,
        entityName,
        null,
        ActionResult.SUCCESS,
        null,
        null,
        null,
        null,
        organizationId);
  }

  @Override
  public void logAction(
      ActionType actionType,
      String userId,
      String userName,
      UserType userType,
      String description,
      EntityType entityType,
      String entityId,
      String entityName,
      String metadata,
      ActionResult result,
      String errorMessage,
      String ipAddress,
      String userAgent,
      String sessionId,
      String organizationId) {

    try {
      ActivityLog activityLog =
          ActivityLog.builder()
              .logId(generateLogId())
              .organizationId(organizationId)
              .timestamp(LocalDateTime.now())
              .userId(userId)
              .actionType(actionType)
              .userType(userType)
              .userName(userName)
              .description(description)
              .entityType(entityType)
              .entityId(entityId)
              .entityName(entityName)
              .metadata(metadata)
              .result(result != null ? result : ActionResult.SUCCESS)
              .errorMessage(errorMessage)
              .ipAddress(ipAddress)
              .userAgent(userAgent)
              .sessionId(sessionId)
              .build();

      dynamoDBMapper.save(activityLog);
      log.info(
          "evt=activity_logged userId={} actionType={} description={} organizationId={}",
          userId,
          actionType,
          description,
          organizationId);

    } catch (Exception e) {
      log.error(
          "evt=activity_log_error userId={} actionType={} organizationId={} error={}",
          userId,
          actionType,
          organizationId,
          e.getMessage(),
          e);
    }
  }

  @Override
  public List<ActivityLogDTO> getRecentActivitiesByUser(
      String userId, int limit, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_user_activities_start userId={} requestingUserId={} organizationId={}",
        userId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Validate user access - users can only see their own activities, coaches can see any user's
    // activities
    validationUtil.validateUserAccess(requestingUserId, userId, organizationId);
    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":organizationId", new AttributeValue().withS(organizationId));
      eav.put(":userId", new AttributeValue().withS(userId));

      DynamoDBQueryExpression<ActivityLog> queryExpression =
          new DynamoDBQueryExpression<ActivityLog>()
              .withIndexName("userId-index")
              .withKeyConditionExpression("organizationId = :organizationId AND userId = :userId")
              .withExpressionAttributeValues(eav)
              .withScanIndexForward(false)
              .withLimit(limit)
              .withConsistentRead(false);

      PaginatedQueryList<ActivityLog> results =
          dynamoDBMapper.query(ActivityLog.class, queryExpression);

      List<ActivityLogDTO> activities =
          results.stream().map(ActivityLogDTO::fromActivityLog).collect(Collectors.toList());
      log.info(
          "evt=get_user_activities_success userId={} count={} organizationId={}",
          userId,
          activities.size(),
          organizationId);
      return activities;

    } catch (Exception e) {
      log.error(
          "evt=get_user_activities_error userId={} organizationId={} error={}",
          userId,
          organizationId,
          e.getMessage(),
          e);
      throw e; // Re-throw the exception so controller can return proper error status
    }
  }

  @Override
  public PageResponseDTO<ActivityLogDTO> getRecentActivities(
      int page, int size, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_recent_activities_start requestingUserId={} organizationId={} page={} size={}",
        requestingUserId,
        organizationId,
        page,
        size);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Check if super admin controls are enabled and user is global super admin
    if (!superAdminAuthService.canModifyOrganization(requestingUserId, organizationId)) {
      // Only coaches can view all activities
      validationUtil.requireCoachPermission(requestingUserId, organizationId);
    }
    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":organizationId", new AttributeValue().withS(organizationId));

      DynamoDBQueryExpression<ActivityLog> queryExpression =
          new DynamoDBQueryExpression<ActivityLog>()
              .withKeyConditionExpression("organizationId = :organizationId")
              .withExpressionAttributeValues(eav)
              .withScanIndexForward(false) // Sort descending by timestamp
              .withConsistentRead(false); // GSIs don't support consistent reads

      PaginatedQueryList<ActivityLog> results =
          dynamoDBMapper.query(ActivityLog.class, queryExpression);

      // Convert to list and sort by timestamp (descending)
      List<ActivityLog> allLogs = results.stream().collect(Collectors.toList());

      // Manual pagination
      int totalElements = allLogs.size();
      int totalPages = (int) Math.ceil((double) totalElements / size);
      int startIndex = page * size;
      int endIndex = Math.min(startIndex + size, totalElements);

      List<ActivityLog> pagedLogs =
          startIndex < totalElements ? allLogs.subList(startIndex, endIndex) : List.of();

      List<ActivityLogDTO> content =
          pagedLogs.stream().map(ActivityLogDTO::fromActivityLog).collect(Collectors.toList());

      PageResponseDTO.PageInfoDTO pageInfo = new PageResponseDTO.PageInfoDTO();
      pageInfo.setCurrentPage(page);
      pageInfo.setPageSize(size);
      pageInfo.setTotalPages(totalPages);
      pageInfo.setTotalElements((long) totalElements);

      log.info(
          "evt=get_recent_activities_success requestingUserId={} organizationId={}"
              + " totalElements={}",
          requestingUserId,
          organizationId,
          totalElements);
      return new PageResponseDTO<>(content, pageInfo);

    } catch (Exception e) {
      log.error(
          "evt=get_recent_activities_error requestingUserId={} organizationId={} page={} size={}"
              + " error={}",
          requestingUserId,
          organizationId,
          page,
          size,
          e.getMessage(),
          e);
      return new PageResponseDTO<>(List.of(), new PageResponseDTO.PageInfoDTO());
    }
  }

  @Override
  public List<ActivityLogDTO> getActivitiesByActionType(
      ActionType actionType, int limit, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_activities_by_type_start actionType={} requestingUserId={} organizationId={}",
        actionType,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Check if super admin controls are enabled and user is global super admin
    if (!superAdminAuthService.canModifyOrganization(requestingUserId, organizationId)) {
      // Only coaches can view activities by action type
      validationUtil.requireCoachPermission(requestingUserId, organizationId);
    }

    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":organizationId", new AttributeValue().withS(organizationId));
      eav.put(":actionType", new AttributeValue().withS(actionType.name()));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression("organizationId = :organizationId AND actionType = :actionType")
              .withExpressionAttributeValues(eav)
              .withLimit(limit);

      PaginatedScanList<ActivityLog> results =
          dynamoDBMapper.scan(ActivityLog.class, scanExpression);

      List<ActivityLogDTO> activities =
          results.stream()
              .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
              .map(ActivityLogDTO::fromActivityLog)
              .collect(Collectors.toList());

      log.info(
          "evt=get_activities_by_type_success actionType={} requestingUserId={} organizationId={}"
              + " count={}",
          actionType,
          requestingUserId,
          organizationId,
          activities.size());
      return activities;

    } catch (Exception e) {
      log.error(
          "evt=get_activities_by_type_error actionType={} requestingUserId={} organizationId={}"
              + " error={}",
          actionType,
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw e; // Re-throw the exception so controller can return proper error status
    }
  }

  @Override
  public List<ActivityLogDTO> getActivitiesByEntity(
      EntityType entityType,
      String entityId,
      int limit,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=get_activities_by_entity_start entityType={} entityId={} requestingUserId={}"
            + " organizationId={}",
        entityType,
        entityId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Check if super admin controls are enabled and user is global super admin
    if (!superAdminAuthService.canModifyOrganization(requestingUserId, organizationId)) {
      // Only coaches can view activities by entity
      validationUtil.requireCoachPermission(requestingUserId, organizationId);
    }

    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":organizationId", new AttributeValue().withS(organizationId));
      eav.put(":entityType", new AttributeValue().withS(entityType.name()));
      eav.put(":entityId", new AttributeValue().withS(entityId));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression(
                  "organizationId = :organizationId AND entityType = :entityType AND entityId ="
                      + " :entityId")
              .withExpressionAttributeValues(eav)
              .withLimit(limit);

      PaginatedScanList<ActivityLog> results =
          dynamoDBMapper.scan(ActivityLog.class, scanExpression);

      List<ActivityLogDTO> activities =
          results.stream()
              .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
              .map(ActivityLogDTO::fromActivityLog)
              .collect(Collectors.toList());

      log.info(
          "evt=get_activities_by_entity_success entityType={} entityId={} requestingUserId={}"
              + " organizationId={} count={}",
          entityType,
          entityId,
          requestingUserId,
          organizationId,
          activities.size());
      return activities;

    } catch (Exception e) {
      log.error(
          "evt=get_activities_by_entity_error entityType={} entityId={} requestingUserId={}"
              + " organizationId={} error={}",
          entityType,
          entityId,
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw e; // Re-throw the exception so controller can return proper error status
    }
  }

  @Override
  public List<ActivityLogDTO> getActivitiesByDateRange(
      LocalDateTime startDate,
      LocalDateTime endDate,
      int page,
      int size,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=get_activities_by_date_range_start requestingUserId={} organizationId={} startDate={}"
            + " endDate={}",
        requestingUserId,
        organizationId,
        startDate,
        endDate);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    // Check if super admin controls are enabled and user is global super admin
    if (!superAdminAuthService.canModifyOrganization(requestingUserId, organizationId)) {
      // Only coaches can view activities by date range
      validationUtil.requireCoachPermission(requestingUserId, organizationId);
    }

    // This would require additional GSI or complex filtering
    // For now, return recent activities within the date range using scan
    return getRecentActivities(page, size, requestingUserId, organizationId).getContent().stream()
        .filter(
            log -> log.getTimestamp().isAfter(startDate) && log.getTimestamp().isBefore(endDate))
        .collect(Collectors.toList());
  }

  // Helper methods for common actions

  @Override
  public void logLogin(String userId, String userName, UserType userType, String organizationId) {
    logAction(
        ActionType.LOGIN,
        userId,
        userName,
        userType,
        String.format("%s logged in", userName),
        organizationId);
  }

  @Override
  public void logLogout(String userId, String userName, UserType userType, String organizationId) {
    logAction(
        ActionType.LOGOUT,
        userId,
        userName,
        userType,
        String.format("%s logged out", userName),
        organizationId);
  }

  @Override
  public void logStudentCreation(
      String coachId,
      String coachName,
      String studentId,
      String studentName,
      String organizationId) {
    logAction(
        ActionType.CREATE_STUDENT,
        coachId,
        coachName,
        UserType.COACH,
        String.format("Created new student: %s", studentName),
        EntityType.USER,
        studentId,
        studentName,
        organizationId);
  }

  @Override
  public void logBatchCreation(
      String coachId, String coachName, String batchId, String batchName, String organizationId) {
    logAction(
        ActionType.CREATE_BATCH,
        coachId,
        coachName,
        UserType.COACH,
        String.format("Created new batch: %s", batchName),
        EntityType.BATCH,
        batchId,
        batchName,
        organizationId);
  }

  @Override
  public void logEnrollment(
      String coachId,
      String coachName,
      String studentId,
      String studentName,
      String batchId,
      String batchName,
      String organizationId) {
    logAction(
        ActionType.ENROLL_STUDENT,
        coachId,
        coachName,
        UserType.COACH,
        String.format("Enrolled %s in %s", studentName, batchName),
        EntityType.ENROLLMENT,
        batchId + ":" + studentId,
        String.format("%s -> %s", studentName, batchName),
        organizationId);
  }

  @Override
  public void logPayment(
      String userId,
      String userName,
      UserType userType,
      String studentId,
      String studentName,
      String batchId,
      String batchName,
      double amount,
      String organizationId) {
    logAction(
        ActionType.PAYMENT_RECEIVED,
        userId,
        userName,
        userType,
        String.format("Payment received: ₹%.2f for %s in %s", amount, studentName, batchName),
        EntityType.PAYMENT,
        batchId + ":" + studentId,
        String.format("₹%.2f", amount),
        organizationId);
  }

  private String generateLogId() {
    return "LOG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
