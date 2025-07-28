# AWS Setup Guide for Genius Chess Academy

This guide will help you set up AWS services for the chess academy application, including DynamoDB tables and proper AWS credentials configuration.

## Prerequisites

- AWS Account
- AWS CLI installed and configured
- Java 21
- Maven 3.6+

## 1. AWS Credentials Setup

### Option A: AWS CLI Configuration (Recommended for Development)

1. **Install AWS CLI** (if not already installed):
   ```bash
   # macOS
   brew install awscli
   
   # Windows
   pip install awscli
   
   # Linux
   sudo apt-get install awscli
   ```

2. **Configure AWS CLI**:
   ```bash
   aws configure
   ```
   
   Enter your credentials:
   ```
   AWS Access Key ID [None]: AKIAIOSFODNN7EXAMPLE
   AWS Secret Access Key [None]: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
   Default region name [None]: us-east-1
   Default output format [None]: json
   ```

3. **Verify Configuration**:
   ```bash
   aws sts get-caller-identity
   ```

### Option B: Environment Variables

Set these environment variables:
```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export AWS_DEFAULT_REGION=us-east-1
```

### Option C: IAM Roles (For EC2/Lambda)

If running on AWS infrastructure, attach an IAM role with appropriate permissions.

## 2. Required AWS Permissions

Your AWS user/role needs these permissions:

### DynamoDB Permissions
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:CreateTable",
                "dynamodb:DescribeTable",
                "dynamodb:PutItem",
                "dynamodb:GetItem",
                "dynamodb:UpdateItem",
                "dynamodb:DeleteItem",
                "dynamodb:Query",
                "dynamodb:Scan"
            ],
            "Resource": [
                "arn:aws:dynamodb:*:*:table/Users",
                "arn:aws:dynamodb:*:*:table/Users/index/*"
            ]
        }
    ]
}
```

### Cognito Permissions
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "cognito-idp:AdminCreateUser",
                "cognito-idp:AdminConfirmSignUp",
                "cognito-idp:AdminInitiateAuth",
                "cognito-idp:SignUp",
                "cognito-idp:InitiateAuth",
                "cognito-idp:GlobalSignOut",
                "cognito-idp:ChangePassword",
                "cognito-idp:ForgotPassword",
                "cognito-idp:ConfirmForgotPassword"
            ],
            "Resource": "arn:aws:cognito-idp:*:*:userpool/*"
        }
    ]
}
```

## 3. Create AWS Cognito User Pool

### Step 1: Create User Pool
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
  ]' \
  --region us-east-1
```

**Save the UserPoolId from the response!**

### Step 2: Create App Client
```bash
aws cognito-idp create-user-pool-client \
  --user-pool-id us-east-1_XXXXXXXXX \
  --client-name "GeniusChessAcademyClient" \
  --generate-secret \
  --explicit-auth-flows ADMIN_NO_SRP_AUTH ALLOW_REFRESH_TOKEN_AUTH \
  --token-validity-units AccessToken=hours,IdToken=hours,RefreshToken=days \
  --access-token-validity 1 \
  --id-token-validity 1 \
  --refresh-token-validity 30 \
  --region us-east-1
```

**Save the ClientId and ClientSecret from the response!**

## 4. Create DynamoDB Table

### Option A: Using AWS CLI
```bash
aws dynamodb create-table \
  --table-name Users \
  --attribute-definitions \
    AttributeName=userId,AttributeType=S \
    AttributeName=userType,AttributeType=S \
    AttributeName=email,AttributeType=S \
    AttributeName=cognitoSub,AttributeType=S \
  --key-schema \
    AttributeName=userId,KeyType=HASH \
    AttributeName=userType,KeyType=RANGE \
  --global-secondary-indexes \
    IndexName=email-index,KeySchema='[{AttributeName=email,KeyType=HASH}]',Projection='{ProjectionType=ALL}',BillingMode=PAY_PER_REQUEST \
    IndexName=cognitoSub-index,KeySchema='[{AttributeName=cognitoSub,KeyType=HASH}]',Projection='{ProjectionType=ALL}',BillingMode=PAY_PER_REQUEST \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### Option B: Using AWS Console

1. **Go to DynamoDB Console**: https://console.aws.amazon.com/dynamodb/
2. **Click "Create table"**
3. **Configure table**:
   - Table name: `Users`
   - Partition key: `userId` (String)
   - Sort key: `userType` (String)
