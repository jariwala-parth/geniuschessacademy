# Genius Chess Academy API Documentation

## Overview

This document provides comprehensive API documentation for the Genius Chess Academy backend. The API is built with Spring Boot and uses JWT tokens for authentication.

## Base URLs

- **Local Development**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Documentation**: `http://localhost:8080/v3/api-docs`

## Authentication

All protected endpoints require JWT authentication. Obtain a token by logging in and include it in the `Authorization` header.

### Format
```
Authorization: Bearer <your_jwt_token>
```

### Getting a Token
```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "coach@example.com",
    "password": "YourPassword123!",
    "userType": "COACH"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userInfo": {
    "userId": "USER_abc123",
    "email": "coach@example.com",
    "userType": "COACH"
  }
}
```

## API Endpoints

### 🔐 Authentication APIs

#### 1. User Registration (Coach)
```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe_coach",
    "email": "coach@example.com",
    "password": "SecurePass123!",
    "name": "John Doe",
    "phoneNumber": "+1234567890",
    "userType": "COACH",
    "isAdmin": true
  }'
```

#### 2. User Registration (Student)
```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice_student",
    "email": "student@example.com",
    "password": "SecurePass123!",
    "name": "Alice Smith",
    "phoneNumber": "+1234567890",
    "userType": "STUDENT",
    "guardianName": "Bob Smith",
    "guardianPhone": "+1234567891"
  }'
```

#### 3. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "coach@example.com",
    "password": "SecurePass123!",
    "userType": "COACH"
  }'
```

#### 4. Refresh Token
```bash
curl -X POST "http://localhost:8080/api/v1/auth/refresh?refreshToken=YOUR_REFRESH_TOKEN"
```

#### 5. Logout
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 6. Change Password
```bash
curl -X POST "http://localhost:8080/api/v1/auth/change-password?oldPassword=OldPass123!&newPassword=NewPass123!" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 7. Forgot Password
```bash
curl -X POST "http://localhost:8080/api/v1/auth/forgot-password?email=coach@example.com"
```

#### 8. Reset Password
```bash
curl -X POST "http://localhost:8080/api/v1/auth/reset-password?email=coach@example.com&confirmationCode=123456&newPassword=NewPass123!"
```

---

### 📚 Batch Management APIs

> **Authorization Required**: Only coaches can create, update, and delete batches.

#### 1. Create Batch
```bash
curl -X POST http://localhost:8080/api/v1/batches \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "batchName": "Beginner Chess - Evening",
    "batchSize": 15,
    "startDate": "2025-02-01",
    "endDate": "2025-04-30",
    "batchTiming": {
      "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
      "startTime": "18:00",
      "endTime": "19:30"
    },
    "paymentType": "MONTHLY",
    "monthlyFee": 1500.00,
    "occurrenceType": "REGULAR",
    "batchStatus": "UPCOMING",
    "notes": "Focus on fundamentals and basic tactics",
    "coachId": "USER_coach123"
  }'
```

#### 2. Get All Batches
```bash
# Basic request
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/batches"

# With filtering and pagination
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/batches?status=ACTIVE&coachId=USER_coach123&page=0&size=10"
```

#### 3. Get Batch by ID
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/batches/BATCH_12345abcd"
```

#### 4. Update Batch
```bash
curl -X PUT http://localhost:8080/api/v1/batches/BATCH_12345abcd \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "batchName": "Beginner Chess - Evening (Updated)",
    "batchSize": 18,
    "startDate": "2025-02-01",
    "endDate": "2025-05-31",
    "batchTiming": {
      "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
      "startTime": "18:00",
      "endTime": "19:30"
    },
    "paymentType": "MONTHLY",
    "monthlyFee": 1600.00,
    "occurrenceType": "REGULAR",
    "batchStatus": "ACTIVE",
    "notes": "Updated curriculum with advanced tactics",
    "coachId": "USER_coach123"
  }'
```

#### 5. Delete Batch (Soft Delete)
```bash
curl -X DELETE http://localhost:8080/api/v1/batches/BATCH_12345abcd \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 6. Get Batches by Coach
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/batches/coach/USER_coach123?page=0&size=10"
```

#### 7. Get Batches by Status
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/batches/status/ACTIVE?page=0&size=10"
```

---

### 👥 Enrollment Management APIs

> **Authorization Required**: Only coaches can create and manage enrollments. Students can view their own enrollments.

#### 1. Enroll Student in Batch
```bash
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "batchId": "BATCH_12345abcd",
    "studentId": "USER_student123",
    "enrollmentDate": "2025-01-27",
    "enrollmentStatus": "ENROLLED",
    "enrollmentPaymentStatus": "PENDING",
    "currentPaymentAmount": 0.0,
    "notes": "New student enrollment"
  }'
```

#### 2. Get Specific Enrollment
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/BATCH_12345abcd/USER_student123"
```

#### 3. Get All Enrollments
```bash
# Basic request
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments"

# With filtering
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments?enrollmentStatus=ENROLLED&paymentStatus=PENDING&page=0&size=10"
```

#### 4. Get Students in a Batch
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/batch/BATCH_12345abcd?page=0&size=10"
```

#### 5. Get Student's Enrollments
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/student/USER_student123?page=0&size=10"
```

#### 6. Update Enrollment
```bash
curl -X PUT http://localhost:8080/api/v1/enrollments/BATCH_12345abcd/USER_student123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "enrollmentStatus": "ENROLLED",
    "enrollmentPaymentStatus": "PAID",
    "currentPaymentAmount": 1500.00,
    "notes": "Payment received - full amount"
  }'
