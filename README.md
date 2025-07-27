# Genius Chess Academy - Backend API

A serverless Spring Boot application for managing a chess academy, built with AWS Lambda, AWS Cognito, and DynamoDB.

## Phase 1: Authentication APIs ✅ COMPLETE

This phase implements a complete authentication system using AWS Cognito with real JWT tokens for coaches and students.

### Features

- **AWS Cognito Integration**: Full integration with AWS Cognito User Pools
- **Real JWT Tokens**: Proper JWT token generation, validation, and refresh
- **User Registration**: Sign up as a coach or student with custom attributes
- **User Authentication**: Login with email and password (no social logins)
- **Token Management**: JWT-based authentication with refresh tokens
- **Password Management**: Change password, forgot password, reset password with confirmation codes
- **User Logout**: Global logout that invalidates all user tokens
- **Comprehensive Error Handling**: Detailed error responses with proper HTTP status codes
- **JWT Utilities**: Token validation and claim extraction utilities
- **DynamoDB Integration**: Complete DynamoDB models with proper annotations
- **Lombok Integration**: Clean DTOs with @Data annotations
- **Serverless Optimized**: Ready for AWS Lambda deployment

### Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   API Gateway   │    │   Lambda        │
│   (React/Next)  │───▶│   + Cognito     │───▶│   (Spring Boot) │
│                 │    │   Authorizer    │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                ↓                       │
                       ┌─────────────────┐              ▼
                       │  AWS Cognito    │     ┌─────────────────┐
                       │  User Pool      │     │   DynamoDB      │
                       │  (JWT Tokens)   │     │   (Users Table) │
                       └─────────────────┘     └─────────────────┘
```

### API Endpoints

#### Base URL (Local Development)
```
http://localhost:3000/api/v1/auth
```

#### Base URL (AWS Lambda Local - SAM)
```
http://127.0.0.1:3000/{proxy+}
```

#### Base URL (Production)
```
https://your-api-gateway-url/api/v1/auth
```

### Development Setup

#### Prerequisites
- Java 17
- Maven 3.6+
- AWS CLI configured
- AWS SAM CLI installed
- AWS Cognito User Pool and App Client

#### Local Development (Spring Boot)

1. **Clone and setup:**
   ```bash
   cd gca_be/GeniusChessAcademy
   ```

2. **Configure environment variables:**
   ```bash
   # Add to application.properties or set as environment variables
   aws.cognito.userPoolId=us-east-1_XXXXXXXXX
   aws.cognito.clientId=your-client-id
   aws.cognito.clientSecret=your-client-secret
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

#### AWS Lambda Local Testing (SAM CLI)

1. **Build the application:**
   ```bash
   cd gca_be/GeniusChessAcademy
   mvn clean package -P assembly-zip
   ```

2. **Start SAM local API:**
   ```bash
   sam local start-api --port 3000
   ```

3. **Test the endpoints:**
   ```bash
   # Test ping endpoint
   curl http://127.0.0.1:3000/ping
   ```

### API Testing Commands

#### 1. User Registration (Coach)
```bash
# Local Spring Boot
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe_coach",
    "email": "coach@chessacademy.com",
    "password": "SecurePass123!",
    "name": "John Doe",
    "phoneNumber": "+1234567890",
    "userType": "COACH",
    "isAdmin": true
  }'

# SAM Local
curl -X POST http://127.0.0.1:3000/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe_coach",
    "email": "coach@chessacademy.com",
    "password": "SecurePass123!",
    "name": "John Doe",
    "phoneNumber": "+1234567890",
    "userType": "COACH",
    "isAdmin": true
  }'
```

#### 2. User Registration (Student)
```bash
# Local Spring Boot
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alicesmith_student",
    "email": "student@chessacademy.com",
    "password": "SecurePass123!",
    "name": "Alice Smith",
    "phoneNumber": "+1234567890",
    "userType": "STUDENT",
    "guardianName": "Bob Smith",
    "guardianPhone": "+1234567891"
  }'

# SAM Local
curl -X POST http://127.0.0.1:3000/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alicesmith_student",
    "email": "student@chessacademy.com",
    "password": "SecurePass123!",
    "name": "Alice Smith",
    "phoneNumber": "+1234567890",
    "userType": "STUDENT",
    "guardianName": "Bob Smith",
    "guardianPhone": "+1234567891"
  }'
```

#### 3. User Login
```bash
# Login with email
# Local Spring Boot
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "coach@chessacademy.com",
    "password": "SecurePass123!",
    "userType": "COACH"
  }'

# Login with username
# SAM Local
curl -X POST http://127.0.0.1:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "johndoe_coach",
    "password": "SecurePass123!",
    "userType": "COACH"
  }'
```

