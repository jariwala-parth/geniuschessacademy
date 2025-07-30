package com.pjariwala.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganizationsResponse {
  private List<UserOrganizationInfo> organizations;
}
