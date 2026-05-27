package com.africa.dinthialma_backend.auth.repository;

import com.africa.dinthialma_backend.auth.entity.PhoneChangeRequestEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneChangeRequestRepository
    extends JpaRepository<PhoneChangeRequestEntity, UUID> {

  /** Dernière demande non vérifiée pour ce nouveau numéro. */
  Optional<PhoneChangeRequestEntity> findTopByUserIdAndNewPhoneAndVerifiedFalseOrderByCreatedAtDesc(
      UUID userId, String newPhone);

  /** Supprimer toutes les demandes d'un utilisateur (avant d'en créer une nouvelle). */
  void deleteAllByUserId(UUID userId);
}
