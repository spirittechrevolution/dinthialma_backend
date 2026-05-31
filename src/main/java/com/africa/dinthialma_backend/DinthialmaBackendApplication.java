package com.africa.dinthialma_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DinthialmaBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(DinthialmaBackendApplication.class, args);
  }
}
