# DynamoDB Enrollments Table Creation Commands

## Create GCA_Enrollments Table

### AWS CLI Command

```bash
aws dynamodb create-table \
    --table-name GCA_Enrollments \
    --attribute-definitions \
        AttributeName=batchId,AttributeType=S \
        AttributeName=studentId,AttributeType=S \
    --key-schema \
        AttributeName=batchId,KeyType=HASH \
        AttributeName=studentId,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=studentId-batchId-index,KeySchema=[{AttributeName=studentId,KeyType=HASH},{AttributeName=batchId,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
    --tags \
        Key=Project,Value=GeniusChessAcademy
```

### Alternative JSON Format

```bash
aws dynamodb create-table \
  --cli-input-json '{
    "TableName": "GCA_Enrollments",
    "KeySchema": [
      {
        "AttributeName": "batchId",
        "KeyType": "HASH"
      },
      {
        "AttributeName": "studentId",
        "KeyType": "RANGE"
      }
    ],
    "AttributeDefinitions": [
      {
        "AttributeName": "batchId",
        "AttributeType": "S"
      },
      {
        "AttributeName": "studentId",
        "AttributeType": "S"
      }
    ],
    "GlobalSecondaryIndexes": [
      {
        "IndexName": "studentId-batchId-index",
        "KeySchema": [
          {
            "AttributeName": "studentId",
            "KeyType": "HASH"
          },
          {
            "AttributeName": "batchId",
            "KeyType": "RANGE"
          }
        ],
        "Projection": {
          "ProjectionType": "ALL"
        },
        "BillingMode": "PAY_PER_REQUEST"
      }
    ],
    "BillingMode": "PAY_PER_REQUEST"
  }' \
  --region ap-south-1
```

## Table Structure

### Primary Key (Composite)
- **Partition Key (Hash)**: `batchId` (String) - Batch identifier
- **Sort Key (Range)**: `studentId` (String) - Student identifier

This composite key uniquely identifies each enrollment record.

### Global Secondary Index (GSI)
1. **studentId-batchId-index**: 
   - Partition Key: `studentId` (String)
   - Sort Key: `batchId` (String)
   - Allows efficient queries for all batches a specific student is enrolled in

### All Attributes (Stored but not indexed)
- `batchId` (String) - Primary hash key, references GCA_Batches table
- `studentId` (String) - Primary range key, references GCA_Users table
- `enrollmentDate` (String) - ISO date when student enrolled
- `enrollmentStatus` (String) - "ENROLLED", "DROPPED", "COMPLETED"
- `paymentStatus` (String) - "PENDING", "PAID", "PARTIALLY_PAID", "OVERDUE", "REFUNDED"
- `currentPaymentAmount` (Number) - Amount paid so far for this enrollment
- `notes` (String) - Additional notes about the enrollment
- `createdAt` (String) - ISO timestamp when enrollment was created
- `updatedAt` (String) - ISO timestamp when enrollment was last updated

## Verify Table Creation

```bash
# Check if table exists
aws dynamodb describe-table --table-name GCA_Enrollments --region ap-south-1

# List all tables
aws dynamodb list-tables --region ap-south-1

# Check table status
aws dynamodb describe-table --table-name GCA_Enrollments --region ap-south-1 --query 'Table.TableStatus'
```

## Sample Data Insertion

```bash
# Insert a sample enrollment
aws dynamodb put-item \
  --table-name GCA_Enrollments \
  --item '{
    "batchId": {"S": "BATCH_12345abcd"},
    "studentId": {"S": "USER_student123"},
    "enrollmentDate": {"S": "2025-01-15"},
    "enrollmentStatus": {"S": "ENROLLED"},
    "paymentStatus": {"S": "PENDING"},
    "currentPaymentAmount": {"N": "0.0"},
    "notes": {"S": "New student enrollment"},
    "createdAt": {"S": "2025-01-27T19:00:00"},
    "updatedAt": {"S": "2025-01-27T19:00:00"}
  }' \
  --region ap-south-1

# Insert another enrollment with payment
aws dynamodb put-item \
  --table-name GCA_Enrollments \
  --item '{
    "batchId": {"S": "BATCH_67890efgh"},
    "studentId": {"S": "USER_student123"},
    "enrollmentDate": {"S": "2025-01-10"},
    "enrollmentStatus": {"S": "ENROLLED"},
    "paymentStatus": {"S": "PAID"},
    "currentPaymentAmount": {"N": "1500.0"},
    "notes": {"S": "Full payment received"},
    "createdAt": {"S": "2025-01-27T19:00:00"},
    "updatedAt": {"S": "2025-01-27T19:00:00"}
  }' \
  --region ap-south-1
```

