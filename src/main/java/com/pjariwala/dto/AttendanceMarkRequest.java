package com.pjariwala.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMarkRequest {
  private String sessionId;
  private String studentId;
  private Boolean isPresent;
  private String markedByCoachId;
  private String notes;
}
