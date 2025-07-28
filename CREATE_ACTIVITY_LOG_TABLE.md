# Create Activity Log DynamoDB Table

This document provides instructions for creating the GCA_ActivityLogs DynamoDB table.

## Table Structure

### Main Table
- **Table Name**: `GCA_ActivityLogs`
- **Partition Key**: `logId` (String)
- **Sort Key**: `timestamp` (String) - ISO DateTime format

### Global Secondary Indexes (GSI)

#### GSI-1: UserIdIndex
- **Partition Key**: `userId` (String)
- **Sort Key**: `timestamp` (String)
- **Purpose**: Query activities for a specific user, sorted by time

#### GSI-2: ActionTypeIndex
- **Partition Key**: `actionType` (String)
- **Sort Key**: `timestamp` (String)
- **Purpose**: Query activities by action type with time range (e.g., all LOGIN activities today)

#### GSI-3: EntityIndex
- **Partition Key**: `entityType` (String)
- **Sort Key**: `entityId` (String)
- **Purpose**: Query all activities for a specific entity (e.g., all actions on a specific student or batch)

## AWS CLI Commands

### Create the Table (Recommended: On-Demand)
```bash
aws dynamodb create-table \
    --table-name GCA_ActivityLogs \
    --attribute-definitions \
        AttributeName=logId,AttributeType=S \
        AttributeName=timestamp,AttributeType=S \
        AttributeName=userId,AttributeType=S \
        AttributeName=actionType,AttributeType=S \
    --key-schema \
        AttributeName=logId,KeyType=HASH \
        AttributeName=timestamp,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        '[
            {
                "IndexName": "UserIdIndex",
                "KeySchema": [
                    { "AttributeName": "userId", "KeyType": "HASH" },
                    { "AttributeName": "timestamp", "KeyType": "RANGE" }
                ],
                "Projection": {
                    "ProjectionType": "ALL"
                }
            },
            {
                "IndexName": "ActionTypeIndex",
                "KeySchema": [
                    { "AttributeName": "actionType", "KeyType": "HASH" },
                    { "AttributeName": "timestamp", "KeyType": "RANGE" }
                ],
                "Projection": {
                    "ProjectionType": "ALL"
                }
            }
        ]'
```

### Alternative: Provisioned Capacity (for predictable workloads)
```bash
aws dynamodb create-table \
    --table-name GCA_ActivityLogs \
    --attribute-definitions \
        AttributeName=logId,AttributeType=S \
        AttributeName=timestamp,AttributeType=S \
        AttributeName=userId,AttributeType=S \
        AttributeName=actionType,AttributeType=S \
        AttributeName=entityType,AttributeType=S \
        AttributeName=entityId,AttributeType=S \
    --key-schema \
        AttributeName=logId,KeyType=HASH \
        AttributeName=timestamp,KeyType=RANGE \
    --global-secondary-indexes \
        '[
            {
                "IndexName": "UserIdIndex",
                "KeySchema": [
                    {"AttributeName": "userId", "KeyType": "HASH"},
                    {"AttributeName": "timestamp", "KeyType": "RANGE"}
                ],
                "Projection": {"ProjectionType": "ALL"},
                "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
            },
            {
                "IndexName": "ActionTypeIndex", 
                "KeySchema": [
                    {"AttributeName": "actionType", "KeyType": "HASH"},
                    {"AttributeName": "timestamp", "KeyType": "RANGE"}
                ],
                "Projection": {"ProjectionType": "ALL"},
                "ProvisionedThroughput": {"ReadCapacityUnits": 5, "WriteCapacityUnits": 5}
            },
            {
                "IndexName": "EntityIndex",
                "KeySchema": [
                    {"AttributeName": "entityType", "KeyType": "HASH"},
                    {"AttributeName": "entityId", "KeyType": "RANGE"}
                ],
                "Projection": {"ProjectionType": "ALL"},
                "ProvisionedThroughput": {"ReadCapacityUnits": 3, "WriteCapacityUnits": 3}
            }
        ]' \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=10 \
    --region us-east-1
```

### Verify Table Creation
```bash
aws dynamodb describe-table --table-name GCA_ActivityLogs --region us-east-1
```

## Sample Data Structure

