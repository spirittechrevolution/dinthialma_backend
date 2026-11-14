package com.africa.dinthialma_backend.contribution.mapper;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.util.Fuseau;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.codeList.Ponctualite;
import com.africa.dinthialma_backend.contribution.dto.CotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.CycleRecapCotisationResponse;
import com.africa.dinthialma_backend.contribution.dto.MembreTotalCotisationResponse;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.calendrier.EcheancesTour;
import com.africa.dinthialma_backend.tontine.entity.EcheanceCycle;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversions des cotisations et des récapitulatifs par membre. */
@Mapper(imports = Ponctualite.class)
public interface CotisationMapper {

  @Mapping(target = "tontineId", source = "tontine.id")
  @Mapping(target = "tontineNom", source = "tontine.nom")
  @Mapping(target = "cycleId", source = "cycle.id")
  @Mapping(target = "numeroCycle", source = "cycle.numeroCycle")
  @Mapping(target = "echeanceId", source = "echeance.id")
  @Mapping(target = "numeroEcheance", source = "echeance.numero")
  @Mapping(target = "echeanceAt", source = "echeance.echeanceAt")
  @Mapping(target = "ponctualite", expression = "java(Ponctualite.de(cotisation))")
  CotisationResponse toResponse(Cotisation cotisation);

  /** Échéance (heure de Dakar) avec son décalage horaire. */
  default OffsetDateTime versOffset(LocalDateTime dateHeure) {
    return Fuseau.versOffset(dateHeure);
  }

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
  @Mapping(target = "echeanceId", ignore = true)
  @Mapping(target = "numeroEcheance", ignore = true)
  @Mapping(target = "echeanceAt", ignore = true)
  @Mapping(target = "nombreEcheances", ignore = true)
  @Mapping(target = "echeancesDues", ignore = true)
  @Mapping(target = "echeancesPayees", ignore = true)
  @Mapping(target = "echeances", ignore = true)
  CycleRecapCotisationResponse toRecap(TontineMembre membre, Cotisation cotisation);

  /**
   * Ligne du récapitulatif d'un tour à N échéances (B32) : champs de cotisation sur l'échéance de
   * {@code reference} (null : tour sans échéance enregistrée, cotisation la plus pertinente du
   * tour), puis le détail de chaque échéance.
   *
   * @param parEcheance cotisation retenue du membre par échéance
   * @param repliTour cotisation retenue sur le tour, utilisée seulement sans échéance de référence
   */
  default CycleRecapCotisationResponse toRecap(
      TontineMembre membre,
      List<EcheanceCycle> echeances,
      EcheanceCycle reference,
      Map<UUID, Cotisation> parEcheance,
      Cotisation repliTour,
      LocalDateTime maintenant) {
    Cotisation retenue = reference != null ? parEcheance.get(reference.getId()) : repliTour;
    List<CycleRecapCotisationResponse.EcheanceRecap> details =
        echeances.stream()
            .map(e -> toEcheanceRecap(e, parEcheance.get(e.getId()), maintenant))
            .toList();
    return toRecap(membre, retenue).toBuilder()
        .echeanceId(reference != null ? reference.getId() : null)
        .numeroEcheance(reference != null ? reference.getNumero() : null)
        .echeanceAt(reference != null ? versOffset(reference.getEcheanceAt()) : null)
        .nombreEcheances(echeances.size())
        .echeancesDues(
            (int)
                details.stream()
                    .filter(CycleRecapCotisationResponse.EcheanceRecap::isOuverte)
                    .count())
        .echeancesPayees(
            (int)
                details.stream()
                    .filter(d -> d.getStatutCotisation() == CotisationStatut.VALIDE)
                    .count())
        .echeances(details)
        .build();
  }

  /** Situation du membre sur une échéance ({@code cotisation} null : rien de déclaré). */
  default CycleRecapCotisationResponse.EcheanceRecap toEcheanceRecap(
      EcheanceCycle echeance, Cotisation cotisation, LocalDateTime maintenant) {
    return CycleRecapCotisationResponse.EcheanceRecap.builder()
        .echeanceId(echeance.getId())
        .numero(echeance.getNumero())
        .echeanceAt(versOffset(echeance.getEcheanceAt()))
        .ouverte(EcheancesTour.ouverte(echeance, maintenant))
        .cotisationId(cotisation != null ? cotisation.getId() : null)
        .statutCotisation(cotisation != null ? cotisation.getStatut() : null)
        .montant(cotisation != null ? cotisation.getMontant() : null)
        .ponctualite(Ponctualite.de(cotisation))
        .build();
  }

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
