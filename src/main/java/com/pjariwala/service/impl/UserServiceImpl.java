package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.enums.ActionType;
import com.pjariwala.enums.EntityType;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.UserService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

  @Autowired private DynamoDBMapper dynamoDBMapper;
  @Autowired private ActivityLogService activityLogService;

  @Override
  public User createUser(User user) {
    try {
      dynamoDBMapper.save(user);

      // Log user creation activity
      try {
        activityLogService.logAction(
            "COACH".equals(user.getUserType())
                ? ActionType.SYSTEM_ACTION
                : ActionType.CREATE_STUDENT,
            user.getUserId(),
            user.getName(),
            "COACH".equals(user.getUserType()) ? UserType.COACH : UserType.STUDENT,
            String.format("Created new %s: %s", user.getUserType().toLowerCase(), user.getName()),
            EntityType.USER,
            user.getUserId(),
            user.getName());
      } catch (Exception e) {
        log.warn("Failed to log user creation activity for user: {}", user.getUserId(), e);
      }

      return user;
    } catch (Exception e) {
      log.error("Failed to create user", e);
      throw UserException.databaseError("Failed to create user", e);
    }
  }

  @Override
  public User getUserById(String userId, String userType) {
    try {
      return dynamoDBMapper.load(User.class, userId, userType);
    } catch (Exception e) {
      log.error("Failed to get user by ID: {}", userId, e);
      throw UserException.databaseError("Failed to get user", e);
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
    log.debug("evt=get_user_by_username_start username={}", username);
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
      log.debug("evt=get_user_by_username_result username={} found={}", username, found);
      return found ? Optional.of(users.get(0)) : Optional.empty();
    } catch (Exception e) {
      log.error("evt=get_user_by_username_error username={}", username, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> getUserByEmail(String email) {
    log.debug("evt=get_user_by_email_start email={}", email);
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
      log.debug("evt=get_user_by_email_result email={} found={}", email, found);
      return found ? Optional.of(users.get(0)) : Optional.empty();
    } catch (Exception e) {
      log.error("evt=get_user_by_email_error email={}", email, e);
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
      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      expressionAttributeValues.put(":userType", new AttributeValue().withS(userType));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression("userType = :userType")
              .withExpressionAttributeValues(expressionAttributeValues);

      PaginatedScanList<User> scanResult = dynamoDBMapper.scan(User.class, scanExpression);
      return new ArrayList<>(scanResult);
    } catch (Exception e) {
      log.error("Failed to get users by type: {}", userType, e);
      throw UserException.databaseError("Failed to get users by type", e);
    }
  }

  @Override
  public PageResponseDTO<UserInfo> getUsersByTypeWithSearch(
      String userType, String searchTerm, int page, int size) {
    try {
      log.info(
          "Searching users: userType={}, searchTerm={}, page={}, size={}",
          userType,
          searchTerm,
          page,
          size);

      // First, get all users of the specified type
      List<User> allUsers = getUsersByType(userType);

      // Apply search filter if provided
      List<User> filteredUsers = allUsers;
      if (StringUtils.hasText(searchTerm)) {
        String searchLower = searchTerm.toLowerCase().trim();
        filteredUsers =
            allUsers.stream()
                .filter(
                    user ->
                        (user.getName() != null
                                && user.getName().toLowerCase().contains(searchLower))
                            || (user.getEmail() != null
                                && user.getEmail().toLowerCase().contains(searchLower))
                            || (user.getPhoneNumber() != null
                                && user.getPhoneNumber().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());
      }

      // Calculate pagination
      int totalElements = filteredUsers.size();
      int totalPages = (int) Math.ceil((double) totalElements / size);
      int startIndex = page * size;
      int endIndex = Math.min(startIndex + size, totalElements);

      // Get page content
      List<User> pageContent = new ArrayList<>();
      if (startIndex < totalElements) {
        pageContent = filteredUsers.subList(startIndex, endIndex);
      }

      // Convert to UserInfo
      List<UserInfo> userInfoList =
          pageContent.stream().map(this::convertToUserInfo).collect(Collectors.toList());

      // Create page info
      PageResponseDTO.PageInfoDTO pageInfo = new PageResponseDTO.PageInfoDTO();
      pageInfo.setCurrentPage(page);
      pageInfo.setPageSize(size);
      pageInfo.setTotalPages(totalPages);
      pageInfo.setTotalElements((long) totalElements);

      // Create response
      PageResponseDTO<UserInfo> response = new PageResponseDTO<>();
      response.setContent(userInfoList);
      response.setPageInfo(pageInfo);

      log.info(
          "Search completed: found {} total, returning {} on page {}",
          totalElements,
          userInfoList.size(),
          page);
      return response;

    } catch (Exception e) {
      log.error("Failed to search users by type: {}", userType, e);
      throw UserException.databaseError("Failed to search users", e);
    }
  }

  @Override
  public User updateUser(User user) {
    try {
      dynamoDBMapper.save(user);

      // Log user update activity
      try {
        activityLogService.logAction(
            ActionType.UPDATE_STUDENT,
            user.getUserId(),
            user.getName(),
            "COACH".equals(user.getUserType()) ? UserType.COACH : UserType.STUDENT,
            String.format(
                "Updated %s profile: %s", user.getUserType().toLowerCase(), user.getName()),
            EntityType.USER,
            user.getUserId(),
            user.getName());
      } catch (Exception e) {
        log.warn("Failed to log user update activity for user: {}", user.getUserId(), e);
      }

      return user;
    } catch (Exception e) {
      log.error("Failed to update user: {}", user.getUserId(), e);
      throw UserException.databaseError("Failed to update user", e);
    }
  }

  @Override
  public void deleteUser(String userId, String userType) {
    try {
      User user = getUserById(userId, userType);
      if (user != null) {
        dynamoDBMapper.delete(user);

        // Log user deletion activity
        try {
          activityLogService.logAction(
              ActionType.DELETE_STUDENT,
              user.getUserId(),
              user.getName(),
              "COACH".equals(user.getUserType()) ? UserType.COACH : UserType.STUDENT,
              String.format("Deleted %s: %s", user.getUserType().toLowerCase(), user.getName()),
              EntityType.USER,
              user.getUserId(),
              user.getName());
        } catch (Exception e) {
          log.warn("Failed to log user deletion activity for user: {}", userId, e);
        }
      }
    } catch (Exception e) {
      log.error("Failed to delete user: {}", userId, e);
      throw UserException.databaseError("Failed to delete user", e);
    }
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
  public boolean userExists(String userId, String userType) {
    try {
      User user = getUserById(userId, userType);
      return user != null;
    } catch (Exception e) {
      log.error("Failed to check if user exists: {}", userId, e);
      return false;
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

  private UserInfo convertToUserInfo(User user) {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(user.getUserId());
    userInfo.setEmail(user.getEmail());
    userInfo.setName(user.getName());
    userInfo.setUserType(user.getUserType());
    userInfo.setPhoneNumber(user.getPhoneNumber());
    return userInfo;
  }
}