```json
{
  "logId": "LOG_abc123def456",
  "timestamp": "2025-01-29T10:30:00.123Z",
  "userId": "USER_coach123",
  "actionType": "CREATE_STUDENT",
  "userType": "COACH",
  "userName": "John Coach",
  "description": "Created new student: Alice Student",
  "entityType": "USER",
  "entityId": "USER_student456",
  "entityName": "Alice Student",
  "metadata": null,
  "result": "SUCCESS",
  "errorMessage": null,
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "sessionId": "session_xyz789"
}
```

## Action Types Tracked

### Authentication
- `LOGIN` - User login
- `LOGOUT` - User logout

### User Management
- `CREATE_STUDENT` - New student created
- `UPDATE_STUDENT` - Student details updated
- `DELETE_STUDENT` - Student removed

### Batch Management
- `CREATE_BATCH` - New batch created
- `UPDATE_BATCH` - Batch details updated
- `DELETE_BATCH` - Batch removed

### Enrollment Management
- `ENROLL_STUDENT` - Student enrolled in batch
- `UPDATE_ENROLLMENT` - Enrollment details updated
- `REMOVE_ENROLLMENT` - Student removed from batch
- `BULK_ENROLLMENT` - Multiple students enrolled

### Payment Management
- `PAYMENT_RECEIVED` - Payment recorded
- `PAYMENT_UPDATED` - Payment details updated

### System Actions
- `PROFILE_UPDATE` - User profile updated
- `PASSWORD_CHANGE` - Password changed
- `VIEW_DASHBOARD` - Dashboard accessed
- `VIEW_REPORTS` - Reports viewed
- `SYSTEM_ACTION` - System-generated action

## Usage in Application

The ActivityLogService automatically logs actions when:

1. **User Authentication**: Login/logout events
2. **CRUD Operations**: Create, update, delete operations on entities
3. **Business Actions**: Enrollments, payments, etc.

## Enhanced Query Capabilities

With the improved GSI design:

### 1. **UserIdIndex (userId + timestamp)**
- Query all activities for a user within a time range
- Example: "Show me all activities for user X in the last week"
- DynamoDB Query: `userId = "USER_123" AND timestamp BETWEEN "2025-01-22T00:00:00Z" AND "2025-01-29T23:59:59Z"`

### 2. **ActionTypeIndex (actionType + timestamp)**  
- Query all activities of a specific type within a time range
- Example: "Show me all login attempts today"
- DynamoDB Query: `actionType = "LOGIN" AND timestamp >= "2025-01-29T00:00:00Z"`

### 3. **EntityIndex (entityType + entityId)**
- Query all activities related to a specific entity
- Example: "Show me all activities related to batch BATCH_123"
- DynamoDB Query: `entityType = "BATCH" AND entityId = "BATCH_123"`

## Capacity Planning

### **On-Demand (Recommended)**
- **Best for**: Unpredictable logging workloads, development environments
- **Pros**: No capacity planning, automatic scaling, pay for what you use
- **Cons**: Higher cost per request compared to optimized provisioned capacity

### **Provisioned Capacity**
- **Best for**: Predictable and consistent high-throughput logging
- **Pros**: Lower cost per request when optimized
- **Cons**: Requires careful capacity planning, risk of throttling

## Querying Examples

### Get Recent Activities for a User
```java
List<ActivityLogDTO> activities = activityLogService.getRecentActivitiesByUser(userId, 10);
```

### Get All Recent Activities (Coach view)
```java
PageResponseDTO<ActivityLogDTO> activities = activityLogService.getRecentActivities(0, 20);
```

### Get Activities by Action Type
```java
List<ActivityLogDTO> logins = activityLogService.getActivitiesByActionType(ActionType.LOGIN, 50);
```

### Time-based Queries (with enhanced GSIs)
```java
// Get user activities in the last 24 hours
LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
List<ActivityLogDTO> recentUserActivities = activityLogService.getActivitiesByDateRange(yesterday, LocalDateTime.now(), 0, 50);

// Get all login activities today
List<ActivityLogDTO> todayLogins = activityLogService.getActivitiesByActionType(ActionType.LOGIN, 100);

// Get all activities for a specific entity
List<ActivityLogDTO> entityActivities = activityLogService.getActivitiesByEntity(EntityType.BATCH, "BATCH_123", 20);
```

## Frontend Integration

The activity logs are displayed in:

1. **Dashboard**: Recent activities section
2. **Student Details**: Student-specific activity history
3. **Admin Panel**: System-wide activity monitoring

## Security and Privacy

- Only coaches can view system-wide activities
- Students can only view their own activities
- Sensitive data is not logged in activity descriptions
- IP addresses and user agents are logged for security auditing 