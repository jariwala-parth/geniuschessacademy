package com.pjariwala.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAttendanceRequest {
  private String sessionId;
  private List<AttendanceMarkRequest> attendances;
}
