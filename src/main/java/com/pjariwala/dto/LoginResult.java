package com.pjariwala.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper class for login results that can represent either a successful login or a challenge
 * response (e.g., NEW_PASSWORD_REQUIRED)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {

  private AuthResponse authResponse;
  private AuthChallengeResponse challengeResponse;
  private boolean isChallengeRequired;

  /** Create a successful login result */
  public static LoginResult forSuccess(AuthResponse authResponse) {
    return new LoginResult(authResponse, null, false);
  }

  /** Create a challenge required result */
  public static LoginResult forChallenge(AuthChallengeResponse challengeResponse) {
    return new LoginResult(null, challengeResponse, true);
  }

  /** Check if this is a successful login result */
  public boolean isSuccess() {
    return !isChallengeRequired && authResponse != null;
  }

  /** Check if this is a challenge result */
  public boolean isChallenge() {
    return isChallengeRequired && challengeResponse != null;
  }
}
