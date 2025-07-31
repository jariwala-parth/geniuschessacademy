package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.constants.SystemConstants;
import com.pjariwala.dto.AttendanceDTO;
import com.pjariwala.dto.BatchResponseDTO;
import com.pjariwala.dto.InvoiceDTO;
import com.pjariwala.dto.InvoiceGenerateRequest;
import com.pjariwala.dto.InvoiceItemDTO;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.PaymentRecordRequest;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.InvoiceStatus;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.AuthException;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Batch;
import com.pjariwala.model.Invoice;
import com.pjariwala.model.Invoice.InvoiceItem;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.AttendanceService;
import com.pjariwala.service.BatchService;
import com.pjariwala.service.InvoiceService;
import com.pjariwala.service.SuperAdminAuthorizationService;
import com.pjariwala.service.UserService;
import com.pjariwala.util.ValidationUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

  @Autowired private DynamoDBMapper dynamoDBMapper;
  @Autowired private AttendanceService attendanceService;
  @Autowired private BatchService batchService;
  @Autowired private ActivityLogService activityLogService;
  @Autowired private UserService userService;
  @Autowired private ValidationUtil validationUtil;
  @Autowired private SuperAdminAuthorizationService superAdminAuthService;

  @Override
  public InvoiceDTO generateInvoice(
      InvoiceGenerateRequest request, String coachId, String organizationId) {
    log.info(
        "evt=generate_invoice studentId={} batchId={} coachId={} organizationId={}",
        request.getStudentId(),
        request.getBatchId(),
        coachId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(coachId, organizationId);

    // Check if super admin controls are enabled and user is global super admin
    if (!superAdminAuthService.canModifyOrganization(coachId, organizationId)) {
      // Validate that the requesting user is a coach
      validationUtil.requireCoachPermission(coachId, organizationId);
    }

    // Get batch details
    Optional<BatchResponseDTO> batchResponse =
        batchService.getBatchById(request.getBatchId(), coachId, organizationId);
    if (batchResponse.isEmpty()) {
      throw UserException.userNotFound("Batch not found: " + request.getBatchId());
    }
    BatchResponseDTO batchResponseDTO = batchResponse.get();

    // Convert BatchResponseDTO to Batch model for internal use
    Batch batch = convertToBatchModel(batchResponseDTO);

    // Calculate fees based on attendance and batch type
    Double calculatedAmount =
        calculateFees(
            request.getStudentId(),
            request.getBatchId(),
            request.getBillingPeriodStart().toString(),
            request.getBillingPeriodEnd().toString(),
            organizationId);

    // Create invoice items based on attendance
    List<InvoiceItem> items =
        createInvoiceItems(
            request.getStudentId(),
            request.getBatchId(),
            request.getBillingPeriodStart(),
            request.getBillingPeriodEnd(),
            batch,
            organizationId);

    // Create invoice
    Invoice invoice =
        Invoice.builder()
            .organizationId(organizationId)
            .studentId(request.getStudentId())
            .invoiceId(UUID.randomUUID().toString())
            .batchId(request.getBatchId())
            .billingPeriodStart(request.getBillingPeriodStart())
            .billingPeriodEnd(request.getBillingPeriodEnd())
            .calculatedAmount(calculatedAmount)
            .amountPaid(0.0)
            .status(InvoiceStatus.PENDING)
            .dueDate(
                request
                    .getBillingPeriodEnd()
                    .plusDays(30)) // Due date is 30 days after billing period end
            .items(items)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    dynamoDBMapper.save(invoice);

    // Log the activity
    try {
      User coach = getUser(coachId, organizationId);
      activityLogService.logPayment(
          coach.getUserId(),
          coach.getName(),
          UserType.COACH,
          request.getStudentId(),
          "Student", // We'll need to get the actual student name
          request.getBatchId(),
          batchResponseDTO.getBatchName(),
          calculatedAmount,
          organizationId);
    } catch (Exception e) {
      log.warn(
          "Failed to log invoice generation activity for invoice: {} organizationId={}",
          invoice.getInvoiceId(),
          organizationId,
          e);
    }

    log.info(
        "evt=invoice_generated invoiceId={} organizationId={}",
        invoice.getInvoiceId(),
        organizationId);

    return convertToDTO(invoice);
  }

  @Override
  public InvoiceDTO recordPayment(
      String invoiceId, PaymentRecordRequest request, String coachId, String organizationId) {
    log.info(
        "evt=record_payment invoiceId={} coachId={} organizationId={}",
        invoiceId,
        coachId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(coachId, organizationId);

    // Check if super admin controls are enabled and user is global super admin
    if (!superAdminAuthService.canModifyOrganization(coachId, organizationId)) {
      // Validate that the requesting user is a coach
      validationUtil.requireCoachPermission(coachId, organizationId);
    }

    Invoice invoice = getInvoiceByIdInternal(invoiceId, organizationId);
    if (invoice == null) {
      throw UserException.userNotFound("Invoice not found: " + invoiceId);
    }

    // Validate organization access for the invoice
    if (!organizationId.equals(invoice.getOrganizationId())) {
      throw new AuthException("ACCESS_DENIED", "Invoice does not belong to this organization", 403);
    }

    // Update payment amount
    double newAmountPaid = invoice.getAmountPaid() + request.getAmountPaid();
    invoice.setAmountPaid(newAmountPaid);

    // Update status based on payment
    if (newAmountPaid >= invoice.getCalculatedAmount()) {
      invoice.setStatus(InvoiceStatus.PAID);
    } else if (newAmountPaid > 0) {
      invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
    }

    invoice.setUpdatedAt(LocalDateTime.now());

    dynamoDBMapper.save(invoice);

    // Log the activity
    try {
      User coach = getUser(coachId, organizationId);
      activityLogService.logPayment(
          coach.getUserId(),
          coach.getName(),
          UserType.COACH,
          invoice.getStudentId(),
          "Student", // We'll need to get the actual student name
          invoice.getBatchId(),
          "Batch", // We'll need to get the actual batch name
          request.getAmountPaid(),
          organizationId);
    } catch (Exception e) {
      log.warn(
          "Failed to log payment activity for invoice: {} organizationId={}",
          invoiceId,
          organizationId,
          e);
    }

    log.info(
        "evt=payment_recorded invoiceId={} amount={} organizationId={}",
        invoiceId,
        request.getAmountPaid(),
        organizationId);

    return convertToDTO(invoice);
  }

  @Override
  public InvoiceDTO getInvoiceById(
      String invoiceId, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_invoice_by_id invoiceId={} requestingUserId={} organizationId={}",
        invoiceId,
        requestingUserId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

    Invoice invoice = getInvoiceByIdInternal(invoiceId, organizationId);

    if (invoice == null) {
      return null;
    }

    // Validate user access - students can only see their own invoices, coaches can see any
    validationUtil.validateUserAccess(requestingUserId, invoice.getStudentId(), organizationId);

    return convertToDTO(invoice);
  }

  @Override
  public PageResponseDTO<InvoiceDTO> getInvoicesByStudent(
      String studentId, int page, int size, String requestingUserId, String organizationId) {
    log.info(
        "evt=get_invoices_by_student studentId={} page={} size={} requestingUserId={}"
            + " organizationId={}",
        studentId,
        page,
        size,
        requestingUserId,
        organizationId);

    // Check if super admin can access this organization
    if (!superAdminAuthService.canAccessOrganization(requestingUserId, organizationId)) {
      // Validate organization access
      validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

      // Validate user access - students can only see their own invoices, coaches can see any
      validationUtil.validateUserAccess(requestingUserId, studentId, organizationId);
    }

    // Create a query expression to find all invoices for the student
    Map<String, AttributeValue> eav = new HashMap<>();
    eav.put(":studentId", new AttributeValue().withS(studentId));
    eav.put(":organizationId", new AttributeValue().withS(organizationId));

    DynamoDBQueryExpression<Invoice> queryExpression =
        new DynamoDBQueryExpression<Invoice>()
            .withKeyConditionExpression("studentId = :studentId")
            .withFilterExpression("organizationId = :organizationId")
            .withExpressionAttributeValues(eav);

    List<Invoice> invoices = dynamoDBMapper.query(Invoice.class, queryExpression);

    // Apply pagination
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, invoices.size());
    List<Invoice> paginatedInvoices = invoices.subList(startIndex, endIndex);

    List<InvoiceDTO> invoiceDTOs =
        paginatedInvoices.stream().map(this::convertToDTO).collect(Collectors.toList());

    PageResponseDTO.PageInfoDTO pageInfo = new PageResponseDTO.PageInfoDTO();
    pageInfo.setCurrentPage(page);
    pageInfo.setPageSize(size);
    pageInfo.setTotalElements((long) invoices.size());
    pageInfo.setTotalPages((int) Math.ceil((double) invoices.size() / size));

    PageResponseDTO<InvoiceDTO> response = new PageResponseDTO<>();
    response.setContent(invoiceDTOs);
    response.setPageInfo(pageInfo);

    log.info(
        "evt=get_invoices_by_student_success studentId={} organizationId={} count={}",
        studentId,
        organizationId,
        invoices.size());

    return response;
  }

  @Override
  public PageResponseDTO<InvoiceDTO> getAllInvoices(
      String status,
      String dueDateStart,
      String dueDateEnd,
      String batchId,
      String studentId,
      int page,
      int size,
      String requestingUserId,
      String organizationId) {
    log.info(
        "evt=get_all_invoices status={} dueDateStart={} dueDateEnd={} batchId={} studentId={}"
            + " page={} size={} requestingUserId={} organizationId={}",
        status,
        dueDateStart,
        dueDateEnd,
        batchId,
        studentId,
        page,
        size,
        requestingUserId,
        organizationId);

    // Check if super admin can access this organization
    if (!superAdminAuthService.canAccessOrganization(requestingUserId, organizationId)) {
      // Validate organization access
      validationUtil.validateOrganizationAccess(requestingUserId, organizationId);

      // Only coaches can view all invoices
      validationUtil.requireCoachPermission(requestingUserId, organizationId);
    }

    // Create a scan expression with filters
    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    StringBuilder filterExpression = new StringBuilder("organizationId = :organizationId");
    expressionAttributeValues.put(":organizationId", new AttributeValue().withS(organizationId));

    if (status != null && !status.isEmpty()) {
      filterExpression.append(" AND status = :status");
      expressionAttributeValues.put(":status", new AttributeValue().withS(status));
    }

    if (studentId != null && !studentId.isEmpty()) {
      filterExpression.append(" AND studentId = :studentId");
      expressionAttributeValues.put(":studentId", new AttributeValue().withS(studentId));
    }

    if (batchId != null && !batchId.isEmpty()) {
      filterExpression.append(" AND batchId = :batchId");
      expressionAttributeValues.put(":batchId", new AttributeValue().withS(batchId));
    }

    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression(filterExpression.toString())
            .withExpressionAttributeValues(expressionAttributeValues);

    List<Invoice> invoices = dynamoDBMapper.scan(Invoice.class, scanExpression);

    // Apply additional date filters if needed
    if (dueDateStart != null || dueDateEnd != null) {
      LocalDate startDate = dueDateStart != null ? LocalDate.parse(dueDateStart) : null;
      LocalDate endDate = dueDateEnd != null ? LocalDate.parse(dueDateEnd) : null;

      invoices =
          invoices.stream()
              .filter(
                  invoice -> {
                    if (startDate != null && invoice.getDueDate().isBefore(startDate)) {
                      return false;
                    }
                    if (endDate != null && invoice.getDueDate().isAfter(endDate)) {
                      return false;
                    }
                    return true;
                  })
              .collect(Collectors.toList());
    }

    // Apply pagination
    int startIndex = page * size;
    int endIndex = Math.min(startIndex + size, invoices.size());
    List<Invoice> paginatedInvoices = invoices.subList(startIndex, endIndex);

    List<InvoiceDTO> invoiceDTOs =
        paginatedInvoices.stream().map(this::convertToDTO).collect(Collectors.toList());

    PageResponseDTO.PageInfoDTO pageInfo = new PageResponseDTO.PageInfoDTO();
    pageInfo.setCurrentPage(page);
    pageInfo.setPageSize(size);
    pageInfo.setTotalElements((long) invoices.size());
    pageInfo.setTotalPages((int) Math.ceil((double) invoices.size() / size));

    PageResponseDTO<InvoiceDTO> response = new PageResponseDTO<>();
    response.setContent(invoiceDTOs);
    response.setPageInfo(pageInfo);

    log.info(
        "evt=get_all_invoices_success organizationId={} totalCount={}",
        organizationId,
        invoices.size());

    return response;
  }

  @Override
  public InvoiceDTO updateInvoice(
      String invoiceId, InvoiceDTO invoiceDTO, String coachId, String organizationId) {
    log.info(
        "evt=update_invoice invoiceId={} coachId={} organizationId={}",
        invoiceId,
        coachId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(coachId, organizationId);

    // Check if super admin controls are enabled and user is global super admin
    if (!superAdminAuthService.canModifyOrganization(coachId, organizationId)) {
      // Validate that the requesting user is a coach
      validationUtil.requireCoachPermission(coachId, organizationId);
    }

    Invoice invoice = getInvoiceByIdInternal(invoiceId, organizationId);
    if (invoice == null) {
      throw UserException.userNotFound("Invoice not found: " + invoiceId);
    }

    // Validate organization access for the invoice
    if (!organizationId.equals(invoice.getOrganizationId())) {
      throw new AuthException("ACCESS_DENIED", "Invoice does not belong to this organization", 403);
    }

    // Update fields
    invoice.setCalculatedAmount(invoiceDTO.getCalculatedAmount());
    invoice.setAmountPaid(invoiceDTO.getAmountPaid());
    invoice.setStatus(invoiceDTO.getStatus());
    invoice.setDueDate(invoiceDTO.getDueDate());
    invoice.setUpdatedAt(LocalDateTime.now());

    dynamoDBMapper.save(invoice);

    // Log the activity
    try {
      User coach = getUser(coachId, organizationId);
      activityLogService.logAction(
          ActionType.SYSTEM_ACTION,
          coach.getUserId(),
          coach.getName(),
          UserType.COACH,
          "Updated invoice " + invoiceId,
          EntityType.INVOICE,
          invoiceId,
          "Invoice",
          organizationId);
    } catch (Exception e) {
      log.warn(
          "Failed to log invoice update activity for invoice: {} organizationId={}",
          invoiceId,
          organizationId,
          e);
    }

    log.info("evt=invoice_updated invoiceId={} organizationId={}", invoiceId, organizationId);

    return convertToDTO(invoice);
  }

  @Override
  public void deleteInvoice(String invoiceId, String coachId, String organizationId) {
    log.info(
        "evt=delete_invoice invoiceId={} coachId={} organizationId={}",
        invoiceId,
        coachId,
        organizationId);

    // Validate organization access
    validationUtil.validateOrganizationAccess(coachId, organizationId);

    // Check if super admin controls are enabled and user is global super admin
    if (!superAdminAuthService.canModifyOrganization(coachId, organizationId)) {
      // Validate that the requesting user is a coach
      validationUtil.requireCoachPermission(coachId, organizationId);
    }

    Invoice invoice = getInvoiceByIdInternal(invoiceId, organizationId);
    if (invoice == null) {
      throw UserException.userNotFound("Invoice not found: " + invoiceId);
    }

    // Validate organization access for the invoice
    if (!organizationId.equals(invoice.getOrganizationId())) {
      throw new AuthException("ACCESS_DENIED", "Invoice does not belong to this organization", 403);
    }

    dynamoDBMapper.delete(invoice);

    // Log the activity
    try {
      User coach = getUser(coachId, organizationId);
      activityLogService.logAction(
          ActionType.SYSTEM_ACTION,
          coach.getUserId(),
          coach.getName(),
          UserType.COACH,
          "Deleted invoice " + invoiceId,
          EntityType.INVOICE,
          invoiceId,
          "Invoice",
          organizationId);
    } catch (Exception e) {
      log.warn(
          "Failed to log invoice deletion activity for invoice: {} organizationId={}",
          invoiceId,
          organizationId,
          e);
    }

    log.info("evt=invoice_deleted invoiceId={} organizationId={}", invoiceId, organizationId);
  }

  @Override
  public Double calculateFees(
      String studentId, String batchId, String startDate, String endDate, String organizationId) {
    log.info(
        "evt=calculate_fees studentId={} batchId={} startDate={} endDate={} organizationId={}",
        studentId,
        batchId,
        startDate,
        endDate,
        organizationId);

    try {
      // Get batch details
      Optional<BatchResponseDTO> batchResponse =
          batchService.getBatchById(
              batchId, SystemConstants.SYSTEM_ORGANIZATION_ID, organizationId);
      if (batchResponse.isEmpty()) {
        throw UserException.userNotFound("Batch not found: " + batchId);
      }

      BatchResponseDTO batch = batchResponse.get();
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);

      // Get attendance for the period
      PageResponseDTO<AttendanceDTO> attendanceResponse =
          attendanceService.getAttendanceByStudent(
              studentId, 0, 1000, SystemConstants.SYSTEM_ORGANIZATION_ID, organizationId);

      List<AttendanceDTO> attendances = attendanceResponse.getContent();

      // Filter attendances by date range
      List<AttendanceDTO> periodAttendances =
          attendances.stream()
              .filter(
                  attendance -> {
                    LocalDate attendanceDate = attendance.getMarkedAt().toLocalDate();
                    return !attendanceDate.isBefore(start) && !attendanceDate.isAfter(end);
                  })
              .collect(Collectors.toList());

      // Calculate fees based on batch payment type
      double totalFees = 0.0;

      if ("FIXED_MONTHLY".equals(batch.getPaymentType())) {
        // Fixed monthly fee
        totalFees = batch.getFixedMonthlyFee() != null ? batch.getFixedMonthlyFee() : 0.0;
      } else if ("PER_SESSION".equals(batch.getPaymentType())) {
        // Per session fee
        long attendedSessions =
            periodAttendances.stream()
                .filter(attendance -> Boolean.TRUE.equals(attendance.getIsPresent()))
                .count();
        totalFees =
            attendedSessions * (batch.getPerSessionFee() != null ? batch.getPerSessionFee() : 0.0);
      }

      log.info(
          "evt=calculate_fees_success studentId={} batchId={} organizationId={} totalFees={}",
          studentId,
          batchId,
          organizationId,
          totalFees);

      return totalFees;
    } catch (Exception e) {
      log.error(
          "evt=calculate_fees_error studentId={} batchId={} organizationId={} error={}",
          studentId,
          batchId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to calculate fees", e);
    }
  }

  // Helper methods

  private Invoice getInvoiceByIdInternal(String invoiceId, String organizationId) {
    try {
      // Use scan to find invoice by invoiceId and organizationId
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":invoiceId", new AttributeValue().withS(invoiceId));
      eav.put(":organizationId", new AttributeValue().withS(organizationId));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression("invoiceId = :invoiceId AND organizationId = :organizationId")
              .withExpressionAttributeValues(eav);

      List<Invoice> results = dynamoDBMapper.scan(Invoice.class, scanExpression);
      return results.isEmpty() ? null : results.get(0);
    } catch (Exception e) {
      log.error(
          "evt=get_invoice_by_id_internal_error invoiceId={} organizationId={} error={}",
          invoiceId,
          organizationId,
          e.getMessage(),
          e);
      return null;
    }
  }

  private List<InvoiceItem> createInvoiceItems(
      String studentId,
      String batchId,
      LocalDate startDate,
      LocalDate endDate,
      Batch batch,
      String organizationId) {

    List<InvoiceItem> items = new ArrayList<>();

    try {
      // Get attendance for the period
      PageResponseDTO<AttendanceDTO> attendanceResponse =
          attendanceService.getAttendanceByStudent(
              studentId, 0, 1000, SystemConstants.SYSTEM_ORGANIZATION_ID, organizationId);

      List<AttendanceDTO> attendances = attendanceResponse.getContent();

      // Filter attendances by date range
      List<AttendanceDTO> periodAttendances =
          attendances.stream()
              .filter(
                  attendance -> {
                    LocalDate attendanceDate = attendance.getMarkedAt().toLocalDate();
                    return !attendanceDate.isBefore(startDate) && !attendanceDate.isAfter(endDate);
                  })
              .collect(Collectors.toList());

      // Create invoice items based on attendance
      for (AttendanceDTO attendance : periodAttendances) {
        if (Boolean.TRUE.equals(attendance.getIsPresent())) {
          InvoiceItem item =
              InvoiceItem.builder()
                  .description("Session on " + attendance.getMarkedAt().toLocalDate())
                  .amount(batch.getPerSessionFee() != null ? batch.getPerSessionFee() : 0.0)
                  .type("SESSION")
                  .build();
          items.add(item);
        }
      }

      // Add fixed monthly fee if applicable
      if ("FIXED_MONTHLY".equals(batch.getPaymentType()) && batch.getFixedMonthlyFee() != null) {
        InvoiceItem item =
            InvoiceItem.builder()
                .description("Monthly fee for " + startDate.getMonth() + " " + startDate.getYear())
                .amount(batch.getFixedMonthlyFee())
                .type("MONTHLY_FEE")
                .build();
        items.add(item);
      }
    } catch (Exception e) {
      log.warn(
          "Failed to create invoice items for student: {} batch: {} organizationId={}",
          studentId,
          batchId,
          organizationId,
          e);
    }

    return items;
  }

  private Batch convertToBatchModel(BatchResponseDTO batchResponseDTO) {
    return Batch.builder()
        .batchId(batchResponseDTO.getBatchId())
        .batchName(batchResponseDTO.getBatchName())
        .paymentType(batchResponseDTO.getPaymentType())
        .fixedMonthlyFee(batchResponseDTO.getFixedMonthlyFee())
        .perSessionFee(batchResponseDTO.getPerSessionFee())
        .build();
  }

  private InvoiceDTO convertToDTO(Invoice invoice) {
    InvoiceDTO dto = new InvoiceDTO();
    dto.setInvoiceId(invoice.getInvoiceId());
    dto.setStudentId(invoice.getStudentId());
    dto.setBatchId(invoice.getBatchId());
    dto.setBillingPeriodStart(invoice.getBillingPeriodStart());
    dto.setBillingPeriodEnd(invoice.getBillingPeriodEnd());
    dto.setCalculatedAmount(invoice.getCalculatedAmount());
    dto.setAmountPaid(invoice.getAmountPaid());
    dto.setStatus(invoice.getStatus());
    dto.setDueDate(invoice.getDueDate());
    dto.setCreatedAt(invoice.getCreatedAt());
    dto.setUpdatedAt(invoice.getUpdatedAt());

    // Convert invoice items
    List<InvoiceItemDTO> itemDTOs =
        invoice.getItems().stream()
            .map(
                item -> {
                  InvoiceItemDTO itemDTO = new InvoiceItemDTO();
                  itemDTO.setDescription(item.getDescription());
                  itemDTO.setAmount(item.getAmount());
                  itemDTO.setType(item.getType());
                  return itemDTO;
                })
            .collect(Collectors.toList());
    dto.setItems(itemDTOs);

    return dto;
  }

  private User getUser(String userId, String organizationId) {
    try {
      User user = userService.getUserById(userId).orElse(null);
      if (user == null) {
        throw UserException.userNotFound("User not found: " + userId);
      }

      if (!organizationId.equals(user.getOrganizationId())) {
        throw UserException.validationError("User does not belong to this organization");
      }

      return user;
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=get_user_error userId={} organizationId={} error={}",
          userId,
          organizationId,
          e.getMessage(),
          e);
      throw UserException.databaseError("Failed to retrieve user", e);
    }
  }
}
