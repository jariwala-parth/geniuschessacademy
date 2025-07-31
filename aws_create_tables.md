```bash

aws dynamodb create-table \
    --table-name GCA_Organizations \
    --attribute-definitions \
        AttributeName=organizationId,AttributeType=S \
        AttributeName=ownerId,AttributeType=S \
    --key-schema \
        AttributeName=organizationId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=ownerId-index,KeySchema=[{AttributeName=ownerId,KeyType=HASH}],Projection={ProjectionType=ALL}' \
    --tags Key=Project,Value=GeniusChessAcademy
    
    
aws dynamodb create-table \
    --table-name GCA_Users \
    --attribute-definitions \
        AttributeName=organizationId,AttributeType=S \
        AttributeName=userId,AttributeType=S \
        AttributeName=username,AttributeType=S \
        AttributeName=email,AttributeType=S \
        AttributeName=cognitoSub,AttributeType=S \
    --key-schema \
        AttributeName=organizationId,KeyType=HASH \
        AttributeName=userId,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=username-index,KeySchema=[{AttributeName=username,KeyType=HASH}],Projection={ProjectionType=ALL}' \
        'IndexName=email-index,KeySchema=[{AttributeName=email,KeyType=HASH}],Projection={ProjectionType=ALL}' \
        'IndexName=cognitoSub-index,KeySchema=[{AttributeName=cognitoSub,KeyType=HASH}],Projection={ProjectionType=ALL}' \
    --tags Key=Project,Value=GeniusChessAcademy
    

aws dynamodb create-table \
    --table-name GCA_Batches \
    --attribute-definitions \
        AttributeName=organizationId,AttributeType=S \
        AttributeName=batchId,AttributeType=S \
        AttributeName=batchStatus,AttributeType=S \
        AttributeName=startDate,AttributeType=S \
        AttributeName=coachId,AttributeType=S \
    --key-schema \
        AttributeName=organizationId,KeyType=HASH \
        AttributeName=batchId,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=batchStatus-startDate-index,KeySchema=[{AttributeName=batchStatus,KeyType=HASH},{AttributeName=startDate,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
        'IndexName=coachId-index,KeySchema=[{AttributeName=coachId,KeyType=HASH},{AttributeName=organizationId,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
    --tags Key=Project,Value=GeniusChessAcademy
    
aws dynamodb create-table \
    --table-name GCA_Sessions \
    --attribute-definitions \
        AttributeName=organizationId,AttributeType=S \
        AttributeName=sessionId,AttributeType=S \
        AttributeName=sessionDate,AttributeType=S \
        AttributeName=coachId,AttributeType=S \
    --key-schema \
        AttributeName=organizationId,KeyType=HASH \
        AttributeName=sessionId,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=sessionDate-batchId-index,KeySchema=[{AttributeName=organizationId,KeyType=HASH},{AttributeName=sessionDate,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
        'IndexName=coachId-sessionDate-index,KeySchema=[{AttributeName=organizationId,KeyType=HASH},{AttributeName=coachId,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
    --tags Key=Project,Value=GeniusChessAcademy
    
aws dynamodb create-table \
    --table-name GCA_Enrollments \
    --attribute-definitions \
        AttributeName=organizationId,AttributeType=S \
        AttributeName=enrollmentId,AttributeType=S \
        AttributeName=studentId,AttributeType=S \
    --key-schema \
        AttributeName=organizationId,KeyType=HASH \
        AttributeName=enrollmentId,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=studentId-index,KeySchema=[{AttributeName=organizationId,KeyType=HASH},{AttributeName=studentId,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
    --tags Key=Project,Value=GeniusChessAcademy
    
aws dynamodb create-table \
    --table-name GCA_Attendance \
    --attribute-definitions \
        AttributeName=organizationId,AttributeType=S \
        AttributeName=sessionId,AttributeType=S \
        AttributeName=studentId,AttributeType=S \
    --key-schema \
        AttributeName=organizationId,KeyType=HASH \
        AttributeName=sessionId,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=studentId-index,KeySchema=[{AttributeName=organizationId,KeyType=HASH},{AttributeName=studentId,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
    --tags Key=Project,Value=GeniusChessAcademy
    
aws dynamodb create-table \
    --table-name GCA_ActivityLogs \
    --attribute-definitions \
        AttributeName=organizationId,AttributeType=S \
        AttributeName=activityId,AttributeType=S \
        AttributeName=userId,AttributeType=S \
    --key-schema \
        AttributeName=organizationId,KeyType=HASH \
        AttributeName=activityId,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=userId-index,KeySchema=[{AttributeName=organizationId,KeyType=HASH},{AttributeName=userId,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
    --tags Key=Project,Value=GeniusChessAcademy

    
aws dynamodb create-table \
    --table-name GCA_Invoices \
    --attribute-definitions \
        AttributeName=organizationId,AttributeType=S \
        AttributeName=invoiceId,AttributeType=S \
        AttributeName=status,AttributeType=S \
        AttributeName=dueDate,AttributeType=S \
    --key-schema \
        AttributeName=organizationId,KeyType=HASH \
        AttributeName=invoiceId,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        'IndexName=status-dueDate-index,KeySchema=[{AttributeName=status,KeyType=HASH},{AttributeName=dueDate,KeyType=RANGE}],Projection={ProjectionType=ALL}' \
    --tags Key=Project,Value=GeniusChessAcademy
```

Add superadmin
```bash
aws dynamodb put-item \
    --table-name GCA_Users \
    --item '{
        "organizationId": {"S": "system"},
        "userId": {"S": "gca_superadmin"},
        "userType": {"S": "SUPER_ADMIN"},
        "username": {"S": "gca_superadmin"},
        "email": {"S": "parth.jariwala13@gmail.com"},
        "name": {"S": "Super Admin"},
        "cognitoSub": {"S": "0163fdea-5051-7050-b6fd-eda6eaa08f99"},
        "isActive": {"BOOL": true},
        "createdAt": {"S": "2025-07-30T12:00:00.000"},
        "updatedAt": {"S": "2025-07-30T12:00:00.000"}
    }' \
    --return-consumed-capacity TOTAL \
    --region ap-south-1
```