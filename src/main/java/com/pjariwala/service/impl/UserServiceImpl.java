package com.pjariwala.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.dto.UserOrganizationInfo;
import com.pjariwala.dto.UserOrganizationsResponse;
import com.pjariwala.enums.UserType;
import com.pjariwala.exception.AuthException;
import com.pjariwala.exception.UserException;
import com.pjariwala.model.Organization;
import com.pjariwala.model.User;
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

  @Override
  public User createUser(User user) {
    try {
      dynamoDBMapper.save(user);
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
      User coach = dynamoDBMapper.load(User.class, userId, UserType.COACH.name());
      if (coach != null) {
        return Optional.of(coach);
      }

      User student = dynamoDBMapper.load(User.class, userId, UserType.STUDENT.name());
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
  public List<User> getUsersByType(String userType, String organizationId) {
    try {
      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      expressionAttributeValues.put(":userType", new AttributeValue().withS(userType));
      expressionAttributeValues.put(":organizationId", new AttributeValue().withS(organizationId));

      DynamoDBScanExpression scanExpression =
          new DynamoDBScanExpression()
              .withFilterExpression("userType = :userType AND organizationId = :organizationId")
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
      String userType,
      String searchTerm,
      int page,
      int size,
      String requestingUserId,
      String organizationId) {
    try {
      log.info(
          "evt=search_users userType={} searchTerm={} page={} size={} requestingUserId={}"
              + " organizationId={}",
          userType,
          searchTerm,
          page,
          size,
          requestingUserId,
          organizationId);

      // Validate organization access
      validateOrganizationAccess(requestingUserId, organizationId);

      // Validate that the requesting user is a coach
      requireCoachPermission(requestingUserId, organizationId);

      // First, get all users of the specified type
      List<User> allUsers = getUsersByType(userType, organizationId);

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
          "evt=search_users_success total={} returned={} page={}",
          totalElements,
          userInfoList.size(),
          page);
      return response;

    } catch (Exception e) {
      log.error("evt=search_users_error userType={} error={}", userType, e.getMessage(), e);
      throw UserException.databaseError("Failed to search users", e);
    }
  }

  @Override
  public User updateUser(User user) {
    try {
      dynamoDBMapper.save(user);
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

  @Override
  public UserOrganizationsResponse getUserOrganizations(String userId) {
    log.info("evt=get_user_organizations_start userId={}", userId);

    try {
      List<UserOrganizationInfo> organizations = new ArrayList<>();

      // Get user by ID to find their organizations
      Optional<User> userOpt = getUserById(userId);
      if (userOpt.isEmpty()) {
        log.error("evt=get_user_organizations_user_not_found userId={}", userId);
        throw UserException.userNotFound("User not found: " + userId);
      }

      User user = userOpt.get();

      // If user is a SUPER_ADMIN, they can access all organizations
      if ("SUPER_ADMIN".equals(user.getUserType())) {
        // Get all organizations
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        List<Organization> allOrgs = dynamoDBMapper.scan(Organization.class, scanExpression);

        for (Organization org : allOrgs) {
          if (org.getIsActive() != null && org.getIsActive()) {
            UserOrganizationInfo orgInfo = new UserOrganizationInfo();
            orgInfo.setOrganizationId(org.getOrganizationId());
            orgInfo.setOrganizationName(org.getOrganizationName());
            orgInfo.setUserRole("SUPER_ADMIN");
            orgInfo.setIsActive(org.getIsActive());
            organizations.add(orgInfo);
          }
        }
      } else {
        // For regular users, check if they own any organizations
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":ownerId", new AttributeValue().withS(userId));

        DynamoDBQueryExpression<Organization> queryExpression =
            new DynamoDBQueryExpression<Organization>()
                .withIndexName("ownerId-index")
                .withConsistentRead(false)
                .withKeyConditionExpression("ownerId = :ownerId")
                .withExpressionAttributeValues(eav);

        List<Organization> ownedOrgs = dynamoDBMapper.query(Organization.class, queryExpression);

        for (Organization org : ownedOrgs) {
          if (org.getIsActive() != null && org.getIsActive()) {
            UserOrganizationInfo orgInfo = new UserOrganizationInfo();
            orgInfo.setOrganizationId(org.getOrganizationId());
            orgInfo.setOrganizationName(org.getOrganizationName());
            orgInfo.setUserRole("OWNER");
            orgInfo.setIsActive(org.getIsActive());
            organizations.add(orgInfo);
          }
        }

        // Also add the organization where the user is a member (COACH or STUDENT)
        if (user.getOrganizationId() != null) {
          Organization userOrg = dynamoDBMapper.load(Organization.class, user.getOrganizationId());
          if (userOrg != null && userOrg.getIsActive() != null && userOrg.getIsActive()) {
            // Check if this organization is not already added as owned
            boolean alreadyAdded =
                organizations.stream()
                    .anyMatch(org -> org.getOrganizationId().equals(user.getOrganizationId()));

            if (!alreadyAdded) {
              UserOrganizationInfo orgInfo = new UserOrganizationInfo();
              orgInfo.setOrganizationId(user.getOrganizationId());
              orgInfo.setOrganizationName(userOrg.getOrganizationName());
              orgInfo.setUserRole(user.getUserType()); // COACH or STUDENT
              orgInfo.setIsActive(userOrg.getIsActive());
              organizations.add(orgInfo);
            }
          }
        }
      }

      log.info(
          "evt=get_user_organizations_success userId={} count={}", userId, organizations.size());
      return new UserOrganizationsResponse(organizations);

    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      log.error("evt=get_user_organizations_error userId={} error={}", userId, e.getMessage(), e);
      throw UserException.databaseError("Failed to get user organizations", e);
    }
  }

  private UserInfo convertToUserInfo(User user) {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(user.getUserId());
    userInfo.setEmail(user.getEmail());
    userInfo.setName(user.getName());
    userInfo.setPhoneNumber(user.getPhoneNumber());
    return userInfo;
  }

  /** Validate that the requesting user has access to the organization */
  private void validateOrganizationAccess(String requestingUserId, String organizationId) {
    try {
      Optional<User> userOpt = getUserById(requestingUserId);
      if (userOpt.isEmpty()) {
        log.error(
            "evt=validate_org_access_user_not_found requestingUserId={} organizationId={}",
            requestingUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "User not found", 403);
      }

      User user = userOpt.get();
      if (!organizationId.equals(user.getOrganizationId())) {
        log.error(
            "evt=validate_org_access_denied requestingUserId={} userOrgId={} requestedOrgId={}",
            requestingUserId,
            user.getOrganizationId(),
            organizationId);
        throw new AuthException("ACCESS_DENIED", "You can only access your own organization", 403);
      }

      log.debug(
          "evt=validate_org_access_success requestingUserId={} organizationId={}",
          requestingUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=validate_org_access_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate organization access", 403);
    }
  }

  /** Require that the requesting user is a coach */
  private void requireCoachPermission(String requestingUserId, String organizationId) {
    try {
      Optional<User> userOpt = getUserById(requestingUserId);
      if (userOpt.isEmpty()) {
        log.error(
            "evt=require_coach_permission_user_not_found requestingUserId={} organizationId={}",
            requestingUserId,
            organizationId);
        throw new AuthException("ACCESS_DENIED", "User not found", 403);
      }

      User user = userOpt.get();
      if (!UserType.COACH.name().equals(user.getUserType())) {
        log.error(
            "evt=require_coach_permission_denied requestingUserId={} userType={} organizationId={}",
            requestingUserId,
            user.getUserType(),
            organizationId);
        throw new AuthException("ACCESS_DENIED", "Coach permission required", 403);
      }

      log.debug(
          "evt=require_coach_permission_granted requestingUserId={} organizationId={}",
          requestingUserId,
          organizationId);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "evt=require_coach_permission_error requestingUserId={} organizationId={} error={}",
          requestingUserId,
          organizationId,
          e.getMessage(),
          e);
      throw new AuthException("ACCESS_DENIED", "Failed to validate coach permission", 403);
    }
  }
}
