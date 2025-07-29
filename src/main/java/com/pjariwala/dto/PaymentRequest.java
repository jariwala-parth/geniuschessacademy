package com.pjariwala.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
  private String invoiceId;
  private Double amount;
  private String paymentMethod;
  private String notes;
}
