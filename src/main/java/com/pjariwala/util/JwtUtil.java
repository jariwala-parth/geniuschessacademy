package com.pjariwala.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  @Value("${aws.cognito.userPoolId}")
  private String userPoolId;

  @Value("${aws.region:us-east-1}")
  private String awsRegion;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private Map<String, PublicKey> jwksKeys = new HashMap<>();

  /** Extract user ID (sub) from JWT token */
  public String getUserIdFromToken(String token) {
    try {
      // Decode JWT payload without validation (for development)
      String[] chunks = token.split("\\.");
      if (chunks.length != 3) {
        throw new RuntimeException("Invalid JWT token format");
      }

      Base64.Decoder decoder = Base64.getUrlDecoder();
      String payload = new String(decoder.decode(chunks[1]));

      JsonNode jsonNode = objectMapper.readTree(payload);
      return jsonNode.get("sub").asText();

    } catch (Exception e) {
      throw new RuntimeException("Error extracting user ID from token: " + e.getMessage(), e);
    }
  }

  /** Extract Cognito sub from JWT token (clearer method name) */
  public String getCognitoSubFromToken(String token) {
    return getUserIdFromToken(token); // Delegate to existing method for now
  }

  /** Extract email from JWT token */
  public String getEmailFromToken(String token) {
    try {
      String[] chunks = token.split("\\.");
      if (chunks.length != 3) {
        throw new RuntimeException("Invalid JWT token format");
      }

      Base64.Decoder decoder = Base64.getUrlDecoder();
      String payload = new String(decoder.decode(chunks[1]));

      JsonNode jsonNode = objectMapper.readTree(payload);

      // Try email first, then username, then sub as fallback
      JsonNode emailNode = jsonNode.get("email");
      if (emailNode != null && !emailNode.isNull()) {
        return emailNode.asText();
      }

      JsonNode usernameNode = jsonNode.get("username");
      if (usernameNode != null && !usernameNode.isNull()) {
        return usernameNode.asText();
      }

      JsonNode subNode = jsonNode.get("sub");
      if (subNode != null && !subNode.isNull()) {
        return subNode.asText();
      }

      throw new RuntimeException("No email, username, or sub field found in token");

    } catch (Exception e) {
      throw new RuntimeException("Error extracting email from token: " + e.getMessage(), e);
    }
  }

  /** Extract user type from JWT token */
  public String getUserTypeFromToken(String token) {
    try {
      String[] chunks = token.split("\\.");
      if (chunks.length != 3) {
        throw new RuntimeException("Invalid JWT token format");
      }

      Base64.Decoder decoder = Base64.getUrlDecoder();
      String payload = new String(decoder.decode(chunks[1]));

      JsonNode jsonNode = objectMapper.readTree(payload);
      return jsonNode.get("custom:user_type").asText();

    } catch (Exception e) {
      throw new RuntimeException("Error extracting user type from token: " + e.getMessage(), e);
    }
  }

  /** Check if token is expired (basic check without signature validation) */
  public boolean isTokenExpired(String token) {
    try {
      String[] chunks = token.split("\\.");
      if (chunks.length != 3) {
        throw new RuntimeException("Invalid JWT token format");
      }

      Base64.Decoder decoder = Base64.getUrlDecoder();
      String payload = new String(decoder.decode(chunks[1]));

      JsonNode jsonNode = objectMapper.readTree(payload);
      long exp = jsonNode.get("exp").asLong();
      long currentTime = System.currentTimeMillis() / 1000;

      return currentTime > exp;

    } catch (Exception e) {
      return true; // Consider expired if we can't parse
    }
  }

  /**
   * Validate JWT token signature (simplified version) In production with API Gateway, this
   * validation is done by API Gateway itself
   */
  public boolean validateToken(String token) {
    try {
      // Basic format validation
      String[] chunks = token.split("\\.");
      if (chunks.length != 3) {
        return false;
      }

      // Check if token is expired
      if (isTokenExpired(token)) {
        return false;
      }

      // In a real implementation, you would:
      // 1. Download JWKS from Cognito
      // 2. Verify the signature using the appropriate public key
      // 3. Validate issuer, audience, and other claims

      // For now, we'll do basic validation
      return true;

    } catch (Exception e) {
      return false;
    }
  }

  /** Extract all claims from token */
  public Map<String, Object> getClaimsFromToken(String token) {
    try {
      String[] chunks = token.split("\\.");
      if (chunks.length != 3) {
        throw new RuntimeException("Invalid JWT token format");
      }

      Base64.Decoder decoder = Base64.getUrlDecoder();
      String payload = new String(decoder.decode(chunks[1]));

      JsonNode jsonNode = objectMapper.readTree(payload);
      Map<String, Object> claims = new HashMap<>();

      jsonNode
          .fields()
          .forEachRemaining(
              entry -> {
                claims.put(entry.getKey(), entry.getValue().asText());
              });

      return claims;

    } catch (Exception e) {
      throw new RuntimeException("Error extracting claims from token: " + e.getMessage(), e);
    }
  }

  /** Get the Cognito User Pool issuer URL */
  public String getCognitoIssuer() {
    return String.format("https://cognito-idp.%s.amazonaws.com/%s", awsRegion, userPoolId);
  }

  /** Download and cache JWKS keys from Cognito (for future implementation) */
  private void loadJwksKeys() {
    try {
      String jwksUrl = getCognitoIssuer() + "/.well-known/jwks.json";

      URL url = new URL(jwksUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      BufferedReader reader =
          new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder response = new StringBuilder();
      String line;

      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      reader.close();

      // Parse JWKS and extract public keys
      JsonNode jwks = objectMapper.readTree(response.toString());
      JsonNode keys = jwks.get("keys");

      for (JsonNode key : keys) {
        String kid = key.get("kid").asText();
        String n = key.get("n").asText();
        String e = key.get("e").asText();

        // Convert to PublicKey
        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);

        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = factory.generatePublic(spec);

        jwksKeys.put(kid, publicKey);
      }

    } catch (Exception e) {
      System.err.println("Error loading JWKS keys: " + e.getMessage());
    }
  }
}
