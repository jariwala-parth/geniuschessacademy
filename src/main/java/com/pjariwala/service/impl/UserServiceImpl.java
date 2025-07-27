package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.User;
import com.pjariwala.service.UserService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

  @Autowired private DynamoDBMapper dynamoDBMapper;

  @Override
  public User createUser(User user) {
    log.info(
        "Creating new user with email: {} and userType: {}", user.getEmail(), user.getUserType());
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    try {
      dynamoDBMapper.save(user);
      log.info(
          "User created successfully with userId: {} for email: {}",
          user.getUserId(),
          user.getEmail());
      return user;
    } catch (Exception e) {
      log.error("Failed to create user for email: {}", user.getEmail(), e);
      throw UserException.databaseError("Failed to create user", e);
    }
  }

  @Override
  public Optional<User> getUserById(String userId) {
    try {
      // Query by primary key (userId + userType)
      // Since we don't know the userType, we need to scan or query both possible values
      User coach = dynamoDBMapper.load(User.class, userId, "COACH");
      if (coach != null) {
        return Optional.of(coach);
      }

      User student = dynamoDBMapper.load(User.class, userId, "STUDENT");
      return Optional.ofNullable(student);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> getUserByUsername(String username) {
    log.debug("Searching for user by username: {}", username);
    try {
      // Use GSI for username lookup
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":username", new AttributeValue().withS(username));

      DynamoDBQueryExpression<User> queryExpression =
          new DynamoDBQueryExpression<User>()
              .withIndexName("username-index")
              .withConsistentRead(false)
              .withKeyConditionExpression("username = :username")
              .withExpressionAttributeValues(eav);

      List<User> users = dynamoDBMapper.query(User.class, queryExpression);
      boolean found = !users.isEmpty();
      log.debug("User search by username '{}' - Found: {}", username, found);
      return found ? Optional.of(users.get(0)) : Optional.empty();
    } catch (Exception e) {
      log.error("Error searching for user by username: {}", username, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> getUserByEmail(String email) {
    log.debug("Searching for user by email: {}", email);
    try {
      // Use GSI for email lookup
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":email", new AttributeValue().withS(email));

      DynamoDBQueryExpression<User> queryExpression =
          new DynamoDBQueryExpression<User>()
              .withIndexName("email-index")
              .withConsistentRead(false)
              .withKeyConditionExpression("email = :email")
              .withExpressionAttributeValues(eav);

      List<User> users = dynamoDBMapper.query(User.class, queryExpression);
      boolean found = !users.isEmpty();
      log.debug("User search by email '{}' - Found: {}", email, found);
      return found ? Optional.of(users.get(0)) : Optional.empty();
    } catch (Exception e) {
      log.error("Error searching for user by email: {}", email, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> getUserByPhone(String phoneNumber) {
    try {
      // Scan for phone number (no GSI for phone in current schema)
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":phoneNumber", new AttributeValue().withS(phoneNumber));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression("phoneNumber = :phoneNumber")
              .withExpressionAttributeValues(eav);

      List<User> users = dynamoDBMapper.scan(User.class, scanExpression);
      return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> getUserByCognitoSub(String cognitoSub) {
    try {
      // Use GSI for cognitoSub lookup
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":cognitoSub", new AttributeValue().withS(cognitoSub));

      DynamoDBQueryExpression<User> queryExpression =
          new DynamoDBQueryExpression<User>()
              .withIndexName("cognitoSub-index")
              .withConsistentRead(false)
              .withKeyConditionExpression("cognitoSub = :cognitoSub")
              .withExpressionAttributeValues(eav);

      List<User> users = dynamoDBMapper.query(User.class, queryExpression);
      return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public List<User> getUsersByType(String userType) {
    try {
      // Scan with filter for userType
      Map<String, AttributeValue> eav = new HashMap<>();
      eav.put(":userType", new AttributeValue().withS(userType));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression("userType = :userType")
              .withExpressionAttributeValues(eav);

      return dynamoDBMapper.scan(User.class, scanExpression);
    } catch (Exception e) {
      throw UserException.databaseError("Failed to retrieve users by type: " + userType, e);
    }
  }

  @Override
  public User updateUser(User user) {
    user.setUpdatedAt(LocalDateTime.now());
    dynamoDBMapper.save(user);
    return user;
  }

  @Override
  public void deleteUser(String userId) {
    try {
      // We need to find the user first to get the userType for proper deletion
      Optional<User> userOpt = getUserById(userId);
      if (userOpt.isPresent()) {
        dynamoDBMapper.delete(userOpt.get());
      } else {
        throw UserException.userNotFound(userId);
      }
    } catch (UserException e) {
      throw e; // Re-throw UserException as-is
    } catch (Exception e) {
      throw UserException.databaseError("Failed to delete user: " + userId, e);
    }
  }

  @Override
  public boolean userExistsByEmail(String email) {
    return getUserByEmail(email).isPresent();
  }

  @Override
  public String generateUserId() {
    return "USER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
