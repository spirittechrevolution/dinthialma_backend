package com.africa.dinthialma_backend.tontine.mapper;

import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.tontine.codeList.StatutEcheance;
import com.africa.dinthialma_backend.tontine.dto.BeneficiaireHistoriqueResponse;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse.MembreDistributionInfo;
import com.africa.dinthialma_backend.tontine.dto.EcheanceResponse;
import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Conversions des cycles (tours) et de l'historique des bénéficiaires. */
@Mapper
public interface CycleMapper {

  @Mapping(target = "tontineId", source = "tontine.id")
  @Mapping(target = "gagnants", source = "gagnants", qualifiedByName = "gagnantsActifs")
  @Mapping(target = "distributionParMembre", ignore = true)
  @Mapping(target = "echeances", source = "echeances", qualifiedByName = "echeancesActives")
  @Mapping(target = "nombreEcheances", source = "echeances", qualifiedByName = "nombreEcheances")
  @Mapping(target = "echeanceEnCours", source = "echeances", qualifiedByName = "echeanceEnCours")
  CycleResponse toResponse(CycleTontine cycle);

  /** Échéance avec son statut calculé à l'instant {@code maintenant} (heure de Dakar). */
  default EcheanceResponse toEcheance(EcheanceCycle echeance, LocalDateTime maintenant) {
    return EcheanceResponse.builder()
        .id(echeance.getId())
        .numero(echeance.getNumero())
        .dateDebut(echeance.getDateDebut())
        .echeanceAt(Fuseau.versOffset(echeance.getEcheanceAt()))
        .statut(StatutEcheance.de(echeance, maintenant))
        .build();
  }

  /** Échéances non supprimées, dans l'ordre. */
  @Named("echeancesActives")
  default List<EcheanceResponse> echeancesActives(List<EcheanceCycle> echeances) {
    LocalDateTime maintenant = Fuseau.maintenant();
    return actives(echeances).stream().map(e -> toEcheance(e, maintenant)).toList();
  }

  @Named("nombreEcheances")
  default int nombreEcheances(List<EcheanceCycle> echeances) {
    return actives(echeances).size();
  }

  /** Première échéance ouverte en ce moment, ou null. */
  @Named("echeanceEnCours")
  default EcheanceResponse echeanceEnCours(List<EcheanceCycle> echeances) {
    LocalDateTime maintenant = Fuseau.maintenant();
    return actives(echeances).stream()
        .filter(e -> StatutEcheance.de(e, maintenant) == StatutEcheance.EN_COURS)
        .findFirst()
        .map(e -> toEcheance(e, maintenant))
        .orElse(null);
  }

  private static List<EcheanceCycle> actives(List<EcheanceCycle> echeances) {
    return echeances == null
        ? List.of()
        : echeances.stream()
            .filter(e -> e.getDeletedAt() == null)
            .sorted(Comparator.comparingInt(EcheanceCycle::getNumero))
            .toList();
  }

  /** Échéance (heure de Dakar) avec son décalage horaire. */
  default OffsetDateTime versOffset(LocalDateTime dateHeure) {
    return Fuseau.versOffset(dateHeure);
  }

  /** Cycle final d'une tontine événementielle, avec la distribution par membre. */
  default CycleResponse toResponse(CycleTontine cycle, List<MembreDistributionInfo> distribution) {
    return toResponse(cycle).toBuilder().distributionParMembre(distribution).build();
  }

  @Mapping(target = "membreId", source = "membre.id")
  @Mapping(target = "userId", source = "membre.user.id")
  @Mapping(target = "firstName", source = "membre.user.firstName")
  @Mapping(target = "lastName", source = "membre.user.lastName")
  @Mapping(target = "phone", source = "membre.user.phone")
  @Mapping(target = "ordreJackpot", source = "membre.ordreJackpot")
  @Mapping(target = "dateJackpot", source = "membre.dateJackpot")
  CycleResponse.GagnantInfo toGagnantInfo(CycleGagnant gagnant);

  /** Gagnants non supprimés. */
  @Named("gagnantsActifs")
  default List<CycleResponse.GagnantInfo> gagnantsActifs(List<CycleGagnant> gagnants) {
    return gagnants == null
        ? List.of()
        : gagnants.stream().filter(g -> g.getDeletedAt() == null).map(this::toGagnantInfo).toList();
  }

  // ─── Historique des bénéficiaires ────────────────────────────────────────

  @Mapping(target = "cycleId", source = "id")
  @Mapping(target = "gagnants", source = "gagnants", qualifiedByName = "gagnantsHistorique")
  @Mapping(target = "montantParGagnant", source = "gagnants", qualifiedByName = "montantParGagnant")
  BeneficiaireHistoriqueResponse toHistorique(CycleTontine cycle);

  @Mapping(target = "membreId", source = "membre.id")
  @Mapping(target = "userId", source = "membre.user.id")
  @Mapping(target = "firstName", source = "membre.user.firstName")
  @Mapping(target = "lastName", source = "membre.user.lastName")
  @Mapping(target = "phone", source = "membre.user.phone")
  @Mapping(target = "ordreJackpot", source = "membre.ordreJackpot")
  @Mapping(target = "dateJackpot", source = "membre.dateJackpot")
  BeneficiaireHistoriqueResponse.GagnantInfo toGagnantHistorique(CycleGagnant gagnant);

  @Named("gagnantsHistorique")
  default List<BeneficiaireHistoriqueResponse.GagnantInfo> gagnantsHistorique(
      List<CycleGagnant> gagnants) {
    return gagnants == null
        ? List.of()
        : gagnants.stream()
            .filter(g -> g.getDeletedAt() == null)
            .map(this::toGagnantHistorique)
            .toList();
  }

  /** Montant reçu par chaque gagnant (identique pour tous) – null si aucun gagnant. */
  @Named("montantParGagnant")
  default BigDecimal montantParGagnant(List<CycleGagnant> gagnants) {
    return gagnants == null
        ? null
        : gagnants.stream()
            .filter(g -> g.getDeletedAt() == null)
            .map(CycleGagnant::getMontantRecu)
            .findFirst()
            .orElse(null);
  }
}
