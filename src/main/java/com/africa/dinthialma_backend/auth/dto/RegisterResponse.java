package com.africa.dinthialma_backend.auth.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {

  private UUID userId;
  private String keycloakId;
  private String phone;
  private String firstName;
  private String lastName;
  private String email;
  private String role;
}
