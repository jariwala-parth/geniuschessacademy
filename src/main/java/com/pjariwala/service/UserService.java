package com.pjariwala.service;

import com.pjariwala.dto.PageResponseDTO;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.dto.UserOrganizationsResponse;
import com.pjariwala.model.User;
import java.util.List;
import java.util.Optional;

public interface UserService {

  User createUser(User user);

  User getUserById(String userId, String userType);

  /** Get user by ID (tries both COACH and STUDENT types) */
  Optional<User> getUserById(String userId);

  /** Get user by username */
  Optional<User> getUserByUsername(String username);

  /** Get user by email */
  Optional<User> getUserByEmail(String email);

  /** Get user by phone number */
  Optional<User> getUserByPhone(String phoneNumber);

  /** Get user by Cognito sub */
  Optional<User> getUserByCognitoSub(String cognitoSub);

  /** Get user by ID and organization ID (for organization-scoped operations) */
  Optional<User> getUserByIdAndOrganizationId(String userId, String organizationId);

  List<User> getUsersByType(String userType, String organizationId);

  /**
   * Get users by type with search and pagination support
   *
   * @param userType The type of users to retrieve (STUDENT, COACH)
   * @param searchTerm Optional search term to filter by name, email, or phone
   * @param page Page number (0-based)
   * @param size Page size
   * @param requestingUserId The ID of the user making the request (for validation)
   * @param organizationId Organization ID for filtering
   * @return Paginated list of users matching criteria
   */
  PageResponseDTO<UserInfo> getUsersByTypeWithSearch(
      String userType,
      String searchTerm,
      int page,
      int size,
      String requestingUserId,
      String organizationId);

  User updateUser(User user);

  void deleteUser(String userId, String userType);

  /** Delete user by ID (tries both types) */
  void deleteUser(String userId);

  boolean userExists(String userId, String userType);

  /** Check if user exists by email */
  boolean userExistsByEmail(String email);

  /** Generate unique user ID */
  String generateUserId();

  /** Get user's organizations and roles */
  UserOrganizationsResponse getUserOrganizations(String userId);
}