#### 4. Refresh Token
```bash
# Local Spring Boot
curl -X POST "http://localhost:8080/api/v1/auth/refresh?refreshToken=YOUR_REFRESH_TOKEN"

# SAM Local
curl -X POST "http://127.0.0.1:3000/api/v1/auth/refresh?refreshToken=YOUR_REFRESH_TOKEN"
```

#### 5. User Logout
```bash
# Local Spring Boot
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# SAM Local
curl -X POST http://127.0.0.1:3000/api/v1/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 6. Change Password
```bash
# Local Spring Boot
curl -X POST "http://localhost:8080/api/v1/auth/change-password?oldPassword=SecurePass123!&newPassword=NewSecurePass123!" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# SAM Local
curl -X POST "http://127.0.0.1:3000/api/v1/auth/change-password?oldPassword=SecurePass123!&newPassword=NewSecurePass123!" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### 7. Forgot Password
```bash
# Local Spring Boot
curl -X POST "http://localhost:8080/api/v1/auth/forgot-password?email=coach@chessacademy.com"

# SAM Local
curl -X POST "http://127.0.0.1:3000/api/v1/auth/forgot-password?email=coach@chessacademy.com"
```

#### 8. Reset Password
```bash
# Local Spring Boot
curl -X POST "http://localhost:8080/api/v1/auth/reset-password?email=coach@chessacademy.com&confirmationCode=123456&newPassword=NewSecurePass123!"

# SAM Local
curl -X POST "http://127.0.0.1:3000/api/v1/auth/reset-password?email=coach@chessacademy.com&confirmationCode=123456&newPassword=NewSecurePass123!"
```

### Response Examples

#### Successful Authentication Response
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJjdHkiOiJKV1QiLCJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ...",
  "idToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userInfo": {
    "userId": "USER_abc123def456",
    "email": "coach@chessacademy.com",
    "name": "John Doe",
    "userType": "COACH",
    "phoneNumber": "+1234567890"
  }
}
```

#### Error Response
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Invalid email or password",
  "status": 401,
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/v1/auth/login"
}
```

### DynamoDB Table Structure

#### Users Table
```yaml
TableName: GCA_Users
PartitionKey: userId (String)
SortKey: userType (String)

GlobalSecondaryIndexes:
  - IndexName: username-index
    PartitionKey: username (String)
  
  - IndexName: email-index
    PartitionKey: email (String)
  
  - IndexName: cognitoSub-index
    PartitionKey: cognitoSub (String)

Attributes:
  - userId: String (Primary Key)
  - userType: String (COACH | STUDENT)
  - username: String (Unique login identifier)
  - email: String
  - name: String
  - phoneNumber: String
  - cognitoSub: String
  - isActive: Boolean
  - createdAt: String (ISO DateTime)
  - updatedAt: String (ISO DateTime)
  - guardianName: String (Student only)
  - guardianPhone: String (Student only)
  - joiningDate: String (Student only, ISO DateTime)
  - isAdmin: Boolean (Coach only)
```

### AWS Cognito Setup

#### 1. Create User Pool
```bash
aws cognito-idp create-user-pool \
  --pool-name "GeniusChessAcademy" \
  --username-attributes email \
  --auto-verified-attributes email \
  --password-policy MinimumLength=8,RequireUppercase=true,RequireLowercase=true,RequireNumbers=true,RequireSymbols=true \
  --schema '[
    {
      "Name": "email",
      "AttributeDataType": "String",
      "Required": true,
      "Mutable": true
    },
    {
      "Name": "name",
      "AttributeDataType": "String",
      "Required": true,
      "Mutable": true
    },
    {
      "Name": "phone_number",
      "AttributeDataType": "String",
      "Required": false,
      "Mutable": true
    },
    {
      "Name": "user_type",
      "AttributeDataType": "String",
      "Required": false,
      "Mutable": true
    },
    {
      "Name": "guardian_name",
      "AttributeDataType": "String",
      "Required": false,
      "Mutable": true
    },
    {
      "Name": "guardian_phone",
      "AttributeDataType": "String",
      "Required": false,
      "Mutable": true
    },
    {
      "Name": "is_admin",
      "AttributeDataType": "String",
      "Required": false,
      "Mutable": true
    }
  ]'
```

#### 2. Create App Client
```bash
aws cognito-idp create-user-pool-client \
  --user-pool-id us-east-1_XXXXXXXXX \
  --client-name "GeniusChessAcademyClient" \
  --generate-secret \
  --explicit-auth-flows ADMIN_NO_SRP_AUTH ALLOW_REFRESH_TOKEN_AUTH \
  --token-validity-units AccessToken=hours,IdToken=hours,RefreshToken=days \
  --access-token-validity 1 \
  --id-token-validity 1 \
  --refresh-token-validity 30
```

#### 3. Create DynamoDB Table
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