4. **Add Global Secondary Indexes**:
   
   **GSI 1 - email-index**:
   - Partition key: `email` (String)
   - Projection type: All attributes
   
   **GSI 2 - cognitoSub-index**:
   - Partition key: `cognitoSub` (String)
   - Projection type: All attributes

5. **Set billing mode**: On-demand
6. **Click "Create table"**

### Verify Table Creation
```bash
aws dynamodb describe-table --table-name Users --region us-east-1
```

## 5. Configure Application

### Update application.properties
```properties
# AWS Cognito Configuration
aws.cognito.userPoolId=us-east-1_XXXXXXXXX
aws.cognito.clientId=your-client-id
aws.cognito.clientSecret=your-client-secret

# AWS Region
aws.region=us-east-1

# DynamoDB Configuration
aws.dynamodb.tableName=GCA_Users
aws.dynamodb.region=us-east-1

# For production, set this to false
local.development=false
```

### Environment Variables (Alternative)
```bash
export COGNITO_USER_POOL_ID=us-east-1_XXXXXXXXX
export COGNITO_CLIENT_ID=your-client-id
export COGNITO_CLIENT_SECRET=your-client-secret
export AWS_REGION=us-east-1
```

## 6. Testing the Setup

### Test DynamoDB Connection
```bash
# Test table access
aws dynamodb scan --table-name Users --region us-east-1
```

### Test Cognito User Pool
```bash
# List user pools
aws cognito-idp list-user-pools --max-results 10 --region us-east-1
```

### Test Application Locally

1. **Start the application**:
   ```bash
   cd gca_be/GeniusChessAcademy
   mvn spring-boot:run
   ```

2. **Test signup endpoint**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/signup \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@chessacademy.com",
       "password": "TestPass123!",
       "name": "Test User",
       "phoneNumber": "+1234567890",
       "userType": "COACH",
       "isAdmin": true
     }'
   ```

## 7. Troubleshooting

### Common Issues

#### Issue: "Unable to load AWS credentials"
**Solution**: 
- Ensure AWS CLI is configured: `aws configure`
- Or set environment variables
- Or check IAM role permissions

#### Issue: "User pool does not exist"
**Solution**:
- Verify the User Pool ID in application.properties
- Ensure the region is correct
- Check if the user pool exists: `aws cognito-idp list-user-pools --max-results 10`

#### Issue: "Table 'Users' doesn't exist"
**Solution**:
- Create the DynamoDB table using the commands above
- Verify table exists: `aws dynamodb describe-table --table-name Users`
- Check the region configuration

#### Issue: "Access denied" errors
**Solution**:
- Check IAM permissions
- Ensure your AWS user/role has the required permissions listed above

#### Issue: "Invalid client secret"
**Solution**:
- Regenerate the client secret in Cognito console
- Update application.properties with the new secret

### Local Development Mode

For local development without AWS setup:
```properties
# Use in-memory storage
local.development=true

# Mock Cognito (you'll need to implement this)
aws.cognito.userPoolId=mock-pool
aws.cognito.clientId=mock-client
aws.cognito.clientSecret=mock-secret
```

## 8. Cost Considerations

### DynamoDB Costs (Pay-per-request)
- **Read requests**: $0.25 per million
- **Write requests**: $1.25 per million
- **Storage**: $0.25 per GB per month

### Cognito Costs
- **Monthly Active Users**: $0.0055 per MAU
- **First 50,000 MAUs**: Free

### Estimated Monthly Cost for 100 Active Users
- DynamoDB: ~$5-10
- Cognito: ~$0.55
- **Total**: ~$6-11/month

## 9. Security Best Practices

1. **Use IAM roles instead of access keys when possible**
2. **Rotate access keys regularly**
3. **Use least privilege principle for IAM permissions**
4. **Enable CloudTrail for audit logging**
5. **Use VPC endpoints for DynamoDB in production**
6. **Enable encryption at rest for DynamoDB**
7. **Use environment-specific Cognito User Pools**

## 10. Next Steps

1. **Set up AWS credentials** using one of the methods above
2. **Create Cognito User Pool** and save the credentials
3. **Create DynamoDB table** with the specified schema
4. **Update application.properties** with your AWS configuration
5. **Test the application** locally
6. **Deploy to AWS Lambda** when ready for production

For production deployment, see the main README.md for SAM CLI deployment instructions. 