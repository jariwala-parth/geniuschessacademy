package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
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
import com.pjariwala.model.Batch;
import com.pjariwala.model.Invoice;
import com.pjariwala.model.Invoice.InvoiceItem;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.AttendanceService;
import com.pjariwala.service.BatchService;
import com.pjariwala.service.InvoiceService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

  @Autowired private DynamoDBMapper dynamoDBMapper;

  @Autowired private AttendanceService attendanceService;

  @Autowired private BatchService batchService;

  @Autowired private ActivityLogService activityLogService;

  @Override
  public InvoiceDTO generateInvoice(
      InvoiceGenerateRequest request, String coachId, String userType) {
    log.info(
        "evt=generate_invoice studentId={} batchId={} coachId={} userType={}",
        request.getStudentId(),
        request.getBatchId(),
        coachId,
        userType);

    // Validate that the requesting user is a coach
    if (!"COACH".equals(userType)) {
      log.error("evt=unauthorized_invoice_generation coachId={} userType={}", coachId, userType);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can generate invoices");
    }

    // Get batch details
    Optional<BatchResponseDTO> batchResponse =
        batchService.getBatchById(request.getBatchId(), "system");
    if (batchResponse.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Batch not found: " + request.getBatchId());
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
            request.getBillingPeriodEnd().toString());

    // Create invoice items based on attendance
    List<InvoiceItem> items =
        createInvoiceItems(
            request.getStudentId(),
            request.getBatchId(),
            request.getBillingPeriodStart(),
            request.getBillingPeriodEnd(),
            batch);

    // Create invoice
    Invoice invoice =
        Invoice.builder()
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
    activityLogService.logAction(
        ActionType.SYSTEM_ACTION,
        coachId,
        "Coach", // We'll need to get the actual coach name
        UserType.COACH,
        "Generated invoice for student " + request.getStudentId(),
        EntityType.INVOICE,
        invoice.getInvoiceId(),
        "Invoice");

    log.info("evt=invoice_generated invoiceId={}", invoice.getInvoiceId());

    return convertToDTO(invoice);
  }

  @Override
  public InvoiceDTO recordPayment(
      String invoiceId, PaymentRecordRequest request, String coachId, String userType) {
    log.info(
        "evt=record_payment invoiceId={} amount={} coachId={} userType={}",
        invoiceId,
        request.getAmountPaid(),
        coachId,
        userType);

    // Validate that the requesting user is a coach
    if (!"COACH".equals(userType)) {
      log.error("evt=unauthorized_payment_record coachId={} userType={}", coachId, userType);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can record payments");
    }

    Invoice invoice = getInvoiceByIdInternal(invoiceId);
    if (invoice == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found: " + invoiceId);
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
    activityLogService.logPayment(
        coachId,
        "Coach", // We'll need to get the actual coach name
        UserType.COACH,
        invoice.getStudentId(),
        "Student", // We'll need to get the actual student name
        invoice.getBatchId(),
        "Batch", // We'll need to get the actual batch name
        request.getAmountPaid());

    log.info("evt=payment_recorded invoiceId={}", invoiceId);

    return convertToDTO(invoice);
  }

  @Override
  public InvoiceDTO getInvoiceById(String invoiceId) {
    log.info("evt=get_invoice_by_id invoiceId={}", invoiceId);

    Invoice invoice = getInvoiceByIdInternal(invoiceId);

    if (invoice == null) {
      return null;
    }

    return convertToDTO(invoice);
  }

  @Override
  public PageResponseDTO<InvoiceDTO> getInvoicesByStudent(String studentId, int page, int size) {
    log.info("evt=get_invoices_by_student studentId={} page={} size={}", studentId, page, size);

    // Create a query expression to find all invoices for the student
    DynamoDBQueryExpression<Invoice> queryExpression =
        new DynamoDBQueryExpression<Invoice>()
            .withHashKeyValues(Invoice.builder().studentId(studentId).build());

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
      int size) {
    log.info(
        "evt=get_all_invoices status={} dueDateStart={} dueDateEnd={} batchId={} studentId={}"
            + " page={} size={}",
        status,
        dueDateStart,
        dueDateEnd,
        batchId,
        studentId,
        page,
        size);

    // Create a scan expression with filters
    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    StringBuilder filterExpression = new StringBuilder();

    if (status != null && !status.isEmpty()) {
      filterExpression.append("status = :status");
      expressionAttributeValues.put(":status", new AttributeValue().withS(status));
    }

    if (studentId != null && !studentId.isEmpty()) {
      if (filterExpression.length() > 0) {
        filterExpression.append(" AND ");
      }
      filterExpression.append("studentId = :studentId");
      expressionAttributeValues.put(":studentId", new AttributeValue().withS(studentId));
    }

    if (batchId != null && !batchId.isEmpty()) {
      if (filterExpression.length() > 0) {
        filterExpression.append(" AND ");
      }
      filterExpression.append("batchId = :batchId");
      expressionAttributeValues.put(":batchId", new AttributeValue().withS(batchId));
    }

    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
    if (filterExpression.length() > 0) {
      scanExpression.setFilterExpression(filterExpression.toString());
      scanExpression.setExpressionAttributeValues(expressionAttributeValues);
    }

    List<Invoice> invoices = dynamoDBMapper.scan(Invoice.class, scanExpression);

    // Apply additional date filters if needed
    if (dueDateStart != null || dueDateEnd != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      invoices =
          invoices.stream()
              .filter(
                  invoice -> {
                    if (dueDateStart != null) {
                      LocalDate startDate = LocalDate.parse(dueDateStart, formatter);
                      if (invoice.getDueDate().isBefore(startDate)) {
                        return false;
                      }
                    }
                    if (dueDateEnd != null) {
                      LocalDate endDate = LocalDate.parse(dueDateEnd, formatter);
                      if (invoice.getDueDate().isAfter(endDate)) {
                        return false;
                      }
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

    return response;
  }

  @Override
  public InvoiceDTO updateInvoice(
      String invoiceId, InvoiceDTO invoiceDTO, String coachId, String userType) {
    log.info(
        "evt=update_invoice invoiceId={} coachId={} userType={}", invoiceId, coachId, userType);

    // Validate that the requesting user is a coach
    if (!"COACH".equals(userType)) {
      log.error("evt=unauthorized_invoice_update coachId={} userType={}", coachId, userType);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can update invoices");
    }

    Invoice invoice = getInvoiceByIdInternal(invoiceId);
    if (invoice == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found: " + invoiceId);
    }

    // Update fields
    invoice.setCalculatedAmount(invoiceDTO.getCalculatedAmount());
    invoice.setAmountPaid(invoiceDTO.getAmountPaid());
    invoice.setStatus(invoiceDTO.getStatus());
    invoice.setDueDate(invoiceDTO.getDueDate());
    invoice.setUpdatedAt(LocalDateTime.now());

    dynamoDBMapper.save(invoice);

    // Log the activity
    activityLogService.logAction(
        ActionType.SYSTEM_ACTION,
        coachId,
        "Coach", // We'll need to get the actual coach name
        UserType.COACH,
        "Updated invoice " + invoiceId,
        EntityType.INVOICE,
        invoiceId,
        "Invoice");

    log.info("evt=invoice_updated invoiceId={}", invoiceId);

    return convertToDTO(invoice);
  }

  @Override
  public void deleteInvoice(String invoiceId, String coachId, String userType) {
    log.info(
        "evt=delete_invoice invoiceId={} coachId={} userType={}", invoiceId, coachId, userType);

    // Validate that the requesting user is a coach
    if (!"COACH".equals(userType)) {
      log.error("evt=unauthorized_invoice_delete coachId={} userType={}", coachId, userType);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only coaches can delete invoices");
    }

    Invoice invoice = getInvoiceByIdInternal(invoiceId);
    if (invoice == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found: " + invoiceId);
    }

    dynamoDBMapper.delete(invoice);

    // Log the activity
    activityLogService.logAction(
        ActionType.SYSTEM_ACTION,
        coachId,
        "Coach", // We'll need to get the actual coach name
        UserType.COACH,
        "Deleted invoice " + invoiceId,
        EntityType.INVOICE,
        invoiceId,
        "Invoice");

    log.info("evt=invoice_deleted invoiceId={}", invoiceId);
  }

  @Override
  public Double calculateFees(String studentId, String batchId, String startDate, String endDate) {
    log.info(
        "evt=calculate_fees studentId={} batchId={} startDate={} endDate={}",
        studentId,
        batchId,
        startDate,
        endDate);

    // Get batch details
    Optional<BatchResponseDTO> batchResponse = batchService.getBatchById(batchId, "system");
    if (batchResponse.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found: " + batchId);
    }
    BatchResponseDTO batchResponseDTO = batchResponse.get();
    Batch batch = convertToBatchModel(batchResponseDTO);

    // Get attendance records for the period
    PageResponseDTO<AttendanceDTO> attendanceResponse =
        attendanceService.getAttendanceByStudent(studentId, 0, 1000);
    List<AttendanceDTO> attendances = attendanceResponse.getContent();

    // Filter attendances by date range and batch
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate start = LocalDate.parse(startDate, formatter);
    LocalDate end = LocalDate.parse(endDate, formatter);

    List<AttendanceDTO> filteredAttendances =
        attendances.stream()
            .filter(
                attendance -> {
                  // Check if attendance is for the specified batch (we need to get session details)
                  // For now, we'll assume all attendances are for the specified batch
                  return attendance.getMarkedAt().toLocalDate().isAfter(start.minusDays(1))
                      && attendance.getMarkedAt().toLocalDate().isBefore(end.plusDays(1))
                      && attendance.getIsPresent();
                })
            .collect(Collectors.toList());

    // Calculate fees based on batch payment type
    double totalFees = 0.0;

    switch (batch.getPaymentType()) {
      case PER_ATTENDANCE:
        totalFees = filteredAttendances.size() * batch.getPerSessionFee();
        break;
      case PER_SESSION:
        // Count unique sessions
        long uniqueSessions =
            filteredAttendances.stream().map(AttendanceDTO::getSessionId).distinct().count();
        totalFees = uniqueSessions * batch.getPerSessionFee();
        break;
      case MONTHLY:
      case FIXED_MONTHLY:
        // Calculate number of months in the billing period
        long months = java.time.temporal.ChronoUnit.MONTHS.between(start, end) + 1;
        totalFees = months * batch.getFixedMonthlyFee();
        break;
      case ONE_TIME:
        totalFees = batch.getPerSessionFee();
        break;
      default:
        totalFees = 0.0;
    }

    log.info(
        "evt=fees_calculated totalFees={} studentId={} batchId={}", totalFees, studentId, batchId);

    return totalFees;
  }

  private Invoice getInvoiceByIdInternal(String invoiceId) {
    // Create a scan expression to find the invoice by ID
    DynamoDBScanExpression scanExpression =
        new DynamoDBScanExpression()
            .withFilterExpression("invoiceId = :invoiceId")
            .withExpressionAttributeValues(
                Map.of(":invoiceId", new AttributeValue().withS(invoiceId)));

    List<Invoice> invoices = dynamoDBMapper.scan(Invoice.class, scanExpression);

    if (invoices.isEmpty()) {
      return null;
    }

    return invoices.get(0);
  }

  private List<InvoiceItem> createInvoiceItems(
      String studentId, String batchId, LocalDate startDate, LocalDate endDate, Batch batch) {
    List<InvoiceItem> items = new ArrayList<>();

    // Get attendance records for the period
    PageResponseDTO<AttendanceDTO> attendanceResponse =
        attendanceService.getAttendanceByStudent(studentId, 0, 1000);
    List<AttendanceDTO> attendances = attendanceResponse.getContent();

    // Filter attendances by date range
    List<AttendanceDTO> filteredAttendances =
        attendances.stream()
            .filter(
                attendance -> {
                  return attendance.getMarkedAt().toLocalDate().isAfter(startDate.minusDays(1))
                      && attendance.getMarkedAt().toLocalDate().isBefore(endDate.plusDays(1))
                      && attendance.getIsPresent();
                })
            .collect(Collectors.toList());

    // Create invoice items based on payment type
    switch (batch.getPaymentType()) {
      case PER_ATTENDANCE:
        for (AttendanceDTO attendance : filteredAttendances) {
          InvoiceItem item =
              InvoiceItem.builder()
                  .sessionId(attendance.getSessionId())
                  .sessionDate(attendance.getMarkedAt().toLocalDate())
                  .description("Attendance for session " + attendance.getSessionId())
                  .amount(batch.getPerSessionFee())
                  .type("ATTENDANCE")
                  .build();
          items.add(item);
        }
        break;
      case PER_SESSION:
        // Group by session
        Map<String, List<AttendanceDTO>> sessionGroups =
            filteredAttendances.stream()
                .collect(Collectors.groupingBy(AttendanceDTO::getSessionId));

        for (Map.Entry<String, List<AttendanceDTO>> entry : sessionGroups.entrySet()) {
          InvoiceItem item =
              InvoiceItem.builder()
                  .sessionId(entry.getKey())
                  .sessionDate(entry.getValue().get(0).getMarkedAt().toLocalDate())
                  .description("Session " + entry.getKey())
                  .amount(batch.getPerSessionFee())
                  .type("SESSION")
                  .build();
          items.add(item);
        }
        break;
      case MONTHLY:
      case FIXED_MONTHLY:
        // Calculate number of months
        long months = java.time.temporal.ChronoUnit.MONTHS.between(startDate, endDate) + 1;
        InvoiceItem item =
            InvoiceItem.builder()
                .sessionId(null)
                .sessionDate(startDate)
                .description("Monthly fee for " + months + " month(s)")
                .amount(months * batch.getFixedMonthlyFee())
                .type("FIXED_MONTHLY")
                .build();
        items.add(item);
        break;
      case ONE_TIME:
        InvoiceItem oneTimeItem =
            InvoiceItem.builder()
                .sessionId(null)
                .sessionDate(startDate)
                .description("One-time fee")
                .amount(batch.getPerSessionFee())
                .type("ONE_TIME")
                .build();
        items.add(oneTimeItem);
        break;
    }

    return items;
  }

  private InvoiceDTO convertToDTO(Invoice invoice) {
    InvoiceDTO dto = new InvoiceDTO();
    dto.setStudentId(invoice.getStudentId());
    dto.setInvoiceId(invoice.getInvoiceId());
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
    if (invoice.getItems() != null) {
      dto.setItems(
          invoice.getItems().stream().map(this::convertToItemDTO).collect(Collectors.toList()));
    }

    return dto;
  }

  private InvoiceItemDTO convertToItemDTO(InvoiceItem item) {
    InvoiceItemDTO dto = new InvoiceItemDTO();
    dto.setSessionId(item.getSessionId());
    dto.setSessionDate(item.getSessionDate());
    dto.setDescription(item.getDescription());
    dto.setAmount(item.getAmount());
    dto.setType(item.getType());
    return dto;
  }

  private Batch convertToBatchModel(BatchResponseDTO batchResponseDTO) {
    Batch batch = new Batch();
    batch.setBatchId(batchResponseDTO.getBatchId());
    batch.setBatchName(batchResponseDTO.getBatchName());
    batch.setBatchSize(batchResponseDTO.getBatchSize());
    // currentStudents is now calculated dynamically, not stored
    batch.setStartDate(batchResponseDTO.getStartDate());
    batch.setEndDate(batchResponseDTO.getEndDate());
    batch.setPaymentType(batchResponseDTO.getPaymentType());
    batch.setFixedMonthlyFee(batchResponseDTO.getFixedMonthlyFee());
    batch.setPerSessionFee(batchResponseDTO.getPerSessionFee());
    batch.setOccurrenceType(batchResponseDTO.getOccurrenceType());
    batch.setBatchStatus(batchResponseDTO.getBatchStatus());
    batch.setNotes(batchResponseDTO.getNotes());
    batch.setCoachId(batchResponseDTO.getCoachId());
    batch.setCreatedAt(batchResponseDTO.getCreatedAt());
    batch.setUpdatedAt(batchResponseDTO.getUpdatedAt());
    return batch;
  }
}
