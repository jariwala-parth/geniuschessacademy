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
import com.amazonaws.services.cognitoidp.model.ChallengeNameType;
import com.amazonaws.services.cognitoidp.model.ChangePasswordRequest;
import com.amazonaws.services.cognitoidp.model.CodeMismatchException;
import com.amazonaws.services.cognitoidp.model.ConfirmForgotPasswordRequest;
import com.amazonaws.services.cognitoidp.model.ExpiredCodeException;
import com.amazonaws.services.cognitoidp.model.ForgotPasswordRequest;
import com.amazonaws.services.cognitoidp.model.GlobalSignOutRequest;
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidp.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidp.model.RespondToAuthChallengeRequest;
import com.amazonaws.services.cognitoidp.model.RespondToAuthChallengeResult;
import com.amazonaws.services.cognitoidp.model.SignUpRequest;
import com.amazonaws.services.cognitoidp.model.SignUpResult;
import com.amazonaws.services.cognitoidp.model.UserNotConfirmedException;
import com.amazonaws.services.cognitoidp.model.UserNotFoundException;
import com.amazonaws.services.cognitoidp.model.UsernameExistsException;
import com.pjariwala.dto.AuthChallengeRequest;
import com.pjariwala.dto.AuthChallengeResponse;
import com.pjariwala.dto.AuthRequest;
import com.pjariwala.dto.AuthResponse;
import com.pjariwala.dto.LoginResult;
import com.pjariwala.dto.SignupRequest;
import com.pjariwala.dto.UserInfo;
import com.pjariwala.exception.AuthException;
import com.pjariwala.model.User;
import com.pjariwala.service.ActivityLogService;
import com.pjariwala.service.AuthService;
import com.pjariwala.service.UserService;
import com.pjariwala.util.AuthUtil;
import com.pjariwala.util.JwtUtil;
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
  @Autowired private ActivityLogService activityLogService;
  @Autowired private AuthUtil authUtil;
  @Autowired private JwtUtil jwtUtil;

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

  @Override
  public AuthResponse signup(SignupRequest signupRequest) {
    log.info(
        "Starting user signup process for email: {} with userType: {}",
        signupRequest.getEmail(),
        signupRequest.getUserType());
    try {
      // Validation is handled by @NotEmpty annotations in the DTO

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

      // Note: isAdmin field removed - use userType instead
      // No coach-specific attributes needed

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
      user.setUsername(signupRequest.getUsername());
      user.setCognitoSub(signUpResult.getUserSub());
      user.setIsActive(true);
      user.setCreatedAt(LocalDateTime.now());
      user.setUpdatedAt(LocalDateTime.now());

      if ("STUDENT".equals(signupRequest.getUserType())) {
        user.setGuardianName(signupRequest.getGuardianName());
        user.setGuardianPhone(signupRequest.getGuardianPhone());
        user.setJoiningDate(LocalDateTime.now());
      } else if ("COACH".equals(signupRequest.getUserType())) {
        // Note: isAdmin field removed - use userType instead
        // COACH users are not admin by default
      }

      log.debug("Creating user record in our system for email: {}", signupRequest.getEmail());
      userService.createUser(user);
      log.info(
          "User record created successfully in our system with userId: {} for email: {}",
          user.getUserId(),
          signupRequest.getEmail());

      // Auto-login after successful signup
      log.info("Auto-login after successful signup for email: {}", signupRequest.getEmail());
      LoginResult loginResult =
          login(new AuthRequest(signupRequest.getUsername(), signupRequest.getPassword()));

      if (loginResult.isChallenge()) {
        log.warn(
            "Challenge required during auto-login after signup for email: {}",
            signupRequest.getEmail());
        throw AuthException.challengeRequired(
            "NEW_PASSWORD_REQUIRED", loginResult.getChallengeResponse().getSession());
      }

      log.info("Signup process completed successfully for email: {}", signupRequest.getEmail());
      return loginResult.getAuthResponse();

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
  public LoginResult login(AuthRequest authRequest) {
    log.info("Starting secure login process for user: {}", authRequest.getLogin());
    try {
      // STEP 1: Prepare the authentication request for Cognito's admin flow
      // Cognito can use username, email, or phone as the 'USERNAME'
      Map<String, String> authParameters = new HashMap<>();
      authParameters.put("USERNAME", authRequest.getLogin());
      authParameters.put("PASSWORD", authRequest.getPassword());
      // Add SECRET_HASH for admin flow
      authParameters.put("SECRET_HASH", calculateSecretHash(authRequest.getLogin()));

      AdminInitiateAuthRequest initiateAuthRequest =
          new AdminInitiateAuthRequest()
              .withUserPoolId(userPoolId)
              .withClientId(clientId)
              .withAuthFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
              .withAuthParameters(authParameters);

      // STEP 2: Initiate authentication with Cognito using admin flow
      AdminInitiateAuthResult authResult = cognitoClient.adminInitiateAuth(initiateAuthRequest);

      // STEP 3: Handle authentication challenges (e.g., NEW_PASSWORD_REQUIRED)
      if (authResult.getChallengeName() != null) {
        log.info(
            "Authentication challenge required for user: {} - Challenge: {}",
            authRequest.getLogin(),
            authResult.getChallengeName());

        // Handle NEW_PASSWORD_REQUIRED challenge
        if (ChallengeNameType.NEW_PASSWORD_REQUIRED.name().equals(authResult.getChallengeName())) {
          log.info("NEW_PASSWORD_REQUIRED challenge for user: {}", authRequest.getLogin());

          // Return challenge response using wrapper
          return LoginResult.forChallenge(
              new AuthChallengeResponse(authResult.getChallengeName(), authResult.getSession()));
        }

        // Handle other challenges if needed in the future
        throw new RuntimeException(
            "Unsupported authentication challenge: " + authResult.getChallengeName());
      }

      // STEP 4: Get authentication result and build the response
      AuthenticationResultType authenticationResult = authResult.getAuthenticationResult();

      if (authenticationResult == null) {
        log.error(
            "Authentication failed - no result returned from Cognito for user: {}",
            authRequest.getLogin());
        throw new RuntimeException("Authentication failed - no result returned");
      }

      log.info("Cognito authentication successful for user: {}", authRequest.getLogin());

      // Extract cognitoSub from ID token to get user info
      String cognitoSub = authUtil.validateTokenAndGetCognitoSub(authenticationResult.getIdToken());

      // Fetch user info from our database
      User user =
          userService
              .getUserByCognitoSub(cognitoSub)
              .orElseThrow(
                  () -> {
                    log.error("evt=login_user_not_found cognitoSub={}", cognitoSub);
                    return AuthException.invalidCredentials();
                  });

      // Build response with JWT tokens from Cognito and user info
      AuthResponse response = new AuthResponse();
      response.setAccessToken(authenticationResult.getAccessToken());
      response.setRefreshToken(authenticationResult.getRefreshToken());
      response.setIdToken(authenticationResult.getIdToken());
      response.setTokenType(authenticationResult.getTokenType());
      response.setExpiresIn(authenticationResult.getExpiresIn());

      // Include basic user info (without userType for multi-tenancy)
      UserInfo userInfo = new UserInfo();
      userInfo.setUserId(user.getUserId());
      userInfo.setEmail(user.getEmail());
      userInfo.setName(user.getName());
      userInfo.setPhoneNumber(user.getPhoneNumber());
      response.setUserInfo(userInfo);

      log.info("Login process completed successfully for user: {}", authRequest.getLogin());
      return LoginResult.forSuccess(response);

    } catch (NotAuthorizedException | UserNotConfirmedException | UserNotFoundException e) {
      log.error("Authentication failed: ", e);
      // Map these exceptions to a generic invalid credentials error to avoid
      // telling an attacker if a user exists or not.
      throw AuthException.invalidCredentials();
    } catch (Exception e) {
      log.error("System error during login: ", e);
      throw AuthException.cognitoError("System error during login", e);
    }
  }

  @Override
  public AuthResponse respondChallenge(AuthChallengeRequest challengeRequest) {
    log.info("Responding to authentication challenge");
    try {
      // Validation is handled by @NotEmpty annotations in the DTO

      // Prepare challenge response parameters
      Map<String, String> challengeResponses = new HashMap<>();
      challengeResponses.put("NEW_PASSWORD", challengeRequest.getNewPassword());
      challengeResponses.put("USERNAME", challengeRequest.getUsername());
      // Add SECRET_HASH for admin flow
      challengeResponses.put("SECRET_HASH", calculateSecretHash(challengeRequest.getUsername()));

      // Respond to the NEW_PASSWORD_REQUIRED challenge
      RespondToAuthChallengeRequest respondRequest =
          new RespondToAuthChallengeRequest()
              .withClientId(clientId)
              .withChallengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
              .withSession(challengeRequest.getSession())
              .withChallengeResponses(challengeResponses);

      RespondToAuthChallengeResult result = cognitoClient.respondToAuthChallenge(respondRequest);

      // Check if challenge was successful
      if (result.getChallengeName() != null) {
        log.error(
            "Challenge response failed - additional challenge required: {}",
            result.getChallengeName());
        throw new RuntimeException("Challenge response failed - additional challenge required");
      }

      // Get authentication result
      AuthenticationResultType authenticationResult = result.getAuthenticationResult();
      if (authenticationResult == null) {
        log.error("Challenge response failed - no authentication result returned");
        throw new RuntimeException("Challenge response failed - no authentication result returned");
      }

      log.info("Authentication challenge completed successfully");

      // Extract cognitoSub from ID token to get user info
      String cognitoSub = authUtil.validateTokenAndGetCognitoSub(authenticationResult.getIdToken());

      // Fetch user info from our database
      User user =
          userService
              .getUserByCognitoSub(cognitoSub)
              .orElseThrow(
                  () -> {
                    log.error("evt=challenge_user_not_found cognitoSub={}", cognitoSub);
                    return AuthException.invalidCredentials();
                  });

      // Build response with JWT tokens from Cognito and user info
      AuthResponse response = new AuthResponse();
      response.setAccessToken(authenticationResult.getAccessToken());
      response.setRefreshToken(authenticationResult.getRefreshToken());
      response.setIdToken(authenticationResult.getIdToken());
      response.setTokenType(authenticationResult.getTokenType());
      response.setExpiresIn(authenticationResult.getExpiresIn());

      // Include basic user info (without userType for multi-tenancy)
      UserInfo userInfo = new UserInfo();
      userInfo.setUserId(user.getUserId());
      userInfo.setEmail(user.getEmail());
      userInfo.setName(user.getName());
      userInfo.setPhoneNumber(user.getPhoneNumber());
      response.setUserInfo(userInfo);

      return response;

    } catch (InvalidPasswordException e) {
      log.error("Invalid password in challenge response: ", e);
      throw AuthException.invalidPassword("Password does not meet requirements");
    } catch (Exception e) {
      log.error("System error during challenge response: ", e);
      throw AuthException.cognitoError("System error during challenge response", e);
    }
  }

  @Override
  public AuthResponse refreshToken(String refreshToken, String username) {
    log.info("evt=refresh_token_start");
    try {
      if (refreshToken == null || refreshToken.trim().isEmpty()) {
        log.error("evt=refresh_token_error msg=refresh_token_required");
        throw AuthException.validationError("Refresh token is required");
      }

      log.info("evt=refresh_token_oauth2_start userPoolId={} clientId={}", userPoolId, clientId);

      // Use InitiateAuth for refresh tokens (supports rotation better than AdminInitiateAuth)
      Map<String, String> authParameters = new HashMap<>();
      authParameters.put("REFRESH_TOKEN", refreshToken);

      // Validate username parameter
      if (username == null || username.trim().isEmpty()) {
        log.error("evt=refresh_token_error msg=username_required_for_secret_hash");
        throw AuthException.validationError("Username is required for SECRET_HASH calculation");
      }

      // Add SECRET_HASH if client has secret
      if (clientSecret != null && !clientSecret.trim().isEmpty()) {
        authParameters.put("SECRET_HASH", calculateSecretHash(username));
        authParameters.put("REFRESH_TOKEN", refreshToken);
        log.debug("evt=refresh_token_secret_hash_added username={}", username);
      }

      log.info(
          "evt=refresh_token_initiate_auth client_has_secret={} userPoolId={} clientId={}",
          clientSecret != null && !clientSecret.trim().isEmpty(),
          userPoolId,
          clientId);

      // Use InitiateAuth (works better with token rotation)
      InitiateAuthRequest initiateAuthRequest =
          new InitiateAuthRequest()
              .withClientId(clientId)
              .withAuthFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
              .withAuthParameters(authParameters);

      InitiateAuthResult authResult = cognitoClient.initiateAuth(initiateAuthRequest);
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

      // Set refresh token - it may or may not be rotated depending on Cognito config
      if (authenticationResult.getRefreshToken() != null) {
        response.setRefreshToken(authenticationResult.getRefreshToken());
        log.info("evt=refresh_token_success msg=new_refresh_token_received");
      } else {
        response.setRefreshToken(refreshToken); // Keep the same refresh token
        log.info("evt=refresh_token_success msg=refresh_token_not_rotated");
      }

      log.info(
          "evt=refresh_token_success access_token_length={}",
          response.getAccessToken() != null ? response.getAccessToken().length() : 0);
      return response;

    } catch (AuthException e) {
      // Re-throw our custom auth exceptions - don't wrap them
      throw e;
    } catch (NotAuthorizedException e) {
      log.error(
          "evt=refresh_token_unauthorized msg=invalid_refresh_token error={}", e.getMessage(), e);
      throw AuthException.invalidToken();
    } catch (Exception e) {
      log.error("evt=refresh_token_system_error msg=unexpected_error error={}", e.getMessage(), e);
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

      //      // Log successful logout activity
      //      try {
      //        User user = userService.getUserById(userId).orElse(null);
      //        if (user != null) {
      //          activityLogService.logLogout(
      //              userId,
      //              user.getName(),
      //              "COACH".equals(user.getUserType()) ? UserType.COACH : UserType.STUDENT);
      //        }
      //      } catch (Exception e) {
      //        log.warn("Failed to log logout activity for user: {}", userId, e);
      //      }

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
              .withUsername(cognitoUsername)
              .withSecretHash(calculateSecretHash(cognitoUsername));

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

  @Override
  public UserInfo addStudent(SignupRequest signupRequest, String coachId) {
    log.info(
        "Starting add student process for email: {} by coach: {}",
        signupRequest.getEmail(),
        coachId);
    try {
      // Force user type to STUDENT
      signupRequest.setUserType("STUDENT");

      // Validation is handled by @NotEmpty annotations in the DTO
      // Note: Guardian fields are optional in the DTO but required for students
      // This validation is still needed for business logic
      if (signupRequest.getGuardianName() == null
          || signupRequest.getGuardianName().trim().isEmpty()) {
        log.error(
            "Add student validation failed: Guardian name is required for email: {}",
            signupRequest.getEmail());
        throw AuthException.validationError("Guardian name is required for students");
      }

      if (signupRequest.getGuardianPhone() == null
          || signupRequest.getGuardianPhone().trim().isEmpty()) {
        log.error(
            "Add student validation failed: Guardian phone is required for email: {}",
            signupRequest.getEmail());
        throw AuthException.validationError("Guardian phone is required for students");
      }

      // Check if user already exists in our system
      if (userService.userExistsByEmail(signupRequest.getEmail())) {
        log.error(
            "Add student failed: User already exists with email: {}", signupRequest.getEmail());
        throw AuthException.userExists();
      }

      // Create user in Cognito User Pool
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
                  new AttributeType().withName("custom:user_type").withValue("STUDENT"),
                  new AttributeType()
                      .withName("custom:guardian_name")
                      .withValue(signupRequest.getGuardianName()),
                  new AttributeType()
                      .withName("custom:guardian_phone")
                      .withValue(signupRequest.getGuardianPhone()));

      log.debug("Creating student in Cognito User Pool for email: {}", signupRequest.getEmail());
      SignUpResult signUpResult = cognitoClient.signUp(cognitoSignupRequest);
      log.info(
          "Student created in Cognito successfully with sub: {} for email: {}",
          signUpResult.getUserSub(),
          signupRequest.getEmail());

      // Auto-confirm the user
      log.debug("Auto-confirming student in Cognito for email: {}", signupRequest.getEmail());
      AdminConfirmSignUpRequest confirmRequest =
          new AdminConfirmSignUpRequest()
              .withUserPoolId(userPoolId)
              .withUsername(signupRequest.getUsername());

      cognitoClient.adminConfirmSignUp(confirmRequest);
      log.info("Student auto-confirmed in Cognito for email: {}", signupRequest.getEmail());

      // Create user record in our system
      User user = new User();
      user.setUserId(userService.generateUserId());
      user.setUserType("STUDENT");
      user.setEmail(signupRequest.getEmail());
      user.setName(signupRequest.getName());
      user.setPhoneNumber(signupRequest.getPhoneNumber());
      user.setUsername(signupRequest.getUsername());
      user.setCognitoSub(signUpResult.getUserSub());
      user.setIsActive(true);
      user.setCreatedAt(LocalDateTime.now());
      user.setUpdatedAt(LocalDateTime.now());
      user.setGuardianName(signupRequest.getGuardianName());
      user.setGuardianPhone(signupRequest.getGuardianPhone());
      user.setJoiningDate(LocalDateTime.now());

      log.debug("Creating student record in our system for email: {}", signupRequest.getEmail());
      userService.createUser(user);
      log.info(
          "Student record created successfully in our system with userId: {} for email: {} by"
              + " coach: {}",
          user.getUserId(),
          signupRequest.getEmail(),
          coachId);

      // Return student info (no auto-login for student creation)
      UserInfo studentInfo = new UserInfo();
      studentInfo.setUserId(user.getUserId());
      studentInfo.setEmail(user.getEmail());
      studentInfo.setName(user.getName());
      studentInfo.setPhoneNumber(user.getPhoneNumber());

      log.info(
          "Add student process completed successfully for email: {} by coach: {}",
          signupRequest.getEmail(),
          coachId);

      // Log student creation activity
      try {
        User coach = userService.getUserById(coachId).orElse(null);
        if (coach != null) {
          activityLogService.logStudentCreation(
              coachId,
              coach.getName(),
              user.getUserId(),
              user.getName(),
              coach.getOrganizationId());
        }
      } catch (Exception e) {
        log.warn("Failed to log student creation activity for student: {}", user.getUserId(), e);
      }

      return studentInfo;

    } catch (AuthException e) {
      log.error(
          "evt=add_student_auth_error email={} coach={} code={} msg={}",
          signupRequest.getEmail(),
          coachId,
          e.getErrorCode(),
          e.getMessage());
      throw e;
    } catch (UsernameExistsException e) {
      log.error(
          "evt=add_student_username_exists email={} coach={} msg=username_already_exists",
          signupRequest.getEmail(),
          coachId,
          e);
      throw AuthException.userExists();
    } catch (InvalidPasswordException e) {
      log.error(
          "evt=add_student_invalid_password email={} coach={} msg=password_requirements_not_met",
          signupRequest.getEmail(),
          coachId,
          e);
      throw AuthException.invalidPassword("Password does not meet requirements");
    } catch (Exception e) {
      log.error(
          "evt=add_student_system_error email={} coach={} msg=unexpected_system_error",
          signupRequest.getEmail(),
          coachId,
          e);
      throw AuthException.cognitoError("System error during student creation", e);
    }
  }
}
