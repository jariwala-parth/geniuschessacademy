package com.pjariwala.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganizationInfo {
  private String organizationId;
  private String organizationName;
  private String userRole; // "OWNER", "COACH", "STUDENT"
  private Boolean isActive;
}
