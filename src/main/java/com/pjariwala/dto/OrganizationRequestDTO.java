package com.pjariwala.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRequestDTO {

  @NotBlank(message = "Organization name is required")
  @Size(min = 2, max = 100, message = "Organization name must be between 2 and 100 characters")
  private String organizationName;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;

  @NotBlank(message = "Owner ID is required")
  private String ownerId;

  @Email(message = "Contact email must be a valid email address")
  private String contactEmail;

  @Size(max = 20, message = "Contact phone must not exceed 20 characters")
  private String contactPhone;

  @Size(max = 200, message = "Address must not exceed 200 characters")
  private String address;

  private String timezone;

  private String subscriptionPlan;

  private Integer maxUsers;
}
