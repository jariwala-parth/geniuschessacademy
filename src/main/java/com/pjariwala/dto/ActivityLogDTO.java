package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pjariwala.enums.ActionResult;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
import com.pjariwala.model.ActivityLog;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogDTO {
  private String logId;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime timestamp;

  private String userId;
  private ActionType actionType;
  private UserType userType;
  private String userName;
  private String description;
  private EntityType entityType;
  private String entityId;
  private String entityName;
  private String metadata;
  private ActionResult result;
  private String errorMessage;

  public static ActivityLogDTO fromActivityLog(ActivityLog log) {
    return ActivityLogDTO.builder()
        .logId(log.getLogId())
        .timestamp(log.getTimestamp())
        .userId(log.getUserId())
        .actionType(log.getActionType())
        .userType(log.getUserType())
        .userName(log.getUserName())
        .description(log.getDescription())
        .entityType(log.getEntityType())
        .entityId(log.getEntityId())
        .entityName(log.getEntityName())
        .metadata(log.getMetadata())
        .result(log.getResult())
        .errorMessage(log.getErrorMessage())
        .build();
  }
}
