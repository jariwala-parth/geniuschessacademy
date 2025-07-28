# DynamoDB Table Creation Commands

## Create GCA_Users Table

### AWS CLI Command

```bash
aws dynamodb create-table \
  --table-name GCA_Users \
  --attribute-definitions \
    AttributeName=userId,AttributeType=S \
    AttributeName=userType,AttributeType=S \
    AttributeName=username,AttributeType=S \
    AttributeName=email,AttributeType=S \
    AttributeName=cognitoSub,AttributeType=S \
  --key-schema \
    AttributeName=userId,KeyType=HASH \
    AttributeName=userType,KeyType=RANGE \
  --global-secondary-indexes \
    IndexName=username-index,KeySchema='[{AttributeName=username,KeyType=HASH}]',Projection='{ProjectionType=ALL}',BillingMode=PAY_PER_REQUEST \
    IndexName=email-index,KeySchema='[{AttributeName=email,KeyType=HASH}]',Projection='{ProjectionType=ALL}',BillingMode=PAY_PER_REQUEST \
    IndexName=cognitoSub-index,KeySchema='[{AttributeName=cognitoSub,KeyType=HASH}]',Projection='{ProjectionType=ALL}',BillingMode=PAY_PER_REQUEST \
  --billing-mode PAY_PER_REQUEST \
  --region ap-south-1
```

### Alternative JSON Format

```bash
aws dynamodb create-table \
  --cli-input-json '{
    "TableName": "GCA_Users",
    "KeySchema": [
      {
        "AttributeName": "userId",
        "KeyType": "HASH"
      },
      {
        "AttributeName": "userType", 
        "KeyType": "RANGE"
      }
    ],
    "AttributeDefinitions": [
      {
        "AttributeName": "userId",
        "AttributeType": "S"
      },
      {
        "AttributeName": "userType",
        "AttributeType": "S"
      },
      {
        "AttributeName": "username",
        "AttributeType": "S"
      },
      {
        "AttributeName": "email",
        "AttributeType": "S"
      },
      {
        "AttributeName": "cognitoSub",
        "AttributeType": "S"
      }
    ],
    "GlobalSecondaryIndexes": [
      {
        "IndexName": "username-index",
        "KeySchema": [
          {
            "AttributeName": "username",
            "KeyType": "HASH"
          }
        ],
        "Projection": {
          "ProjectionType": "ALL"
        },
        "BillingMode": "PAY_PER_REQUEST"
      },
      {
        "IndexName": "email-index",
        "KeySchema": [
          {
            "AttributeName": "email",
            "KeyType": "HASH"
          }
        ],
        "Projection": {
          "ProjectionType": "ALL"
        },
        "BillingMode": "PAY_PER_REQUEST"
      },
      {
        "IndexName": "cognitoSub-index",
        "KeySchema": [
          {
            "AttributeName": "cognitoSub",
            "KeyType": "HASH"
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
- **Partition Key (Hash)**: `userId` (String) - Unique identifier for each user
- **Sort Key (Range)**: `userType` (String) - Either "COACH" or "STUDENT"

### Global Secondary Indexes (GSI)
1. **username-index**: 
   - Partition Key: `username` (String)
   - Allows login via username
   
2. **email-index**: 
   - Partition Key: `email` (String)
   - Allows login via email
   
3. **cognitoSub-index**: 
   - Partition Key: `cognitoSub` (String)
   - Links to Cognito User Pool

### All Attributes (Stored but not indexed)
- `userId` (String) - Primary hash key
- `userType` (String) - Primary range key: "COACH" or "STUDENT"
- `username` (String) - User-chosen username (indexed)
- `email` (String) - User email (indexed)
- `name` (String) - Full name
- `phoneNumber` (String) - Phone number
- `cognitoSub` (String) - Cognito User Pool Sub ID (indexed)
- `isActive` (Boolean) - Account status
- `createdAt` (String) - ISO timestamp when user was created
- `updatedAt` (String) - ISO timestamp when user was last updated

#### Student-specific fields:
- `guardianName` (String) - Guardian's name
- `guardianPhone` (String) - Guardian's phone number  
- `joiningDate` (String) - ISO timestamp when student joined

#### Coach-specific fields:
- `isAdmin` (Boolean) - Whether coach has admin privileges

## Verify Table Creation

```bash
# Check if table exists
aws dynamodb describe-table --table-name GCA_Users --region ap-south-1

# List all tables
aws dynamodb list-tables --region ap-south-1

# Check table status
aws dynamodb describe-table --table-name GCA_Users --region ap-south-1 --query 'Table.TableStatus'
```

## Sample Data Insertion

```bash
# Insert a sample coach
aws dynamodb put-item \
  --table-name GCA_Users \
  --item '{
    "userId": {"S": "USER_12345"},
    "userType": {"S": "COACH"},
    "username": {"S": "admin_coach"},
    "email": {"S": "admin@chessacademy.com"},
    "name": {"S": "Admin Coach"},
    "phoneNumber": {"S": "+1234567890"},
    "cognitoSub": {"S": "cognito-sub-12345"},
    "isActive": {"BOOL": true},
    "isAdmin": {"BOOL": true},
    "createdAt": {"S": "2025-01-27T19:00:00Z"},
    "updatedAt": {"S": "2025-01-27T19:00:00Z"}
  }' \
  --region ap-south-1

# Insert a sample student
aws dynamodb put-item \
  --table-name GCA_Users \
  --item '{
    "userId": {"S": "USER_67890"},
    "userType": {"S": "STUDENT"},
    "username": {"S": "student1"},
    "email": {"S": "student@chessacademy.com"},
    "name": {"S": "Student One"},
    "phoneNumber": {"S": "+1234567891"},
    "cognitoSub": {"S": "cognito-sub-67890"},
    "isActive": {"BOOL": true},
    "guardianName": {"S": "Parent Name"},
    "guardianPhone": {"S": "+1234567892"},
    "joiningDate": {"S": "2025-01-27T19:00:00Z"},
    "createdAt": {"S": "2025-01-27T19:00:00Z"},
    "updatedAt": {"S": "2025-01-27T19:00:00Z"}
  }' \
  --region ap-south-1
```

## Query Examples

```bash
# Find user by username
aws dynamodb query \
  --table-name GCA_Users \
  --index-name username-index \
  --key-condition-expression "username = :username" \
  --expression-attribute-values '{
    ":username": {"S": "admin_coach"}
  }' \
  --region ap-south-1

# Find user by email
aws dynamodb query \
  --table-name GCA_Users \
  --index-name email-index \
  --key-condition-expression "email = :email" \
  --expression-attribute-values '{
    ":email": {"S": "admin@chessacademy.com"}
  }' \
  --region ap-south-1

# Get all coaches
aws dynamodb scan \
  --table-name GCA_Users \
  --filter-expression "userType = :userType" \
  --expression-attribute-values '{
    ":userType": {"S": "COACH"}
  }' \
  --region ap-south-1
```

## Delete Table (if needed)

```bash
aws dynamodb delete-table --table-name GCA_Users --region ap-south-1
``` 