```

#### 7. Update Payment Status
```bash
curl -X PATCH "http://localhost:8080/api/v1/enrollments/BATCH_12345abcd/USER_student123/payment?paymentStatus=PAID&paymentAmount=1500.0" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 8. Unenroll Student
```bash
curl -X DELETE http://localhost:8080/api/v1/enrollments/BATCH_12345abcd/USER_student123 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 9. Get Enrollments by Payment Status
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/payment-status/OVERDUE?page=0&size=10"
```

#### 10. Alternative RESTful Endpoints
```bash
# Get batch enrollments (alternative route)
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/batches/BATCH_12345abcd/enrollments"

# Get student enrollments (alternative route)
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/students/USER_student123/enrollments"
```

---

## 📊 Response Formats

### Success Response Example
```json
{
  "batchId": "BATCH_12345abcd",
  "batchName": "Beginner Chess - Evening",
  "batchSize": 15,
  "currentStudents": 5,
  "startDate": "2025-02-01",
  "endDate": "2025-04-30",
  "batchTiming": {
    "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
    "startTime": "18:00",
    "endTime": "19:30"
  },
  "paymentType": "MONTHLY",
  "monthlyFee": 1500.00,
  "occurrenceType": "REGULAR",
  "batchStatus": "ACTIVE",
  "notes": "Focus on fundamentals and basic tactics",
  "coachId": "USER_coach123",
  "createdAt": "2025-01-27T19:00:00",
  "updatedAt": "2025-01-27T19:00:00"
}
```

### Paginated Response Example
```json
{
  "content": [
    {
      "batchId": "BATCH_12345abcd",
      "batchName": "Beginner Chess - Evening",
      // ... batch details
    }
  ],
  "pageInfo": {
    "currentPage": 0,
    "pageSize": 10,
    "totalPages": 3,
    "totalElements": 25
  }
}
```

### Error Response Example
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Batch name is required",
  "status": 400,
  "timestamp": "2025-01-27T19:00:00",
  "path": "/api/v1/batches"
}
```

---

## 🔒 Authorization Rules

### Coach Permissions
- ✅ Create, update, delete batches
- ✅ Enroll/unenroll students
- ✅ View all enrollments and batches
- ✅ Update payment statuses
- ✅ Access all student data

### Student Permissions
- ✅ View their own enrollments
- ✅ View batch information for enrolled batches
- ❌ Cannot create or modify batches
- ❌ Cannot enroll/unenroll themselves or others
- ❌ Cannot access other students' data

---

## 🏷️ Enums and Constants

### Batch Status
- `UPCOMING` - Batch is scheduled but not started
- `ACTIVE` - Batch is currently running
- `FULL` - Batch has reached capacity
- `COMPLETED` - Batch has finished
- `CANCELLED` - Batch has been cancelled

### Payment Type
- `ONE_TIME` - Single payment for entire batch
- `MONTHLY` - Monthly recurring payments

### Occurrence Type
- `REGULAR` - Standard recurring schedule
- `WEEKLY` - Weekly occurrences
- `DAILY` - Daily occurrences

### Enrollment Status
- `ENROLLED` - Student is actively enrolled
- `DROPPED` - Student has dropped out
- `COMPLETED` - Student has completed the batch

### Payment Status
- `PENDING` - Payment is due
- `PAID` - Payment has been received
- `PARTIALLY_PAID` - Partial payment received
- `OVERDUE` - Payment is overdue
- `REFUNDED` - Payment has been refunded

---

## 🚀 Testing with Swagger UI

For interactive API testing, visit `http://localhost:8080/swagger-ui.html` when the application is running locally.

### Steps to use Swagger UI:
1. Start the application: `mvn spring-boot:run`
2. Open browser to `http://localhost:8080/swagger-ui.html`
3. Click "Authorize" button
4. Enter: `Bearer YOUR_JWT_TOKEN` (get token from login endpoint)
5. Test any endpoint interactively

---

## 📋 Common Use Cases

### 1. Complete Batch Creation Flow
```bash
# 1. Login as coach
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"coach@example.com","password":"YourPassword123!","userType":"COACH"}' \
  | jq -r '.accessToken')

# 2. Create batch
curl -X POST http://localhost:8080/api/v1/batches \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"batchName":"Test Batch","batchSize":10,"startDate":"2025-02-01","endDate":"2025-04-30","batchTiming":{"daysOfWeek":["MONDAY"],"startTime":"18:00","endTime":"19:00"},"paymentType":"MONTHLY","monthlyFee":1500,"occurrenceType":"REGULAR","coachId":"USER_coach123"}'
```

### 2. Student Enrollment Flow
```bash
# 1. Enroll student
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"batchId":"BATCH_12345","studentId":"USER_student123","enrollmentStatus":"ENROLLED","enrollmentPaymentStatus":"PENDING"}'

# 2. Update payment status
curl -X PATCH "http://localhost:8080/api/v1/enrollments/BATCH_12345/USER_student123/payment?paymentStatus=PAID&paymentAmount=1500" \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Payment Tracking
```bash
# Get all overdue payments
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/enrollments/payment-status/OVERDUE"

# Get specific student's payment status
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/enrollments/student/USER_student123"
```

---

## 🛠️ Development Notes

- All endpoints support CORS for frontend integration
- JWT tokens expire in 1 hour by default
- Use refresh tokens for extended sessions
- All dates use ISO format (YYYY-MM-DD)
- All timestamps use ISO format (YYYY-MM-DDTHH:mm:ss)
- Soft deletes are used for batches (status change to CANCELLED)
- Hard deletes are used for enrollments
- Pagination uses 0-based indexing 