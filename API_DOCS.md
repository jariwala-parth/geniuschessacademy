# Genius Chess Academy API Documentation

## Overview

This document provides comprehensive API documentation for the Genius Chess Academy backend. The API is built with Spring Boot and uses JWT tokens for authentication.

## Base URLs

- **Local Development**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Documentation**: `http://localhost:8080/v3/api-docs`

## Authentication

Protected endpoints require JWT authentication. Obtain a token by logging in and include it in the `Authorization` header.

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

#### 1. User Registration (Public - Coach Signup)
**Note:** This endpoint is public and intended for initial coach registration only.

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

#### 2. Add Student (Coach Only - Authentication Required)
**Authorization Required:** Only authenticated coaches can add students.

```bash
curl -X POST http://localhost:8080/api/v1/auth/add-student \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "username": "alice_student",
    "email": "student@example.com",
    "password": "SecurePass123!",
    "name": "Alice Smith",
    "phoneNumber": "+1234567890",
    "guardianName": "Bob Smith",
    "guardianPhone": "+1234567891"
  }'
```

**Response:**
```json
{
  "userId": "USER_student123",
  "email": "student@example.com",
  "name": "Alice Smith",
  "userType": "STUDENT",
  "phoneNumber": "+1234567890"
}
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

### 👥 User Management APIs

> **Authorization Required**: Only coaches can access user management endpoints.

#### 1. Get All Students (with Search & Pagination)
```bash
# Get first page (100 students)
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/users/students"

# Search students
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/users/students?search=alice&page=0&size=50"

# Get specific page
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/users/students?page=1&size=100"
```

**Response:**
```json
{
  "content": [
    {
      "userId": "USER_student123",
      "email": "alice@example.com",
      "name": "Alice Smith",
      "userType": "STUDENT",
      "phoneNumber": "+1234567890"
    }
  ],
  "pageInfo": {
    "currentPage": 0,
    "pageSize": 100,
    "totalPages": 2,
    "totalElements": 150
  }
}
```

#### 2. Get All Coaches (with Search & Pagination)
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/users/coaches?search=john&page=0&size=50"
```

**Response:** Same format as students endpoint.

---

### 👥 Enrollment Management APIs

> **Authorization Required**: Only coaches can access enrollment endpoints.

#### 1. Bulk Enroll Students
```bash
curl -X POST http://localhost:8080/api/v1/enrollments/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '[
    {
      "batchId": "BATCH_12345",
      "studentId": "USER_student123",
      "enrollmentStatus": "ENROLLED",
      "enrollmentPaymentStatus": "PENDING",
      "currentPaymentAmount": 1500,
      "enrollmentDate": "2025-01-15",
      "notes": "Bulk enrollment"
    },
    {
      "batchId": "BATCH_12345", 
      "studentId": "USER_student456",
      "enrollmentStatus": "ENROLLED",
      "enrollmentPaymentStatus": "PENDING",
      "currentPaymentAmount": 1500,
      "enrollmentDate": "2025-01-15",
      "notes": "Bulk enrollment"
    }
  ]'
```

**Response:**
```json
[
  {
    "batchId": "BATCH_12345",
    "studentId": "USER_student123",
    "enrollmentDate": "2025-01-15",
    "enrollmentStatus": "ENROLLED",
    "enrollmentPaymentStatus": "PENDING",
    "currentPaymentAmount": 1500,
    "notes": "Bulk enrollment",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00",
    "batchName": "Monday 6:30 PM",
    "studentName": "Alice Smith"
  }
]
```

**Note**: Returns only successful enrollments. If some enrollments fail (e.g., student already enrolled), they are skipped and logged.

#### 2. Individual Enrollment
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

#### 3. Get Specific Enrollment
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/BATCH_12345abcd/USER_student123"
```

#### 4. Get All Enrollments
```bash
# Basic request
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments"

# With filtering
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments?enrollmentStatus=ENROLLED&paymentStatus=PENDING&page=0&size=10"
```

#### 5. Get Students in a Batch
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/batch/BATCH_12345abcd?page=0&size=10"
```

#### 6. Get Student's Enrollments
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/student/USER_student123?page=0&size=10"
```

#### 7. Update Enrollment
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

#### 8. Update Payment Status
```bash
curl -X PATCH "http://localhost:8080/api/v1/enrollments/BATCH_12345abcd/USER_student123/payment?paymentStatus=PAID&paymentAmount=1500.0" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 9. Unenroll Student
```bash
curl -X DELETE http://localhost:8080/api/v1/enrollments/BATCH_12345abcd/USER_student123 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 10. Get Enrollments by Payment Status
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  "http://localhost:8080/api/v1/enrollments/payment-status/OVERDUE?page=0&size=10"
```

