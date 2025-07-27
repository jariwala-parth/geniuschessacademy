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
// We use direct @Import instead of @ComponentScan to speed up cold starts
// @ComponentScan(basePackages = "com.pjariwala.controller")
@Import({
  PingController.class,
  AuthController.class,
  AuthServiceImpl.class,
  UserServiceImpl.class,
  JwtUtil.class,
  LocalDateTimeConverter.class
})
public class GeniusChessAcademyApplication {

  public static void main(String[] args) {
    SpringApplication.run(GeniusChessAcademyApplication.class, args);
  }
}
