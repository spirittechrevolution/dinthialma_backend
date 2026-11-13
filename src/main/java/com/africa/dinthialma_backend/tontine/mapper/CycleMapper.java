package com.africa.dinthialma_backend.tontine.mapper;

import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.tontine.dto.BeneficiaireHistoriqueResponse;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse;
import com.africa.dinthialma_backend.tontine.dto.CycleResponse.MembreDistributionInfo;
import com.africa.dinthialma_backend.tontine.entity.CycleGagnant;
import com.africa.dinthialma_backend.tontine.entity.CycleTontine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
  CycleResponse toResponse(CycleTontine cycle);

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
