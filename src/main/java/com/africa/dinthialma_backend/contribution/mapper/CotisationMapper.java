package com.africa.dinthialma_backend.contribution.mapper;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.contribution.codeList.Ponctualite;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.CycleRecapCotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.MembreTotalCotisationResponse;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversions des cotisations et des récapitulatifs par membre. */
@Mapper(imports = Ponctualite.class)
public interface CotisationMapper {

  @Mapping(target = "tontineId", source = "tontine.id")
  @Mapping(target = "tontineNom", source = "tontine.nom")
  @Mapping(target = "cycleId", source = "cycle.id")
  @Mapping(target = "numeroCycle", source = "cycle.numeroCycle")
  @Mapping(target = "ponctualite", expression = "java(Ponctualite.de(cotisation))")
  CotisationResponse toResponse(Cotisation cotisation);

  /** Cotisant d'une cotisation (null si le membre ou son compte est absent). */
  default CotisationResponse.MembreInfo toMembreInfo(TontineMembre membre) {
    if (membre == null || membre.getUser() == null) return null;
    User u = membre.getUser();
    return CotisationResponse.MembreInfo.builder()
        .membreId(membre.getId())
        .userId(u.getId())
        .firstName(u.getFirstName())
        .lastName(u.getLastName())
        .phone(u.getPhone())
        .build();
  }

  CotisationResponse.SaisiePar toSaisiePar(User user);

  CotisationResponse.ValideurInfo toValideurInfo(User user);

  /**
   * Ligne du récapitulatif d'un tour : le membre et, s'il y en a une, sa cotisation retenue pour ce
   * tour ({@code cotisation} peut être null : champs de cotisation vides).
   */
  @Mapping(target = "membreId", source = "membre.id")
  @Mapping(target = "userId", source = "membre.user.id")
  @Mapping(target = "firstName", source = "membre.user.firstName")
  @Mapping(target = "lastName", source = "membre.user.lastName")
  @Mapping(target = "phone", source = "membre.user.phone")
  @Mapping(target = "accountStatus", source = "membre.user.accountStatus")
  @Mapping(target = "statutMembre", source = "membre.statut")
  @Mapping(target = "ordreJackpot", source = "membre.ordreJackpot")
  @Mapping(target = "cotisationId", source = "cotisation.id")
  @Mapping(target = "montant", source = "cotisation.montant")
  @Mapping(target = "methodePaiement", source = "cotisation.methodePaiement")
  @Mapping(target = "referenceTransaction", source = "cotisation.referenceTransaction")
  @Mapping(target = "note", source = "cotisation.note")
  @Mapping(target = "statutCotisation", source = "cotisation.statut")
  @Mapping(target = "dateValidation", source = "cotisation.dateValidation")
  @Mapping(target = "motifRefus", source = "cotisation.motifRefus")
  @Mapping(target = "cotisationCreatedAt", source = "cotisation.createdAt")
  @Mapping(target = "enregistrePar", source = "cotisation.enregistrePar")
  @Mapping(target = "ponctualite", expression = "java(Ponctualite.de(cotisation))")
  CycleRecapCotisationResponse toRecap(TontineMembre membre, Cotisation cotisation);

  /** Total VALIDÉ d'un membre, tous tours confondus (0 si aucun). */
  @Mapping(target = "membreId", source = "membre.id")
  @Mapping(target = "userId", source = "membre.user.id")
  @Mapping(target = "firstName", source = "membre.user.firstName")
  @Mapping(target = "lastName", source = "membre.user.lastName")
  @Mapping(target = "phone", source = "membre.user.phone")
  @Mapping(target = "accountStatus", source = "membre.user.accountStatus")
  @Mapping(target = "statutMembre", source = "membre.statut")
  @Mapping(target = "ordreJackpot", source = "membre.ordreJackpot")
  @Mapping(
      target = "totalCotise",
      source = "totalCotise",
      defaultExpression = "java(BigDecimal.ZERO)")
  @Mapping(target = "nombreCotisationsValidees", source = "nombreCotisationsValidees")
  MembreTotalCotisationResponse toTotal(
      TontineMembre membre, BigDecimal totalCotise, long nombreCotisationsValidees);
}
