package com.pjariwala.config;

import com.pjariwala.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired private JwtAuthInterceptor jwtAuthInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(jwtAuthInterceptor)
        .addPathPatterns("/api/**") // Apply to all API endpoints
        .excludePathPatterns(
            "/api/v1/auth/login", // Allow login without auth
            "/swagger-ui/**",
            "/v3/api-docs/**");
  }
}
