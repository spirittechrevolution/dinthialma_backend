package com.africa.dinthialma_backend.auth.entity;

import com.africa.dinthialma_backend.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** OTP de vérification – utilisé pour l'inscription et la réinitialisation du mot de passe. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "otp_verifications", schema = "dinthialma")
public class OtpVerification extends BaseEntity {

  @Column(name = "phone", nullable = false)
  private String phone;

  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "purpose", nullable = false)
  private String purpose; // REGISTRATION | RESET_PASSWORD

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "used", nullable = false)
  private boolean used = false;

  /** Indique si cet OTP a été vérifié (mais pas encore consommé). */
  @Column(name = "verified", nullable = false)
  private boolean verified = false;

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  public boolean isValid() {
    return !used && !isExpired();
  }
}
