# DynamoDB Batches Table Creation Commands

## Create GCA_Batches Table

### AWS CLI Command

```bash
aws dynamodb create-table \
    --table-name GCA_Batches \
    --attribute-definitions \
        AttributeName=batchId,AttributeType=S \
        AttributeName=batchStatus,AttributeType=S \
        AttributeName=coachId,AttributeType=S \
        AttributeName=startDate,AttributeType=S \
    --key-schema \
        AttributeName=batchId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=batchStatus-startDate-index,KeySchema=[{AttributeName=batchStatus,KeyType=HASH},{AttributeName=startDate,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
        'IndexName=coachId-index,KeySchema=[{AttributeName=coachId,KeyType=HASH}],Projection={ProjectionType=ALL}' \
    --tags \
        Key=Project,Value=GeniusChessAcademy \
        Key=Environment,Value=development
```

### Alternative JSON Format

```bash
aws dynamodb create-table \
  --cli-input-json '{
    "TableName": "GCA_Batches",
    "KeySchema": [
      {
        "AttributeName": "batchId",
        "KeyType": "HASH"
      }
    ],
    "AttributeDefinitions": [
      {
        "AttributeName": "batchId",
        "AttributeType": "S"
      },
      {
        "AttributeName": "batchStatus",
        "AttributeType": "S"
      },
      {
        "AttributeName": "coachId",
        "AttributeType": "S"
      },
      {
        "AttributeName": "startDate",
        "AttributeType": "S"
      }
    ],
    "GlobalSecondaryIndexes": [
      {
        "IndexName": "coachId-index",
        "KeySchema": [
          {
            "AttributeName": "coachId",
            "KeyType": "HASH"
          }
        ],
        "Projection": {
          "ProjectionType": "ALL"
        },
        "BillingMode": "PAY_PER_REQUEST"
      },
      {
        "IndexName": "batchStatus-startDate-index",
        "KeySchema": [
          {
            "AttributeName": "batchStatus",
            "KeyType": "HASH"
          },
          {
            "AttributeName": "startDate",
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

### Primary Key
- **Partition Key (Hash)**: `batchId` (String) - Unique identifier for each batch

### Global Secondary Indexes (GSI)
1. **coachId-index**: 
   - Partition Key: `coachId` (String)
   - Allows querying batches by coach
   
2. **batchStatus-startDate-index**: 
   - Partition Key: `batchStatus` (String)
   - Sort Key: `startDate` (String)
   - Allows querying batches by status and sorting by start date

### All Attributes (Stored but not indexed)
- `batchId` (String) - Primary hash key
- `batchName` (String) - Name of the batch
- `batchSize` (Integer) - Maximum capacity
- `currentStudents` (Integer) - Current enrollment count
- `startDate` (String) - ISO date when batch starts
- `endDate` (String) - ISO date when batch ends
- `batchTiming` (Map) - Nested object with timing details:
  - `daysOfWeek` (List<String>) - Days when batch occurs
  - `startTime` (String) - Start time in ISO format
  - `endTime` (String) - End time in ISO format
- `paymentType` (String) - "ONE_TIME" or "MONTHLY"
- `monthlyFee` (Number) - Fee for monthly payment type
- `oneTimeFee` (Number) - Fee for one-time payment type
- `occurrenceType` (String) - "REGULAR", "WEEKLY", or "DAILY"
- `batchStatus` (String) - "UPCOMING", "ACTIVE", "FULL", "COMPLETED", "CANCELLED"
- `notes` (String) - Additional notes about the batch
- `coachId` (String) - ID of the assigned coach (indexed)
- `createdAt` (String) - ISO timestamp when batch was created
- `updatedAt` (String) - ISO timestamp when batch was last updated

## Verify Table Creation

```bash
# Check if table exists
aws dynamodb describe-table --table-name GCA_Batches --region ap-south-1

# List all tables
aws dynamodb list-tables --region ap-south-1

# Check table status
aws dynamodb describe-table --table-name GCA_Batches --region ap-south-1 --query 'Table.TableStatus'
```

## Sample Data Insertion

```bash
# Insert a sample batch
aws dynamodb put-item \
  --table-name GCA_Batches \
  --item '{
    "batchId": {"S": "BATCH_12345abcd"},
    "batchName": {"S": "Beginner Chess - Evening"},
    "batchSize": {"N": "15"},
    "currentStudents": {"N": "0"},
    "startDate": {"S": "2025-02-01"},
    "endDate": {"S": "2025-04-30"},
    "batchTiming": {
      "M": {
        "daysOfWeek": {"L": [{"S": "MONDAY"}, {"S": "WEDNESDAY"}, {"S": "FRIDAY"}]},
        "startTime": {"S": "18:00:00"},
        "endTime": {"S": "19:30:00"}
      }
    },
    "paymentType": {"S": "MONTHLY"},
    "monthlyFee": {"N": "1500.00"},
    "occurrenceType": {"S": "REGULAR"},
    "batchStatus": {"S": "UPCOMING"},
    "notes": {"S": "Focus on fundamentals and basic tactics"},
    "coachId": {"S": "USER_coach123"},
    "createdAt": {"S": "2025-01-27T19:00:00"},
    "updatedAt": {"S": "2025-01-27T19:00:00"}
  }' \
  --region ap-south-1
```

## Query Examples

```bash
# Find batches by coach
aws dynamodb query \
  --table-name GCA_Batches \
  --index-name coachId-index \
  --key-condition-expression "coachId = :coachId" \
  --expression-attribute-values '{
    ":coachId": {"S": "USER_coach123"}
  }' \
  --region ap-south-1

# Find batches by status
aws dynamodb query \
  --table-name GCA_Batches \
  --index-name batchStatus-startDate-index \
  --key-condition-expression "batchStatus = :status" \
  --expression-attribute-values '{
    ":status": {"S": "ACTIVE"}
  }' \
  --region ap-south-1

# Get all batches (scan)
aws dynamodb scan \
  --table-name GCA_Batches \
  --region ap-south-1
```

## Delete Table (if needed)

```bash
aws dynamodb delete-table --table-name GCA_Batches --region ap-south-1
``` 