#### 11. Alternative RESTful Endpoints
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
- ✅ Add new students (`/api/v1/auth/add-student`)
- ✅ Get all students and coaches (`/api/v1/users/students`, `/api/v1/users/coaches`)
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
- ❌ Cannot add new students
- ❌ Cannot access user management endpoints

### Public Access
- ✅ Login (`/api/v1/auth/login`)
- ✅ Initial signup (`/api/v1/auth/signup`) - intended for coach registration
- ✅ Password reset endpoints

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

### 1. Complete Coach Setup Flow
```bash
# 1. Register as coach (public)
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"coach_john","email":"coach@example.com","password":"SecurePass123!","name":"John Doe","phoneNumber":"+1234567890","userType":"COACH","isAdmin":true}'

# 2. Login to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"coach@example.com","password":"SecurePass123!","userType":"COACH"}' \
  | jq -r '.accessToken')

# 3. Add a student
curl -X POST http://localhost:8080/api/v1/auth/add-student \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"username":"alice_student","email":"alice@example.com","password":"StudentPass123!","name":"Alice Smith","phoneNumber":"+1234567890","guardianName":"Bob Smith","guardianPhone":"+1234567891"}'

# 4. Get all students
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/users/students"
```

### 2. Student Management & Enrollment Flow
```bash
# 1. Get students with search (paginated)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/users/students?search=alice&page=0&size=100"

# 2. Get students for a specific batch (to see who's already enrolled)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/enrollments/batch/BATCH_12345"

# 3. Bulk enroll multiple students
curl -X POST http://localhost:8080/api/v1/enrollments/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '[
    {
      "batchId": "BATCH_12345",
      "studentId": "USER_student123", 
      "enrollmentStatus": "ENROLLED",
      "enrollmentPaymentStatus": "PENDING",
      "currentPaymentAmount": 1500
    },
    {
      "batchId": "BATCH_12345",
      "studentId": "USER_student456",
      "enrollmentStatus": "ENROLLED", 
      "enrollmentPaymentStatus": "PENDING",
      "currentPaymentAmount": 1500
    }
  ]'
```

### 3. Smart Search Workflow (Frontend Integration)
```bash
# Step 1: Check total students count
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/users/students?page=0&size=100"

# If totalElements <= 100: Load all and search locally in frontend
# If totalElements > 100: Use backend search for real-time results

# Step 2: Backend search (when needed)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/users/students?search=john&page=0&size=100"

# Step 3: Load more pages as needed
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/users/students?search=john&page=1&size=100"
```

### 4. Batch Management Flow
```bash
# 1. Create a batch
curl -X POST http://localhost:8080/api/v1/batches \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"batchName":"Beginner Chess","batchSize":10,"startDate":"2025-02-01","endDate":"2025-04-30","batchTiming":{"daysOfWeek":["MONDAY"],"startTime":"18:00","endTime":"19:00"},"paymentType":"MONTHLY","monthlyFee":1500,"occurrenceType":"REGULAR","coachId":"USER_coach123"}'

# 2. Get all batches
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/batches"

# 3. Get specific batch by ID
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/batches/BATCH_12345abcd"

# 4. Update batch
curl -X PUT http://localhost:8080/api/v1/batches/BATCH_12345abcd \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"batchName":"Beginner Chess (Updated)","batchSize":15,"startDate":"2025-02-01","endDate":"2025-05-31","batchTiming":{"daysOfWeek":["MONDAY","WEDNESDAY","FRIDAY"],"startTime":"18:00","endTime":"19:30"},"paymentType":"MONTHLY","monthlyFee":1600,"occurrenceType":"REGULAR","batchStatus":"ACTIVE","notes":"Updated curriculum with advanced tactics","coachId":"USER_coach123"}'

# 5. Delete batch
curl -X DELETE http://localhost:8080/api/v1/batches/BATCH_12345abcd \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Payment Tracking
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

- `/api/v1/auth/signup` is public for initial coach registration
- `/api/v1/auth/add-student` requires coach authentication for adding students
- `/api/v1/users/students` and `/api/v1/users/coaches` require coach authentication
- All other protected endpoints support CORS for frontend integration
- JWT tokens expire in 1 hour by default
- Use refresh tokens for extended sessions
- All dates use ISO format (YYYY-MM-DD)
- All timestamps use ISO format (YYYY-MM-DDTHH:mm:ss)
- Soft deletes are used for batches (status change to CANCELLED)
- Hard deletes are used for enrollments
- Pagination uses 0-based indexing 