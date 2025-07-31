package com.pjariwala.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganizationsResponse {
  private List<UserOrganizationInfo> organizations;

  @JsonProperty("isSuperAdmin")
  private boolean isSuperAdmin;
}
