package com.pjariwala.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RestController
@EnableWebMvc
public class PingController {
  @RequestMapping(path = "/ping", method = RequestMethod.GET)
  public Map<String, String> ping() {
    Map<String, String> pong = new HashMap<>();
    pong.put("pong", "Hello, World!");
    return pong;
  }
}
