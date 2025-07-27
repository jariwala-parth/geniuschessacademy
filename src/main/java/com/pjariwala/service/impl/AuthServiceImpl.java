package com.pjariwala.service.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminConfirmSignUpRequest;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;
import com.amazonaws.services.cognitoidp.model.ChangePasswordRequest;
import com.amazonaws.services.cognitoidp.model.CodeMismatchException;
import com.amazonaws.services.cognitoidp.model.ConfirmForgotPasswordRequest;
import com.amazonaws.services.cognitoidp.model.ExpiredCodeException;
import com.amazonaws.services.cognitoidp.model.ForgotPasswordRequest;
import com.amazonaws.services.cognitoidp.model.GlobalSignOutRequest;
import com.amazonaws.services.cognitoidp.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidp.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidp.model.SignUpRequest;
import com.amazonaws.services.cognitoidp.model.SignUpResult;
import com.amazonaws.services.cognitoidp.model.UserNotConfirmedException;
import com.amazonaws.services.cognitoidp.model.UserNotFoundException;
import com.amazonaws.services.cognitoidp.model.UsernameExistsException;
import com.pjariwala.dto.AuthRequest;
import com.pjariwala.dto.AuthResponse;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.exception.AuthException;
import com.pjariwala.model.User;
import com.pjariwala.service.AuthService;
import com.pjariwala.service.UserService;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

  @Value("${aws.cognito.userPoolId}")
  private String userPoolId;

  @Value("${aws.cognito.clientId}")
  private String clientId;

  @Value("${aws.cognito.clientSecret}")
  private String clientSecret;

  @Value("${aws.region:ap-south-1}")
  private String awsRegion;

  @Value("${aws.accessKeyId}")
  private String awsAccessKeyId;

  @Value("${aws.secretKey}")
  private String awsSecretKey;

  @Autowired private UserService userService;

  private AWSCognitoIdentityProvider cognitoClient;

  @PostConstruct
  void init() {
    log.info("Initializing AWS Cognito client for region: {}", awsRegion);

    AWSCognitoIdentityProviderClientBuilder clientBuilder =
        AWSCognitoIdentityProviderClientBuilder.standard().withRegion(Regions.fromName(awsRegion));
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKey);
    clientBuilder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));

    this.cognitoClient = clientBuilder.build();
    log.info("AWS Cognito client initialized successfully");
  }

  public String calculateSecretHash(String username) {
    log.debug("Calculating secret hash for username: {}", username);
    try {
      final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
      SecretKeySpec signingKey =
          new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
      Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
      mac.init(signingKey);

      String message = username + clientId;
      byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

      String secretHash = Base64.getEncoder().encodeToString(rawHmac);
      log.debug("Secret hash calculated successfully for username: {}", username);
      return secretHash;
    } catch (Exception e) {
      log.error("Error calculating secret hash for username: {}", username, e);
      throw new RuntimeException("Error calculating secret hash", e);
    }
  }

  /** Validate signup request */
  private void validateSignupRequest(SignupRequest signupRequest) {
    log.debug("Validating signup request for email: {}", signupRequest.getEmail());

    if (signupRequest.getUsername() == null || signupRequest.getUsername().trim().isEmpty()) {
      log.error("Signup validation failed: Username is required");
      throw AuthException.validationError("Username is required");
    }

    if (signupRequest.getEmail() == null || signupRequest.getEmail().trim().isEmpty()) {
      log.error("Signup validation failed: Email is required");
      throw AuthException.validationError("Email is required");
    }

    if (signupRequest.getPassword() == null || signupRequest.getPassword().length() < 8) {
      log.error(
          "Signup validation failed: Password must be at least 8 characters long for email: {}",
          signupRequest.getEmail());
      throw AuthException.invalidPassword("Password must be at least 8 characters long");
    }

    if (signupRequest.getName() == null || signupRequest.getName().trim().isEmpty()) {
      log.error(
          "Signup validation failed: Name is required for email: {}", signupRequest.getEmail());
      throw AuthException.validationError("Name is required");
    }

    if (signupRequest.getUserType() == null
        || (!signupRequest.getUserType().equals("COACH")
            && !signupRequest.getUserType().equals("STUDENT"))) {
      log.error(
          "Signup validation failed: Invalid user type '{}' for email: {}",
          signupRequest.getUserType(),
          signupRequest.getEmail());
      throw AuthException.validationError("User type must be either COACH or STUDENT");
    }

    log.debug("Signup request validation passed for email: {}", signupRequest.getEmail());
  }

  /** Validate login request */
  private void validateAuthRequest(AuthRequest authRequest) {
    log.debug("Validating login request for: {}", authRequest.getLogin());

    if (authRequest.getLogin() == null || authRequest.getLogin().trim().isEmpty()) {
      log.error("Login validation failed: Login identifier is required");
      throw AuthException.validationError("Login (username, email, or phone) is required");
    }

    if (authRequest.getPassword() == null || authRequest.getPassword().trim().isEmpty()) {
      log.error(
          "Login validation failed: Password is required for login: {}", authRequest.getLogin());
      throw AuthException.validationError("Password is required");
    }

    log.debug("Login request validation passed for: {}", authRequest.getLogin());
  }

  @Override
  public AuthResponse signup(SignupRequest signupRequest) {
    log.info(
        "Starting user signup process for email: {} with userType: {}",
        signupRequest.getEmail(),
        signupRequest.getUserType());
    try {
      // Validate input
      validateSignupRequest(signupRequest);

      // Check if user already exists in our system
      if (userService.userExistsByEmail(signupRequest.getEmail())) {
        log.error("Signup failed: User already exists with email: {}", signupRequest.getEmail());
        throw AuthException.userExists();
      }

      // Create user in Cognito User Pool
      // NOTE: Consider using email as username for better UX in future:
      // .withUsername(signupRequest.getEmail()) instead of signupRequest.getUsername()
      SignUpRequest cognitoSignupRequest =
          new SignUpRequest()
              .withClientId(clientId)
              .withUsername(signupRequest.getUsername())
              .withPassword(signupRequest.getPassword())
              .withSecretHash(calculateSecretHash(signupRequest.getUsername()))
              .withUserAttributes(
                  new AttributeType().withName("email").withValue(signupRequest.getEmail()),
                  new AttributeType().withName("name").withValue(signupRequest.getName()),
                  new AttributeType()
                      .withName("phone_number")
                      .withValue(signupRequest.getPhoneNumber()),
                  new AttributeType()
                      .withName("custom:user_type")
                      .withValue(signupRequest.getUserType()));

      // Add student-specific attributes
      if ("STUDENT".equals(signupRequest.getUserType())) {
        if (signupRequest.getGuardianName() != null) {
          cognitoSignupRequest
              .getUserAttributes()
              .add(
                  new AttributeType()
                      .withName("custom:guardian_name")
                      .withValue(signupRequest.getGuardianName()));
        }
        if (signupRequest.getGuardianPhone() != null) {
          cognitoSignupRequest
              .getUserAttributes()
              .add(
                  new AttributeType()
                      .withName("custom:guardian_phone")
                      .withValue(signupRequest.getGuardianPhone()));
        }
      }

      // Add coach-specific attributes
      if ("COACH".equals(signupRequest.getUserType())) {
        cognitoSignupRequest
            .getUserAttributes()
            .add(
                new AttributeType()
                    .withName("custom:is_admin")
                    .withValue(signupRequest.getIsAdmin().toString()));
      }

      log.debug("Creating user in Cognito User Pool for email: {}", signupRequest.getEmail());
      SignUpResult signUpResult = cognitoClient.signUp(cognitoSignupRequest);
      log.info(
          "User created in Cognito successfully with sub: {} for email: {}",
          signUpResult.getUserSub(),
          signupRequest.getEmail());

      // Auto-confirm the user (in production, you might want email verification)
      log.debug("Auto-confirming user in Cognito for email: {}", signupRequest.getEmail());
      AdminConfirmSignUpRequest confirmRequest =
          new AdminConfirmSignUpRequest()
              .withUserPoolId(userPoolId)
              .withUsername(signupRequest.getUsername());

      cognitoClient.adminConfirmSignUp(confirmRequest);
      log.info("User auto-confirmed in Cognito for email: {}", signupRequest.getEmail());

      // Create user record in our system
      User user = new User();
      user.setUserId(userService.generateUserId());
      user.setUserType(signupRequest.getUserType());
      user.setEmail(signupRequest.getEmail());
      user.setName(signupRequest.getName());
      user.setPhoneNumber(signupRequest.getPhoneNumber());
      user.setUsername(signupRequest.getUsername()); // ✅ Store Cognito username
      user.setCognitoSub(signUpResult.getUserSub());
      user.setIsActive(true);
      user.setCreatedAt(LocalDateTime.now());
      user.setUpdatedAt(LocalDateTime.now());

      if ("STUDENT".equals(signupRequest.getUserType())) {
        user.setGuardianName(signupRequest.getGuardianName());
        user.setGuardianPhone(signupRequest.getGuardianPhone());
        user.setJoiningDate(LocalDateTime.now());
      } else if ("COACH".equals(signupRequest.getUserType())) {
        user.setIsAdmin(signupRequest.getIsAdmin());
      }

      log.debug("Creating user record in our system for email: {}", signupRequest.getEmail());
      userService.createUser(user);
      log.info(
          "User record created successfully in our system with userId: {} for email: {}",
          user.getUserId(),
          signupRequest.getEmail());

      // Auto-login after successful signup
      log.info("Auto-login after successful signup for email: {}", signupRequest.getEmail());
      AuthResponse response =
          login(
              new AuthRequest(
                  signupRequest.getUsername(),
                  signupRequest.getPassword(),
                  signupRequest.getUserType()));

      log.info("Signup process completed successfully for email: {}", signupRequest.getEmail());
      return response;

    } catch (UsernameExistsException e) {
      log.error(
          "Signup failed: Username already exists in Cognito for email: {}",
          signupRequest.getEmail(),
          e);
      throw AuthException.userExists();
    } catch (InvalidPasswordException e) {
      log.error(
          "Signup failed: Password does not meet Cognito requirements for email: {}",
          signupRequest.getEmail(),
          e);
      throw AuthException.invalidPassword("Password does not meet requirements");
    } catch (Exception e) {
      log.error("Signup failed: Unexpected error for email: {}", signupRequest.getEmail(), e);
      throw AuthException.cognitoError("Signup failed", e);
    }
  }

  @Override
  public AuthResponse login(AuthRequest authRequest) {
    log.info(
        "Starting login process for user: {} with userType: {}",
        authRequest.getLogin(),
        authRequest.getUserType());
    try {
      // Validate input
      validateAuthRequest(authRequest);

      // ✅ STEP 1: Find user in our system first to get the Cognito username
      log.debug("Retrieving user information from our system for: {}", authRequest.getLogin());
      User user =
          userService
              .getUserByUsername(authRequest.getLogin())
              .or(() -> userService.getUserByEmail(authRequest.getLogin()))
              .or(() -> userService.getUserByPhone(authRequest.getLogin()))
              .orElseThrow(
                  () -> {
                    log.error("User not found in our system for login: {}", authRequest.getLogin());
                    return AuthException.invalidCredentials();
                  });

      log.info(
          "User found in our system - userId: {}, cognitoUsername: {} for login: {}",
          user.getUserId(),
          user.getUsername(),
          authRequest.getLogin());

      // ✅ STEP 2: Use the stored Cognito username for authentication
      String cognitoUsername = user.getUsername();
      if (cognitoUsername == null || cognitoUsername.trim().isEmpty()) {
        log.error("Cognito username not found for user: {}", authRequest.getLogin());
        throw AuthException.invalidCredentials();
      }

      // ✅ STEP 3: Prepare authentication parameters with correct username
      log.debug(
          "Preparing authentication parameters for Cognito with username: {}", cognitoUsername);
      Map<String, String> authParameters = new HashMap<>();
      authParameters.put("USERNAME", cognitoUsername);
      authParameters.put("PASSWORD", authRequest.getPassword());
      authParameters.put(
          "SECRET_HASH", calculateSecretHash(cognitoUsername));

      // ✅ STEP 4: Initiate authentication with Cognito
      log.debug("Initiating authentication with Cognito for username: {}", cognitoUsername);
      AdminInitiateAuthRequest initiateAuthRequest =
          new AdminInitiateAuthRequest()
              .withUserPoolId(userPoolId)
              .withClientId(clientId)
              .withAuthFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
              .withAuthParameters(authParameters);

      AdminInitiateAuthResult authResult = cognitoClient.adminInitiateAuth(initiateAuthRequest);

      // Handle any authentication challenges
      if (authResult.getChallengeName() != null) {
        log.error(
            "Authentication challenge required for user: {} - Challenge: {}",
            authRequest.getLogin(),
            authResult.getChallengeName());
        throw new RuntimeException(
            "Authentication challenge required: " + authResult.getChallengeName());
      }

      // Get authentication result
      AuthenticationResultType authenticationResult = authResult.getAuthenticationResult();

      if (authenticationResult == null) {
        log.error(
            "Authentication failed - no result returned from Cognito for user: {}",
            authRequest.getLogin());
        throw new RuntimeException("Authentication failed - no result returned");
      }

      log.info("Cognito authentication successful for user: {}", authRequest.getLogin());

      // Build response with real JWT tokens from Cognito
      AuthResponse response = new AuthResponse();
      response.setAccessToken(authenticationResult.getAccessToken());
      response.setRefreshToken(authenticationResult.getRefreshToken());
      response.setIdToken(authenticationResult.getIdToken());
      response.setTokenType(authenticationResult.getTokenType());
      response.setExpiresIn(authenticationResult.getExpiresIn());

      // Set user information using separate UserInfo class
      UserInfo userInfo = new UserInfo();
      userInfo.setUserId(user.getUserId());
      userInfo.setEmail(user.getEmail());
      userInfo.setName(user.getName());
      userInfo.setUserType(user.getUserType());
      userInfo.setPhoneNumber(user.getPhoneNumber());
      response.setUserInfo(userInfo);

      log.info("Login process completed successfully for user: {}", authRequest.getLogin());
      return response;

    } catch (NotAuthorizedException e) {
      log.error("Login failed: Invalid credentials for user: {}", authRequest.getLogin(), e);
      throw AuthException.invalidCredentials();
    } catch (UserNotConfirmedException e) {
      log.error("Login failed: Email not confirmed for user: {}", authRequest.getLogin(), e);
      throw AuthException.emailNotConfirmed();
    } catch (Exception e) {
      log.error("Login failed: Unexpected error for user: {}", authRequest.getLogin(), e);
      // Add more specific error details for debugging
      if (e.getMessage() != null) {
        log.error("Cognito error details: {}", e.getMessage());
      }
      throw AuthException.cognitoError("Login failed", e);
    }
  }

  @Override
  public AuthResponse refreshToken(String refreshToken) {
    log.info("Starting token refresh process");
    try {
      if (refreshToken == null || refreshToken.trim().isEmpty()) {
        log.error("Token refresh failed: Refresh token is required");
        throw new RuntimeException("Refresh token is required");
      }

      // Prepare authentication parameters for refresh
      Map<String, String> authParameters = new HashMap<>();
      authParameters.put("REFRESH_TOKEN", refreshToken);

      // Initiate auth with refresh token
      AdminInitiateAuthRequest initiateAuthRequest =
          new AdminInitiateAuthRequest()
              .withUserPoolId(userPoolId)
              .withClientId(clientId)
              .withAuthFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
              .withAuthParameters(authParameters);

      AdminInitiateAuthResult authResult = cognitoClient.adminInitiateAuth(initiateAuthRequest);
      AuthenticationResultType authenticationResult = authResult.getAuthenticationResult();

      if (authenticationResult == null) {
        throw new RuntimeException("Token refresh failed - no result returned");
      }

      // Build response with new tokens
      AuthResponse response = new AuthResponse();
      response.setAccessToken(authenticationResult.getAccessToken());
      response.setIdToken(authenticationResult.getIdToken());
      response.setTokenType(authenticationResult.getTokenType());
      response.setExpiresIn(authenticationResult.getExpiresIn());

      // Refresh token might not be returned in refresh operation
      if (authenticationResult.getRefreshToken() != null) {
        response.setRefreshToken(authenticationResult.getRefreshToken());
      } else {
        response.setRefreshToken(refreshToken); // Use the original refresh token
      }

      log.info("Token refresh completed successfully");
      return response;

    } catch (NotAuthorizedException e) {
      log.error("Token refresh failed: Invalid refresh token", e);
      throw new RuntimeException("Invalid refresh token");
    } catch (Exception e) {
      log.error("Token refresh failed: Unexpected error", e);
      throw new RuntimeException("Error refreshing token: " + e.getMessage(), e);
    }
  }

  @Override
  public void logout(String accessToken) {
    log.info("Starting logout process");
    try {
      if (accessToken == null || accessToken.trim().isEmpty()) {
        log.error("Logout failed: Access token is required");
        throw new RuntimeException("Access token is required");
      }

      // Global sign out - invalidates all tokens for the user
      log.debug("Initiating global sign out with Cognito");
      GlobalSignOutRequest signOutRequest = new GlobalSignOutRequest().withAccessToken(accessToken);

      cognitoClient.globalSignOut(signOutRequest);
      log.info("Logout completed successfully");

    } catch (Exception e) {
      log.error("Logout failed: Unexpected error", e);
      throw new RuntimeException("Error during logout: " + e.getMessage(), e);
    }
  }

  @Override
  public void changePassword(String accessToken, String oldPassword, String newPassword) {
    log.info("Starting password change process");
    try {
      if (accessToken == null || accessToken.trim().isEmpty()) {
        log.error("Password change failed: Access token is required");
        throw new RuntimeException("Access token is required");
      }

      if (oldPassword == null || oldPassword.trim().isEmpty()) {
        log.error("Password change failed: Old password is required");
        throw new RuntimeException("Old password is required");
      }

      if (newPassword == null || newPassword.length() < 8) {
        log.error("Password change failed: New password must be at least 8 characters long");
        throw new RuntimeException("New password must be at least 8 characters long");
      }

      log.debug("Initiating password change with Cognito");
      ChangePasswordRequest changePasswordRequest =
          new ChangePasswordRequest()
              .withAccessToken(accessToken)
              .withPreviousPassword(oldPassword)
              .withProposedPassword(newPassword);

      cognitoClient.changePassword(changePasswordRequest);
      log.info("Password change completed successfully");

    } catch (InvalidPasswordException e) {
      log.error("Password change failed: New password does not meet Cognito requirements", e);
      throw new RuntimeException("New password does not meet requirements");
    } catch (NotAuthorizedException e) {
      log.error("Password change failed: Invalid current password", e);
      throw new RuntimeException("Invalid current password");
    } catch (Exception e) {
      log.error("Password change failed: Unexpected error", e);
      throw new RuntimeException("Error changing password: " + e.getMessage(), e);
    }
  }

  @Override
  public void forgotPassword(String login) {
    log.info("Starting forgot password process for user: {}", login);
    try {
      if (login == null || login.trim().isEmpty()) {
        log.error("Forgot password failed: Login identifier is required");
        throw new RuntimeException("Login (username, email, or phone) is required");
      }

      // ✅ Find user to get Cognito username
      log.debug("Retrieving user information from our system for: {}", login);
      User user =
          userService
              .getUserByUsername(login)
              .or(() -> userService.getUserByEmail(login))
              .or(() -> userService.getUserByPhone(login))
              .orElseThrow(
                  () -> {
                    log.error("User not found in our system for login: {}", login);
                    return new RuntimeException("User not found");
                  });

      String cognitoUsername = user.getUsername();
      if (cognitoUsername == null || cognitoUsername.trim().isEmpty()) {
        log.error("Cognito username not found for user: {}", login);
        throw new RuntimeException("User not found");
      }

      log.debug("Initiating forgot password with Cognito for username: {}", cognitoUsername);
      ForgotPasswordRequest forgotPasswordRequest =
          new ForgotPasswordRequest()
              .withClientId(clientId)
              .withUsername(cognitoUsername) // ✅ Use actual Cognito username
              .withSecretHash(
                  calculateSecretHash(cognitoUsername)); // ✅ Calculate with correct username

      cognitoClient.forgotPassword(forgotPasswordRequest);
      log.info("Forgot password initiated successfully for user: {}", login);

    } catch (UserNotFoundException e) {
      log.error("Forgot password failed: User not found for: {}", login, e);
      throw new RuntimeException("User not found");
    } catch (Exception e) {
      log.error("Forgot password failed: Unexpected error for user: {}", login, e);
      throw new RuntimeException("Error initiating forgot password: " + e.getMessage(), e);
    }
  }

  @Override
  public void resetPassword(String login, String confirmationCode, String newPassword) {
    log.info("Starting password reset process for user: {}", login);
    try {
      if (login == null || login.trim().isEmpty()) {
        log.error("Password reset failed: Login identifier is required");
        throw new RuntimeException("Login (username, email, or phone) is required");
      }

      if (confirmationCode == null || confirmationCode.trim().isEmpty()) {
        log.error("Password reset failed: Confirmation code is required for user: {}", login);
        throw new RuntimeException("Confirmation code is required");
      }

      if (newPassword == null || newPassword.length() < 8) {
        log.error(
            "Password reset failed: New password must be at least 8 characters long for user: {}",
            login);
        throw new RuntimeException("New password must be at least 8 characters long");
      }

      // ✅ Find user to get Cognito username
      log.debug("Retrieving user information from our system for: {}", login);
      User user =
          userService
              .getUserByUsername(login)
              .or(() -> userService.getUserByEmail(login))
              .or(() -> userService.getUserByPhone(login))
              .orElseThrow(
                  () -> {
                    log.error("User not found in our system for login: {}", login);
                    return new RuntimeException("User not found");
                  });

      String cognitoUsername = user.getUsername();
      if (cognitoUsername == null || cognitoUsername.trim().isEmpty()) {
        log.error("Cognito username not found for user: {}", login);
        throw new RuntimeException("User not found");
      }

      log.debug("Confirming forgot password with Cognito for username: {}", cognitoUsername);
      ConfirmForgotPasswordRequest confirmRequest =
          new ConfirmForgotPasswordRequest()
              .withClientId(clientId)
              .withUsername(cognitoUsername) // ✅ Use actual Cognito username
              .withConfirmationCode(confirmationCode)
              .withPassword(newPassword)
              .withSecretHash(
                  calculateSecretHash(cognitoUsername)); // ✅ Calculate with correct username

      cognitoClient.confirmForgotPassword(confirmRequest);
      log.info("Password reset completed successfully for user: {}", login);

    } catch (InvalidPasswordException e) {
      log.error(
          "Password reset failed: New password does not meet Cognito requirements for user: {}",
          login,
          e);
      throw new RuntimeException("New password does not meet requirements");
    } catch (CodeMismatchException e) {
      log.error("Password reset failed: Invalid confirmation code for user: {}", login, e);
      throw new RuntimeException("Invalid confirmation code");
    } catch (ExpiredCodeException e) {
      log.error("Password reset failed: Confirmation code has expired for user: {}", login, e);
      throw new RuntimeException("Confirmation code has expired");
    } catch (Exception e) {
      log.error("Password reset failed: Unexpected error for user: {}", login, e);
      throw new RuntimeException("Error resetting password: " + e.getMessage(), e);
    }
  }
}
