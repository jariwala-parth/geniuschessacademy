package com.pjariwala.service;

import com.pjariwala.model.User;
import java.util.List;
import java.util.Optional;

public interface UserService {

  /** Create a new user */
  User createUser(User user);

  /** Get user by ID */
  Optional<User> getUserById(String userId);

  /** Get user by username */
  Optional<User> getUserByUsername(String username);

  /** Get user by email */
  Optional<User> getUserByEmail(String email);

  /** Get user by phone number */
  Optional<User> getUserByPhone(String phoneNumber);

  /** Get user by Cognito sub */
  Optional<User> getUserByCognitoSub(String cognitoSub);

  /** Get all users by type */
  List<User> getUsersByType(String userType);

  /** Update user */
  User updateUser(User user);

  /** Delete user */
  void deleteUser(String userId);

  /** Check if user exists by email */
  boolean userExistsByEmail(String email);

  /** Generate unique user ID */
  String generateUserId();
}
