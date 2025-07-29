package com.pjariwala.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponseDTO {

  private String organizationId;
  private String organizationName;
  private String description;
  private String ownerId;
  private String contactEmail;
  private String contactPhone;
  private String address;
  private String timezone;
  private Boolean isActive;
  private String subscriptionPlan;
  private Integer maxUsers;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
