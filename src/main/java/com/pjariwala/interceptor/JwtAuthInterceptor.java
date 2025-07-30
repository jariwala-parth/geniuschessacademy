package com.pjariwala.interceptor;

import com.pjariwala.model.User;
import com.pjariwala.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtAuthInterceptor implements HandlerInterceptor {

  @Autowired private AuthUtil authUtil;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String requestURI = request.getRequestURI();
    String method = request.getMethod();

    log.debug("evt=jwt_interceptor_request uri={} method={}", requestURI, method);

    // Skip authentication for public endpoints
    if (isPublicEndpoint(requestURI, method)) {
      log.debug("evt=jwt_interceptor_skip_public uri={}", requestURI);
      return true;
    }

    // Get Authorization header
    String authorization = request.getHeader("Authorization");
    if (authorization == null || authorization.isEmpty()) {
      log.warn("evt=jwt_interceptor_missing_token uri={}", requestURI);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response
          .getWriter()
          .write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Authorization token required\"}");
      return false;
    }

    try {
      // Validate JWT and get current user
      User currentUser = authUtil.getCurrentUser(authorization);

      // Store user info and access token in request attributes for controller/service access
      request.setAttribute("userId", currentUser.getUserId());
      request.setAttribute("accessToken", authUtil.extractAccessToken(authorization));

      log.debug(
          "evt=jwt_interceptor_success uri={} userId={} userType={}",
          requestURI,
          currentUser.getUserId(),
          currentUser.getUserType());
      return true;

    } catch (Exception e) {
      log.warn("evt=jwt_interceptor_invalid_token uri={} error={}", requestURI, e.getMessage());
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response
          .getWriter()
          .write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid or expired token\"}");
      return false;
    }
  }

  private boolean isPublicEndpoint(String uri, String method) {
    // Public endpoints that don't require authentication
    return uri.equals("/ping")
        || uri.equals("/api-info")
        || uri.startsWith("/swagger-ui")
        || uri.startsWith("/v3/api-docs")
        || uri.startsWith("/actuator")
        || (uri.equals("/api/v1/auth/login") && "POST".equals(method));
  }
}
