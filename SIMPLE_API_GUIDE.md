# 🚀 Quick API Reference Guide

> **Backup Documentation** - Use this if Swagger UI has issues

## 🔑 Authentication

### Get JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "coach@example.com",
    "password": "YourPassword123!",
    "userType": "COACH"
  }'
```

### Use Token in Headers
```bash
Authorization: Bearer YOUR_JWT_TOKEN_HERE
```

## 📚 Batch APIs (Coach Only)

### Create Batch
```bash
curl -X POST http://localhost:8080/api/v1/batches \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "batchName": "Beginner Chess",
    "batchSize": 10,
    "startDate": "2025-02-01",
    "endDate": "2025-04-30",
    "batchTiming": {
      "daysOfWeek": ["MONDAY", "WEDNESDAY"],
      "startTime": "18:00",
      "endTime": "19:30"
    },
    "paymentType": "MONTHLY",
    "monthlyFee": 1500,
    "coachId": "USER_coach123"
  }'
```

### Get All Batches
```bash
curl -H "Authorization: Bearer TOKEN" \
  "http://localhost:8080/api/v1/batches?page=0&size=10"
```

### Get Batch by ID
```bash
curl -H "Authorization: Bearer TOKEN" \
  "http://localhost:8080/api/v1/batches/BATCH_ID"
```

## 👥 Enrollment APIs (Coach Only)

### Enroll Student
```bash
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_12345",
    "studentId": "USER_student123",
    "enrollmentStatus": "ENROLLED",
    "enrollmentPaymentStatus": "PENDING"
  }'
```

### Get Students in Batch
```bash
curl -H "Authorization: Bearer TOKEN" \
  "http://localhost:8080/api/v1/enrollments/batch/BATCH_ID"
```

### Update Payment Status
```bash
curl -X PATCH "http://localhost:8080/api/v1/enrollments/BATCH_ID/STUDENT_ID/payment?paymentStatus=PAID&paymentAmount=1500" \
  -H "Authorization: Bearer TOKEN"
```

## 🔧 Test Endpoints

```bash
# Health Check
curl http://localhost:8080/ping

# API Documentation (if Swagger works)
curl http://localhost:8080/v3/api-docs
```

## 📊 Response Format
```json
{
  "batchId": "BATCH_12345",
  "batchName": "Beginner Chess",
  "batchSize": 10,
  "currentStudents": 5,
  "batchStatus": "ACTIVE",
  "coachId": "USER_coach123"
}
```

## ❌ Error Format
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Batch name is required",
  "status": 400,
  "timestamp": "2025-01-27T19:00:00"
}
```

## 🔒 Authorization Rules
- **Coaches**: Can do everything
- **Students**: Can only view their own data
- **Unauthenticated**: Can only access auth endpoints 