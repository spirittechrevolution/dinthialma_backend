package com.africa.dinthialma_backend.auth.repository;

import com.africa.dinthialma_backend.auth.entity.OtpVerification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

  /** Dernier OTP non encore consommé (pour envoi / vérification du code). */
  Optional<OtpVerification> findTopByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(
      String phone, String purpose);

  /**
   * Dernier OTP déjà vérifié mais pas encore consommé. Utilisé lors de la complétion du flux
   * (inscription, reset password/pin).
   */
  Optional<OtpVerification> findTopByPhoneAndPurposeAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(
      String phone, String purpose);

  /** Supprime tous les OTP d'un numéro pour un objectif précis (invalider les anciens). */
  void deleteAllByPhoneAndPurpose(String phone, String purpose);

  /** Supprime tous les OTP d'un numéro (nettoyage complet lors de la suppression du compte). */
  void deleteAllByPhone(String phone);
}
