package com.africa.dinthialma_backend.auth.entity;

import com.africa.dinthialma_backend.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Demande de changement de numéro de téléphone en attente de vérification OTP. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "phone_change_requests", schema = "dinthialma")
public class PhoneChangeRequestEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "old_phone", nullable = false)
  private String oldPhone;

  @Column(name = "new_phone", nullable = false)
  private String newPhone;

  @Column(name = "otp_code", nullable = false)
  private String otpCode;

  @Column(name = "verified", nullable = false)
  private boolean verified = false;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }
}