## Query Examples

```bash
# Get all students enrolled in a specific batch
aws dynamodb query \
  --table-name GCA_Enrollments \
  --key-condition-expression "batchId = :batchId" \
  --expression-attribute-values '{
    ":batchId": {"S": "BATCH_12345abcd"}
  }' \
  --region ap-south-1

# Get all batches a specific student is enrolled in
aws dynamodb query \
  --table-name GCA_Enrollments \
  --index-name studentId-batchId-index \
  --key-condition-expression "studentId = :studentId" \
  --expression-attribute-values '{
    ":studentId": {"S": "USER_student123"}
  }' \
  --region ap-south-1

# Get specific enrollment
aws dynamodb get-item \
  --table-name GCA_Enrollments \
  --key '{
    "batchId": {"S": "BATCH_12345abcd"},
    "studentId": {"S": "USER_student123"}
  }' \
  --region ap-south-1

# Get enrollments by payment status (scan with filter)
aws dynamodb scan \
  --table-name GCA_Enrollments \
  --filter-expression "paymentStatus = :status" \
  --expression-attribute-values '{
    ":status": {"S": "PENDING"}
  }' \
  --region ap-south-1

# Get enrollments by enrollment status
aws dynamodb scan \
  --table-name GCA_Enrollments \
  --filter-expression "enrollmentStatus = :status" \
  --expression-attribute-values '{
    ":status": {"S": "ENROLLED"}
  }' \
  --region ap-south-1
```

## Update Examples

```bash
# Update payment status and amount
aws dynamodb update-item \
  --table-name GCA_Enrollments \
  --key '{
    "batchId": {"S": "BATCH_12345abcd"},
    "studentId": {"S": "USER_student123"}
  }' \
  --update-expression "SET paymentStatus = :status, currentPaymentAmount = :amount, updatedAt = :updated" \
  --expression-attribute-values '{
    ":status": {"S": "PAID"},
    ":amount": {"N": "1500.0"},
    ":updated": {"S": "2025-01-27T20:00:00"}
  }' \
  --region ap-south-1

# Update enrollment status
aws dynamodb update-item \
  --table-name GCA_Enrollments \
  --key '{
    "batchId": {"S": "BATCH_12345abcd"},
    "studentId": {"S": "USER_student123"}
  }' \
  --update-expression "SET enrollmentStatus = :status, updatedAt = :updated" \
  --expression-attribute-values '{
    ":status": {"S": "COMPLETED"},
    ":updated": {"S": "2025-01-27T20:00:00"}
  }' \
  --region ap-south-1
```

## Delete Examples

```bash
# Delete an enrollment (unenroll student)
aws dynamodb delete-item \
  --table-name GCA_Enrollments \
  --key '{
    "batchId": {"S": "BATCH_12345abcd"},
    "studentId": {"S": "USER_student123"}
  }' \
  --region ap-south-1
```

## Use Cases and Query Patterns

### 1. Enrollment Management
- **Create Enrollment**: Direct PUT operation
- **Check if Student is Enrolled**: GET with composite key
- **List Students in Batch**: Query by `batchId`
- **List Student's Batches**: Query GSI by `studentId`

### 2. Payment Tracking
- **Find Pending Payments**: Scan with filter on `paymentStatus = PENDING`
- **Update Payment Status**: UPDATE operation
- **Payment History**: Query by student and sort by enrollment date

### 3. Analytics
- **Batch Capacity**: Count enrollments per batch
- **Student Activity**: Track enrollment history per student
- **Payment Reports**: Aggregate by payment status and dates

## Performance Considerations

### Efficient Queries
- Use primary key for single enrollment lookups
- Use GSI for student-centric queries
- Batch operations when possible

### Avoid Expensive Operations
- Minimize full table scans
- Use filter expressions on query results rather than scan operations
- Consider adding more GSIs if additional query patterns emerge

## Delete Table (if needed)

```bash
aws dynamodb delete-table --table-name GCA_Enrollments --region ap-south-1
```

## Integration with Other Tables

### Referential Integrity Considerations
- Ensure `batchId` exists in `GCA_Batches` table
- Ensure `studentId` exists in `GCA_Users` table with `userType = STUDENT`
- Consider batch capacity limits when creating enrollments
- Update batch `currentStudents` count when enrollments change

### Cascade Operations
- When a batch is deleted/cancelled, consider what happens to enrollments
- When a student is deactivated, consider enrollment status updates
- Payment updates should maintain consistency with external payment systems 