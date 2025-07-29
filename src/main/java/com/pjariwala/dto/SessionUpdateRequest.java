package com.pjariwala.dto;

import com.pjariwala.model.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionUpdateRequest {
  private Session.SessionStatus status;
  private String coachId;
  private String notes;
}
