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

    } catch (AuthException e) {
      // Re-throw our custom auth exceptions (like from auto-login) - don't wrap them
      log.error(
          "evt=signup_auth_error email={} code={} msg={}",
          signupRequest.getEmail(),
          e.getErrorCode(),
          e.getMessage());
      throw e;
    } catch (UsernameExistsException e) {
      log.error(
          "evt=signup_username_exists email={} msg=username_already_exists",
          signupRequest.getEmail(),
          e);
      throw AuthException.userExists();
    } catch (InvalidPasswordException e) {
      log.error(
          "evt=signup_invalid_password email={} msg=password_requirements_not_met",
          signupRequest.getEmail(),
          e);
      throw AuthException.invalidPassword("Password does not meet requirements");
    } catch (Exception e) {
      log.error(
          "evt=signup_system_error email={} msg=unexpected_system_error",
          signupRequest.getEmail(),
          e);
      throw AuthException.cognitoError("System error during signup", e);
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
      authParameters.put("SECRET_HASH", calculateSecretHash(cognitoUsername));

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

    } catch (AuthException e) {
      // Re-throw our custom auth exceptions (like user not found) - don't wrap them
      log.error(
          "evt=login_auth_error user={} code={} msg={}",
          authRequest.getLogin(),
          e.getErrorCode(),
          e.getMessage());
      throw e;
    } catch (NotAuthorizedException e) {
      log.error(
          "evt=login_cognito_unauthorized user={} msg=invalid_credentials",
          authRequest.getLogin(),
          e);
      throw AuthException.invalidCredentials();
    } catch (UserNotConfirmedException e) {
      log.error(
          "evt=login_cognito_unconfirmed user={} msg=email_not_confirmed",
          authRequest.getLogin(),
          e);
      throw AuthException.emailNotConfirmed();
    } catch (UserNotFoundException e) {
      log.error(
          "evt=login_cognito_user_not_found user={} msg=user_not_found_in_cognito",
          authRequest.getLogin(),
          e);
      throw AuthException.invalidCredentials();
    } catch (Exception e) {
      // Only use 500 status for actual system errors, not authentication failures
      log.error(
          "evt=login_system_error user={} msg=unexpected_system_error", authRequest.getLogin(), e);
      if (e.getMessage() != null && e.getMessage().toLowerCase().contains("invalid")) {
        // If the error message suggests invalid credentials, treat as 401
        log.warn(
            "evt=login_treating_as_invalid_credentials user={} error_msg={}",
            authRequest.getLogin(),
            e.getMessage());
        throw AuthException.invalidCredentials();
      }
      throw AuthException.cognitoError("System error during login", e);
    }
  }

  @Override
  public AuthResponse refreshToken(String refreshToken) {
    log.info("evt=refresh_token_start");
    try {
      if (refreshToken == null || refreshToken.trim().isEmpty()) {
        log.error("evt=refresh_token_error msg=refresh_token_required");
        throw AuthException.validationError("Refresh token is required");
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
        log.error("evt=refresh_token_error msg=no_result_returned");
        throw AuthException.invalidToken();
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

      log.info("evt=refresh_token_success");
      return response;

    } catch (AuthException e) {
      // Re-throw our custom auth exceptions - don't wrap them
      throw e;
    } catch (NotAuthorizedException e) {
      log.error("evt=refresh_token_unauthorized msg=invalid_refresh_token", e);
      throw AuthException.invalidToken();
    } catch (Exception e) {
      log.error("evt=refresh_token_system_error msg=unexpected_error", e);
      throw AuthException.cognitoError("System error during token refresh", e);
    }
  }

  @Override
  public void logout(String userId, String accessToken) {
    log.info("evt=logout_start userId={}", userId);
    try {
      if (accessToken == null || accessToken.trim().isEmpty()) {
        log.error("evt=logout_error userId={} msg=access_token_required", userId);
        throw AuthException.invalidToken();
      }

      // Global sign out - invalidates all tokens for the user
      log.debug("evt=logout_cognito_start userId={}", userId);
      GlobalSignOutRequest signOutRequest = new GlobalSignOutRequest().withAccessToken(accessToken);

      cognitoClient.globalSignOut(signOutRequest);
      log.info("evt=logout_success userId={}", userId);

    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.error("evt=logout_error userId={}", userId, e);
      throw AuthException.cognitoError("Logout failed", e);
    }
  }

  @Override
  public void changePassword(
      String userId, String accessToken, String oldPassword, String newPassword) {
    log.info("evt=change_password_start userId={}", userId);
    try {
      if (accessToken == null || accessToken.trim().isEmpty()) {
        log.error("evt=change_password_error userId={} msg=access_token_required", userId);
        throw AuthException.invalidToken();
      }

      if (oldPassword == null || oldPassword.trim().isEmpty()) {
        log.error("evt=change_password_error userId={} msg=old_password_required", userId);
        throw AuthException.validationError("Old password is required");
      }

      if (newPassword == null || newPassword.length() < 8) {
        log.error("evt=change_password_error userId={} msg=new_password_too_short", userId);
        throw AuthException.invalidPassword("New password must be at least 8 characters long");
      }

      log.debug("evt=change_password_cognito_start userId={}", userId);
      ChangePasswordRequest changePasswordRequest =
          new ChangePasswordRequest()
              .withAccessToken(accessToken)
              .withPreviousPassword(oldPassword)
              .withProposedPassword(newPassword);

      cognitoClient.changePassword(changePasswordRequest);
      log.info("evt=change_password_success userId={}", userId);

    } catch (AuthException e) {
      throw e;
    } catch (InvalidPasswordException e) {
      log.error("evt=change_password_error userId={} msg=invalid_password_format", userId, e);
      throw AuthException.invalidPassword("New password does not meet requirements");
    } catch (NotAuthorizedException e) {
      log.error("evt=change_password_error userId={} msg=invalid_current_password", userId, e);
      throw AuthException.invalidCredentials();
    } catch (Exception e) {
      log.error("evt=change_password_error userId={}", userId, e);
      throw AuthException.cognitoError("Password change failed", e);
    }
  }

  @Override
  public void forgotPassword(String login) {
    log.info("evt=forgot_password_start user={}", login);
    try {
      if (login == null || login.trim().isEmpty()) {
        log.error("evt=forgot_password_error msg=login_required");
        throw AuthException.validationError("Login (username, email, or phone) is required");
      }

      // ✅ Find user to get Cognito username
      log.debug("evt=forgot_password_lookup_user user={}", login);
      User user =
          userService
              .getUserByUsername(login)
              .or(() -> userService.getUserByEmail(login))
              .or(() -> userService.getUserByPhone(login))
              .orElseThrow(
                  () -> {
                    log.error("evt=forgot_password_user_not_found user={}", login);
                    return new AuthException("USER_NOT_FOUND", "User not found", 404);
                  });

      String cognitoUsername = user.getUsername();
      if (cognitoUsername == null || cognitoUsername.trim().isEmpty()) {
        log.error("evt=forgot_password_cognito_username_missing user={}", login);
        throw new AuthException("USER_NOT_FOUND", "User not found", 404);
      }

      log.debug(
          "evt=forgot_password_cognito_request user={} cognitoUsername={}", login, cognitoUsername);
      ForgotPasswordRequest forgotPasswordRequest =
          new ForgotPasswordRequest()
              .withClientId(clientId)
              .withUsername(cognitoUsername) // ✅ Use actual Cognito username
              .withSecretHash(
                  calculateSecretHash(cognitoUsername)); // ✅ Calculate with correct username

      cognitoClient.forgotPassword(forgotPasswordRequest);
      log.info("evt=forgot_password_success user={}", login);

    } catch (AuthException e) {
      // Re-throw our custom auth exceptions - don't wrap them
      throw e;
    } catch (UserNotFoundException e) {
      log.error("evt=forgot_password_cognito_user_not_found user={}", login, e);
      throw new AuthException("USER_NOT_FOUND", "User not found", 404);
    } catch (Exception e) {
      log.error("evt=forgot_password_system_error user={}", login, e);
      throw AuthException.cognitoError("System error during forgot password", e);
    }
  }

  @Override
  public void resetPassword(String login, String confirmationCode, String newPassword) {
    log.info("evt=reset_password_start user={}", login);
    try {
      if (login == null || login.trim().isEmpty()) {
        log.error("evt=reset_password_error msg=login_required");
        throw AuthException.validationError("Login (username, email, or phone) is required");
      }

      if (confirmationCode == null || confirmationCode.trim().isEmpty()) {
        log.error("evt=reset_password_error user={} msg=confirmation_code_required", login);
        throw AuthException.validationError("Confirmation code is required");
      }

      if (newPassword == null || newPassword.length() < 8) {
        log.error("evt=reset_password_error user={} msg=password_too_short", login);
        throw AuthException.invalidPassword("New password must be at least 8 characters long");
      }

      // ✅ Find user to get Cognito username
      log.debug("evt=reset_password_lookup_user user={}", login);
      User user =
          userService
              .getUserByUsername(login)
              .or(() -> userService.getUserByEmail(login))
              .or(() -> userService.getUserByPhone(login))
              .orElseThrow(
                  () -> {
                    log.error("evt=reset_password_user_not_found user={}", login);
                    return new AuthException("USER_NOT_FOUND", "User not found", 404);
                  });

      String cognitoUsername = user.getUsername();
      if (cognitoUsername == null || cognitoUsername.trim().isEmpty()) {
        log.error("evt=reset_password_cognito_username_missing user={}", login);
        throw new AuthException("USER_NOT_FOUND", "User not found", 404);
      }

      log.debug(
          "evt=reset_password_cognito_request user={} cognitoUsername={}", login, cognitoUsername);
      ConfirmForgotPasswordRequest confirmRequest =
          new ConfirmForgotPasswordRequest()
              .withClientId(clientId)
              .withUsername(cognitoUsername) // ✅ Use actual Cognito username
              .withConfirmationCode(confirmationCode)
              .withPassword(newPassword)
              .withSecretHash(
                  calculateSecretHash(cognitoUsername)); // ✅ Calculate with correct username

      cognitoClient.confirmForgotPassword(confirmRequest);
      log.info("evt=reset_password_success user={}", login);

    } catch (AuthException e) {
      // Re-throw our custom auth exceptions - don't wrap them
      throw e;
    } catch (InvalidPasswordException e) {
      log.error("evt=reset_password_invalid_password user={}", login, e);
      throw AuthException.invalidPassword("New password does not meet requirements");
    } catch (CodeMismatchException e) {
      log.error("evt=reset_password_invalid_code user={}", login, e);
      throw AuthException.invalidCode();
    } catch (ExpiredCodeException e) {
      log.error("evt=reset_password_expired_code user={}", login, e);
      throw AuthException.expiredCode();
    } catch (Exception e) {
      log.error("evt=reset_password_system_error user={}", login, e);
      throw AuthException.cognitoError("System error during password reset", e);
    }
  }
}
