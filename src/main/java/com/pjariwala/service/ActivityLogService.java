package com.pjariwala.service;

import com.pjariwala.dto.ActivityLogDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.enums.ActionResult;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogService {

  /** Log a user action */
  void logAction(
      ActionType actionType, String userId, String userName, UserType userType, String description);

  /** Log a user action with entity details */
  void logAction(
      ActionType actionType,
      String userId,
      String userName,
      UserType userType,
      String description,
      EntityType entityType,
      String entityId,
      String entityName);

  /** Log a user action with full details */
  void logAction(
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
      String sessionId);

  /** Get recent activities for a user */
  List<ActivityLogDTO> getRecentActivitiesByUser(String userId, int limit);

  /** Get recent activities for all users (admin/coach view) */
  PageResponseDTO<ActivityLogDTO> getRecentActivities(int page, int size);

  /** Get activities by action type */
  List<ActivityLogDTO> getActivitiesByActionType(ActionType actionType, int limit);

  /** Get activities for a specific entity */
  List<ActivityLogDTO> getActivitiesByEntity(EntityType entityType, String entityId, int limit);

  /** Get activities within a date range */
  List<ActivityLogDTO> getActivitiesByDateRange(
      LocalDateTime startDate, LocalDateTime endDate, int page, int size);

  /** Helper method to log successful login */
  void logLogin(String userId, String userName, UserType userType);

  /** Helper method to log logout */
  void logLogout(String userId, String userName, UserType userType);

  /** Helper method to log student creation */
  void logStudentCreation(String coachId, String coachName, String studentId, String studentName);

  /** Helper method to log batch creation */
  void logBatchCreation(String coachId, String coachName, String batchId, String batchName);

  /** Helper method to log enrollment */
  void logEnrollment(
      String coachId,
      String coachName,
      String studentId,
      String studentName,
      String batchId,
      String batchName);

  /** Helper method to log payment */
  void logPayment(
      String userId,
      String userName,
      UserType userType,
      String studentId,
      String studentName,
      String batchId,
      String batchName,
      double amount);
}
