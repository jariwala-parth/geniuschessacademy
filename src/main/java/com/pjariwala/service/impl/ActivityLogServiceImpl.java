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

  @Override
  public void logAction(
      ActionType actionType,
      String userId,
      String userName,
      UserType userType,
      String description) {
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
        null);
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
      String entityName) {
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
        null);
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
      String sessionId) {

    try {
      ActivityLog activityLog =
          ActivityLog.builder()
              .logId(generateLogId())
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
          "evt=activity_logged userId={} actionType={} description={}",
          userId,
          actionType,
          description);

    } catch (Exception e) {
      log.error(
          "evt=activity_log_error userId={} actionType={} error={}",
          userId,
          actionType,
          e.getMessage(),
          e);
    }
  }

  @Override
  public List<ActivityLogDTO> getRecentActivitiesByUser(String userId, int limit) {
    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":userId", new AttributeValue().withS(userId));

      DynamoDBQueryExpression<ActivityLog> queryExpression =
          new DynamoDBQueryExpression<ActivityLog>()
              .withIndexName("UserIdIndex")
              .withConsistentRead(false)
              .withKeyConditionExpression("userId = :userId")
              .withExpressionAttributeValues(eav)
              .withScanIndexForward(false) // Sort descending by timestamp
              .withLimit(limit);

      PaginatedQueryList<ActivityLog> results =
          dynamoDBMapper.query(ActivityLog.class, queryExpression);

      return results.stream().map(ActivityLogDTO::fromActivityLog).collect(Collectors.toList());

    } catch (Exception e) {
      log.error("evt=get_user_activities_error userId={} error={}", userId, e.getMessage(), e);
      return List.of();
    }
  }

  @Override
  public PageResponseDTO<ActivityLogDTO> getRecentActivities(int page, int size) {
    try {
      DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();

      PaginatedScanList<ActivityLog> scanResult =
          dynamoDBMapper.scan(ActivityLog.class, scanExpression);

      // Convert to list and sort by timestamp (descending)
      List<ActivityLog> allLogs =
          scanResult.stream()
              .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
              .collect(Collectors.toList());

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

      return new PageResponseDTO<>(content, pageInfo);

    } catch (Exception e) {
      log.error(
          "evt=get_recent_activities_error page={} size={} error={}",
          page,
          size,
          e.getMessage(),
          e);
      return new PageResponseDTO<>(List.of(), new PageResponseDTO.PageInfoDTO());
    }
  }

  @Override
  public List<ActivityLogDTO> getActivitiesByActionType(ActionType actionType, int limit) {
    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":actionType", new AttributeValue().withS(actionType.name()));

      DynamoDBQueryExpression<ActivityLog> queryExpression =
          new DynamoDBQueryExpression<ActivityLog>()
              .withIndexName("ActionTypeIndex")
              .withConsistentRead(false)
              .withKeyConditionExpression("actionType = :actionType")
              .withExpressionAttributeValues(eav)
              .withScanIndexForward(false)
              .withLimit(limit);

      PaginatedQueryList<ActivityLog> results =
          dynamoDBMapper.query(ActivityLog.class, queryExpression);

      return results.stream().map(ActivityLogDTO::fromActivityLog).collect(Collectors.toList());

    } catch (Exception e) {
      log.error(
          "evt=get_activities_by_type_error actionType={} error={}", actionType, e.getMessage(), e);
      return List.of();
    }
  }

  @Override
  public List<ActivityLogDTO> getActivitiesByEntity(
      EntityType entityType, String entityId, int limit) {
    try {
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":entityType", new AttributeValue().withS(entityType.name()));
      eav.put(":entityId", new AttributeValue().withS(entityId));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression("entityType = :entityType AND entityId = :entityId")
              .withExpressionAttributeValues(eav)
              .withLimit(limit);

      PaginatedScanList<ActivityLog> results =
          dynamoDBMapper.scan(ActivityLog.class, scanExpression);

      return results.stream()
          .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
          .map(ActivityLogDTO::fromActivityLog)
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error(
          "evt=get_activities_by_entity_error entityType={} entityId={} error={}",
          entityType,
          entityId,
          e.getMessage(),
          e);
      return List.of();
    }
  }

  @Override
  public List<ActivityLogDTO> getActivitiesByDateRange(
      LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
    // This would require additional GSI or complex filtering
    // For now, return recent activities within the date range using scan
    return getRecentActivities(page, size).getContent().stream()
        .filter(
            log -> log.getTimestamp().isAfter(startDate) && log.getTimestamp().isBefore(endDate))
        .collect(Collectors.toList());
  }

  // Helper methods for common actions

  @Override
  public void logLogin(String userId, String userName, UserType userType) {
    logAction(
        ActionType.LOGIN, userId, userName, userType, String.format("%s logged in", userName));
  }

  @Override
  public void logLogout(String userId, String userName, UserType userType) {
    logAction(
        ActionType.LOGOUT, userId, userName, userType, String.format("%s logged out", userName));
  }

  @Override
  public void logStudentCreation(
      String coachId, String coachName, String studentId, String studentName) {
    logAction(
        ActionType.CREATE_STUDENT,
        coachId,
        coachName,
        UserType.COACH,
        String.format("Created new student: %s", studentName),
        EntityType.USER,
        studentId,
        studentName);
  }

  @Override
  public void logBatchCreation(String coachId, String coachName, String batchId, String batchName) {
    logAction(
        ActionType.CREATE_BATCH,
        coachId,
        coachName,
        UserType.COACH,
        String.format("Created new batch: %s", batchName),
        EntityType.BATCH,
        batchId,
        batchName);
  }

  @Override
  public void logEnrollment(
      String coachId,
      String coachName,
      String studentId,
      String studentName,
      String batchId,
      String batchName) {
    logAction(
        ActionType.ENROLL_STUDENT,
        coachId,
        coachName,
        UserType.COACH,
        String.format("Enrolled %s in %s", studentName, batchName),
        EntityType.ENROLLMENT,
        batchId + ":" + studentId,
        String.format("%s -> %s", studentName, batchName));
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
      double amount) {
    logAction(
        ActionType.PAYMENT_RECEIVED,
        userId,
        userName,
        userType,
        String.format("Payment received: ₹%.2f for %s in %s", amount, studentName, batchName),
        EntityType.PAYMENT,
        batchId + ":" + studentId,
        String.format("₹%.2f", amount));
  }

  private String generateLogId() {
    return "LOG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