### Environment Variables

```bash
# AWS Cognito Configuration
COGNITO_USER_POOL_ID=us-east-1_XXXXXXXXX
COGNITO_CLIENT_ID=your-client-id
COGNITO_CLIENT_SECRET=your-client-secret
AWS_REGION=us-east-1

# DynamoDB Configuration
DYNAMODB_TABLE_NAME=Users

# For local development only
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
```

### SAM Template (template.yml)

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: Genius Chess Academy API

Globals:
  Function:
    Timeout: 30
    MemorySize: 512
    Environment:
      Variables:
        COGNITO_USER_POOL_ID: !Ref CognitoUserPool
        COGNITO_CLIENT_ID: !Ref CognitoUserPoolClient
        COGNITO_CLIENT_SECRET: !GetAtt CognitoUserPoolClient.ClientSecret
        AWS_REGION: !Ref AWS::Region

Resources:
  GeniusChessAcademyFunction:
    Type: AWS::Serverless::Function
    Properties:
      CodeUri: target/GeniusChessAcademy-1.0-SNAPSHOT-lambda-package.zip
      Handler: com.pjariwala.StreamLambdaHandler::handleRequest
      Runtime: java17
      Events:
        ProxyResource:
          Type: Api
          Properties:
            Path: /{proxy+}
            Method: ANY
            RestApiId: !Ref GeniusChessAcademyApi

  GeniusChessAcademyApi:
    Type: AWS::Serverless::Api
    Properties:
      StageName: Prod
      Cors:
        AllowMethods: "'*'"
        AllowHeaders: "'*'"
        AllowOrigin: "'*'"

  CognitoUserPool:
    Type: AWS::Cognito::UserPool
    Properties:
      UserPoolName: GeniusChessAcademy
      UsernameAttributes:
        - email
      AutoVerifiedAttributes:
        - email
      PasswordPolicy:
        MinimumLength: 8
        RequireUppercase: true
        RequireLowercase: true
        RequireNumbers: true
        RequireSymbols: true

  CognitoUserPoolClient:
    Type: AWS::Cognito::UserPoolClient
    Properties:
      UserPoolId: !Ref CognitoUserPool
      ClientName: GeniusChessAcademyClient
      GenerateSecret: true
      ExplicitAuthFlows:
        - ADMIN_NO_SRP_AUTH
        - ALLOW_REFRESH_TOKEN_AUTH

  UsersTable:
    Type: AWS::DynamoDB::Table
    Properties:
      TableName: Users
      AttributeDefinitions:
        - AttributeName: userId
          AttributeType: S
        - AttributeName: userType
          AttributeType: S
        - AttributeName: email
          AttributeType: S
        - AttributeName: cognitoSub
          AttributeType: S
      KeySchema:
        - AttributeName: userId
          KeyType: HASH
        - AttributeName: userType
          KeyType: RANGE
      GlobalSecondaryIndexes:
        - IndexName: email-index
          KeySchema:
            - AttributeName: email
              KeyType: HASH
          Projection:
            ProjectionType: ALL
          BillingMode: PAY_PER_REQUEST
        - IndexName: cognitoSub-index
          KeySchema:
            - AttributeName: cognitoSub
              KeyType: HASH
          Projection:
            ProjectionType: ALL
          BillingMode: PAY_PER_REQUEST
      BillingMode: PAY_PER_REQUEST

Outputs:
  GeniusChessAcademyApi:
    Description: "API Gateway endpoint URL"
    Value: !Sub "https://${GeniusChessAcademyApi}.execute-api.${AWS::Region}.amazonaws.com/Prod/"
```

### Testing Complete Authentication Flow

```bash
# 1. Register a coach
RESPONSE=$(curl -s -X POST http://127.0.0.1:3000/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testcoach",
    "email": "coach@test.com",
    "password": "TestPass123!",
    "name": "Test Coach",
    "phoneNumber": "+1234567890",
    "userType": "COACH",
    "isAdmin": true
  }')

# Extract access token
ACCESS_TOKEN=$(echo $RESPONSE | jq -r '.accessToken')

# 2. Use the token for authenticated requests
curl -X POST http://127.0.0.1:3000/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# 3. Login again
curl -X POST http://127.0.0.1:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "coach@test.com",
    "password": "TestPass123!",
    "userType": "COACH"
  }'
```

### Deployment Commands

#### Local Testing
```bash
# Start local API
mvn spring-boot:run

# Or with SAM
sam local start-api --port 3000
```

#### AWS Deployment
```bash
# Build and deploy
mvn clean package -P assembly-zip
sam build
sam deploy --guided
```

This implementation provides a complete, production-ready authentication system with proper separation of concerns, clean architecture, and comprehensive testing capabilities for both local development and AWS Lambda deployment.
