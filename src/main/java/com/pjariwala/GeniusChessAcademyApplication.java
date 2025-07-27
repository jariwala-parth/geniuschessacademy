package com.pjariwala;

import com.pjariwala.controller.AuthController;
import com.pjariwala.controller.PingController;
import com.pjariwala.service.impl.AuthServiceImpl;
import com.pjariwala.service.impl.UserServiceImpl;
import com.pjariwala.util.JwtUtil;
import com.pjariwala.util.LocalDateTimeConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
  PingController.class,
  AuthController.class,
  AuthServiceImpl.class,
  UserServiceImpl.class,
  JwtUtil.class,
  LocalDateTimeConverter.class,
  // Batch APIs
  com.pjariwala.controller.BatchController.class,
  com.pjariwala.service.impl.BatchServiceImpl.class,
  com.pjariwala.util.LocalDateConverter.class,
  com.pjariwala.util.LocalTimeConverter.class,
  // Enrollment APIs
  com.pjariwala.controller.EnrollmentController.class,
  com.pjariwala.service.impl.EnrollmentServiceImpl.class,
  // Configuration
  com.pjariwala.config.SwaggerConfig.class,
  com.pjariwala.config.WebConfig.class,
  // Interceptors
  com.pjariwala.interceptor.JwtAuthInterceptor.class,
  // Utils
  com.pjariwala.util.AuthUtil.class
})
public class GeniusChessAcademyApplication {

  public static void main(String[] args) {
    SpringApplication.run(GeniusChessAcademyApplication.class, args);
  }
}
