package com.africa.dinthialma_backend.support;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.codeList.CycleStatut;
import com.africa.dinthialma_backend.tontine.codeList.ModeCycle;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Entités prêtes à l'emploi pour les tests unitaires (identifiants aléatoires). */
public final class Fixtures {

  private Fixtures() {}

  public static User user(String prenom) {
    return User.builder()
        .id(UUID.randomUUID())
        .keycloakId(UUID.randomUUID().toString())
        .firstName(prenom)
        .lastName("Test")
        .phone("22177" + (1_000_000 + (int) (Math.random() * 8_999_999)))
        .active(true)
        .accountStatus(AccountStatus.ACTIVE)
        .createdAt(LocalDateTime.now())
        .build();
  }

  /** Tontine ROTATIVE ACTIVE, 1000 F / mois, ordre ROTATION, 5 membres attendus. */
  public static Tontine tontine(User createur) {
    return Tontine.builder()
        .id(UUID.randomUUID())
        .nom("Tontine test")
        .tontineType(TontineType.ROTATIVE)
        .montant(new BigDecimal("1000"))
        .frequence("MENSUEL")
        .ordreBeneficiaire("ROTATION")
        .modeCycle(ModeCycle.MANUEL)
        .dateDebut(LocalDate.now())
        .nombreMembres(5)
        .nombreGagnants(1)
        .statut(TontineStatut.ACTIVE)
        .creePar(createur)
        .createdAt(LocalDateTime.now())
        .build();
  }

  public static TontineMembre membre(Tontine tontine, User user, int ordre) {
    return TontineMembre.builder()
        .id(UUID.randomUUID())
        .tontine(tontine)
        .user(user)
        .ordreJackpot(ordre)
        .statut(MembreStatut.ACTIF)
        .dateAdhesion(LocalDate.now())
        .createdAt(LocalDateTime.now())
        .build();
  }

  public static CycleTontine cycle(Tontine tontine, int numero, CycleStatut statut) {
    return CycleTontine.builder()
        .id(UUID.randomUUID())
        .tontine(tontine)
        .numeroCycle(numero)
        .dateDebut(LocalDate.now())
        .dateFin(LocalDate.now().plusDays(30))
        .statut(statut)
        .createdAt(LocalDateTime.now())
        .build();
  }

  public static Cotisation cotisation(
      TontineMembre membre, CycleTontine cycle, CotisationStatut statut) {
    return Cotisation.builder()
        .id(UUID.randomUUID())
        .tontine(membre.getTontine())
        .membre(membre)
        .cycle(cycle)
        .montant(new BigDecimal("1000"))
        .methodePaiement("WAVE")
        .statut(statut)
        .enregistrePar(membre.getUser())
        .createdAt(LocalDateTime.now())
        .build();
  }
}
