# Genius Chess Academy API Documentation

## Overview
This document describes the REST API endpoints for the Genius Chess Academy management system.

## Base URL
- Development: `http://localhost:8080/api/v1`
- Production: `https://your-domain.com/api/v1`

## Authentication
All endpoints require JWT authentication via Bearer token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

## Common Headers
- `Content-Type: application/json`
- `X-User-ID: <user_id>` (for operations requiring user context)

## Payment Types
The system now supports multiple payment models:

### Payment Types
- `FIXED_MONTHLY`: Student pays a flat fee per month, regardless of sessions/attendance
- `PER_SESSION`: Student pays a fixed fee per scheduled session in that month
- `PER_ATTENDANCE`: Student pays only for sessions they actually attend
- `ONE_TIME`: One-time payment for the entire course (legacy)
- `MONTHLY`: Fixed monthly payment (legacy)

### Payment Fields
- `fixedMonthlyFee`: Amount for FIXED_MONTHLY payment type
- `perSessionFee`: Amount per session for PER_SESSION and PER_ATTENDANCE payment types
- `monthlyFee`: Legacy field for MONTHLY payment type
- `oneTimeFee`: Legacy field for ONE_TIME payment type

## Endpoints

### Authentication

#### POST /auth/login
Login with username and password.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "user": {
    "userId": "string",
    "name": "string",
    "email": "string",
    "userType": "COACH|STUDENT"
  }
}
```

#### POST /auth/add-student
Add a new student (coach only).

**Request Body:**
```json
{
  "name": "string",
  "email": "string",
  "phone": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "userId": "string",
  "name": "string",
  "email": "string",
  "userType": "STUDENT"
}
```

### Batch Management

#### POST /batches
Create a new batch (coach only).

**Request Body:**
```json
{
  "batchName": "string",
  "batchSize": "number",
  "startDate": "YYYY-MM-DD",
  "endDate": "YYYY-MM-DD",
  "batchTiming": {
    "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
    "startTime": "HH:mm",
    "endTime": "HH:mm"
  },
  "paymentType": "FIXED_MONTHLY|PER_SESSION|PER_ATTENDANCE|ONE_TIME|MONTHLY",
  "fixedMonthlyFee": "number (for FIXED_MONTHLY)",
  "perSessionFee": "number (for PER_SESSION/PER_ATTENDANCE)",
  "monthlyFee": "number (legacy for MONTHLY)",
  "oneTimeFee": "number (legacy for ONE_TIME)",
  "occurrenceType": "REGULAR|WEEKLY|DAILY",
  "batchStatus": "UPCOMING|ACTIVE|FULL|COMPLETED|CANCELLED",
  "notes": "string",
  "coachId": "string"
}
```

#### GET /batches
Get all batches with optional filters.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 10)
- `status` (optional)
- `nameContains` (optional)
- `paymentType` (optional)
- `coachId` (optional)

#### PUT /batches/{batchId}
Update a batch (coach only).

#### DELETE /batches/{batchId}
Delete a batch (coach only).

### Session Management

#### POST /sessions
Create a new session (coach only).

**Request Body:**
```json
{
  "batchId": "string",
  "sessionDate": "YYYY-MM-DD",
  "startTime": "HH:mm",
  "endTime": "HH:mm",
  "coachId": "string (optional)",
  "notes": "string (optional)"
}
```

#### GET /sessions/batch/{batchId}
Get all sessions for a specific batch.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)

#### GET /sessions/date-range
Get sessions within a date range.

**Query Parameters:**
- `startDate` (YYYY-MM-DD)
- `endDate` (YYYY-MM-DD)
- `page` (default: 0)
- `size` (default: 20)

#### POST /sessions/batch/{batchId}/generate
Automatically generate sessions for a batch based on its schedule.

**Query Parameters:**
- `startDate` (YYYY-MM-DD)
- `endDate` (YYYY-MM-DD)

#### GET /sessions/today
Get today's sessions for the authenticated coach.

#### PUT /sessions/{sessionId}
Update a session (coach only).

**Request Body:**
```json
{
  "status": "SCHEDULED|COMPLETED|CANCELLED",
  "coachId": "string (optional)",
  "notes": "string (optional)"
}
```

#### DELETE /sessions/{sessionId}
Delete a session (coach only).

### Student Management

#### GET /users/students
Get all students with pagination and search.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 10)
- `search` (optional)

#### GET /users/coaches
Get all coaches with pagination and search.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 10)
- `search` (optional)

### Enrollment Management

#### POST /enrollments
Create a new enrollment (coach only).

**Request Body:**
```json
{
  "batchId": "string",
  "studentId": "string",
  "enrollmentDate": "YYYY-MM-DD",
  "enrollmentStatus": "ENROLLED|DROPPED|COMPLETED",
  "enrollmentPaymentStatus": "PENDING|PAID|PARTIALLY_PAID|OVERDUE|REFUNDED",
  "currentPaymentAmount": "number",
  "notes": "string (optional)"
}
```

#### POST /enrollments/bulk
Bulk enroll multiple students (coach only).

**Request Body:**
```json
{
  "enrollments": [
    {
      "batchId": "string",
      "studentId": "string",
      "enrollmentDate": "YYYY-MM-DD",
      "enrollmentStatus": "ENROLLED",
      "enrollmentPaymentStatus": "PENDING",
      "currentPaymentAmount": "number"
    }
  ]
}
```

#### GET /enrollments
Get all enrollments with optional filters.

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 10)
- `batchId` (optional)
- `studentId` (optional)

### Activity Logs

#### GET /activity-logs/recent
Get recent activity logs (coach only).

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)

## Common Use Cases

### 1. Coach Creates a Batch with Per-Session Payment
```bash
POST /batches
{
  "batchName": "Advanced Chess - Evening",
  "batchSize": 8,
  "startDate": "2025-08-01",
  "endDate": "2025-12-31",
  "batchTiming": {
    "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
    "startTime": "18:00",
    "endTime": "19:30"
  },
  "paymentType": "PER_SESSION",
  "perSessionFee": 500.0,
  "occurrenceType": "REGULAR",
  "batchStatus": "UPCOMING",
  "coachId": "USER_123"
}
```

### 2. Generate Sessions for a Batch
```bash
POST /sessions/batch/BATCH_456/generate?startDate=2025-08-01&endDate=2025-08-31
```

### 3. Bulk Enroll Students
```bash
POST /enrollments/bulk
{
  "enrollments": [
    {
      "batchId": "BATCH_456",
      "studentId": "STUDENT_789",
      "enrollmentDate": "2025-07-30",
      "enrollmentStatus": "ENROLLED",
      "enrollmentPaymentStatus": "PENDING",
      "currentPaymentAmount": 0.0
    }
  ]
}
```

## Error Responses
All endpoints return consistent error responses:

```json
{
  "timestamp": "2025-07-29T01:52:02",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/batches"
}
```

## Status Codes
- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `500` - Internal Server Error 