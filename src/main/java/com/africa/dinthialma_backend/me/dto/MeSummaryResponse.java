package com.africa.dinthialma_backend.me.dto;

import com.africa.dinthialma_backend.me.codeList.StatutCotisationDue;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Schema(
    description =
        "Résumé « Je cotise » de l'utilisateur connecté, toutes tontines confondues"
            + " (uniquement celles où il est inscrit comme cotisant).")
@Getter
@Builder
public class MeSummaryResponse {

  @Schema(description = "Total de mes cotisations VALIDÉES, toutes tontines, en FCFA")
  private BigDecimal totalCotiseValide;

  @Schema(description = "Total de mes cotisations EN_ATTENTE de validation, en FCFA")
  private BigDecimal totalEnAttente;

  @Schema(description = "Total des jackpots que j'ai reçus, toutes tontines, en FCFA")
  private BigDecimal totalJackpotsRecus;

  @Schema(description = "Nombre de cotisations du tour en cours restant A_PAYER", example = "2")
  private long nombreCotisationsDues;

  @Schema(
      description =
          "Somme des montants dus connus (tours en cours A_PAYER), en FCFA. Les tontines à"
              + " montant libre sans plancher n'y contribuent pas.",
      example = "10000")
  private BigDecimal montantDu;

  @Schema(description = "Détail par tontine où je cotise (non paginé : liste courte par nature)")
  private List<TontineCotisant> tontines;

  @Schema(description = "Ma situation dans une tontine où je cotise")
  @Getter
  @Builder
  public static class TontineCotisant {

    @Schema(description = "UUID de la tontine")
    private UUID tontineId;

    @Schema(description = "Nom de la tontine", example = "Famille")
    private String nom;

    @Schema(
        description = "Type de tontine",
        allowableValues = {"ROTATIVE", "EVENEMENTIELLE"})
    private TontineType tontineType;

    @Schema(
        description = "Statut de la tontine",
        allowableValues = {"BROUILLON", "ACTIVE", "SUSPENDUE", "TERMINEE"})
    private TontineStatut statut;

    @Schema(description = "Fréquence de cotisation", example = "MENSUEL")
    private String frequence;

    @Schema(description = "Nom de l'événement (EVENEMENTIELLE)", nullable = true)
    private String nomEvenement;

    @Schema(description = "Date d'échéance (EVENEMENTIELLE)", nullable = true)
    private LocalDate dateEcheance;

    @Schema(description = "UUID de mon membership (tontine_membres.id)")
    private UUID monMembreId;

    @Schema(
        description = "Statut de mon membership",
        allowableValues = {"ACTIF", "SUSPENDU", "SORTI"})
    private MembreStatut monStatutMembre;

    @Schema(description = "Ma position dans l'ordre des jackpots (ROTATIVE)", nullable = true)
    private Integer ordreJackpot;

    @Schema(description = "true si j'ai déjà reçu le jackpot dans cette tontine")
    @JsonProperty("aRecuJackpot")
    private boolean aRecuJackpot;

    /** Getter explicite : cf. {@code MembreResponse#isARecuJackpot()}. */
    @JsonProperty("aRecuJackpot")
    public boolean isARecuJackpot() {
      return aRecuJackpot;
    }

    @Schema(description = "Mes cotisations VALIDÉES dans cette tontine, en FCFA")
    private BigDecimal totalCotiseValide;

    @Schema(description = "Mes cotisations EN_ATTENTE dans cette tontine, en FCFA")
    private BigDecimal totalEnAttente;

    @Schema(
        description =
            "Total des jackpots reçus dans cette tontine (FCFA). 0 si aucun ; toujours 0 pour une"
                + " tontine EVENEMENTIELLE (pas de jackpot : chaque membre récupère sa mise).",
        example = "50000")
    private BigDecimal totalJackpotsRecus;

    @Schema(description = "Nombre de jackpots reçus dans cette tontine. 0 si aucun.", example = "1")
    private long nombreJackpotsRecus;

    @Schema(description = "Nombre de tours où ma cotisation est VALIDÉE", example = "4")
    private long toursPayes;

    @Schema(
        description = "Nombre de tours générés à ce jour (croît en mode MANUEL)",
        example = "12")
    private long toursTotal;

    @Schema(description = "Tour EN_COURS – null si aucun", nullable = true)
    private CycleEnCours cycleEnCours;

    @Schema(
        description =
            "Cotisation attendue sur le tour en cours – null si aucun tour en cours, tontine non"
                + " ACTIVE ou membership non ACTIF",
        nullable = true)
    private CotisationDue cotisationDue;

    @Schema(description = "Gestionnaire (créateur) de la tontine")
    private Gestionnaire gestionnaire;
  }

  @Schema(description = "Tour EN_COURS d'une tontine")
  @Getter
  @Builder
  public static class CycleEnCours {

    @Schema(description = "UUID du cycle")
    private UUID cycleId;

    @Schema(description = "Numéro du tour", example = "5")
    private int numeroCycle;

    @Schema(description = "Début du tour")
    private LocalDate dateDebut;

    @Schema(description = "Fin du tour")
    private LocalDate dateFin;
  }

  @Schema(description = "Ma cotisation attendue sur le tour en cours")
  @Getter
  @Builder
  public static class CotisationDue {

    @Schema(
        description =
            "Montant attendu en FCFA : montant fixe, ou plancher en mode libre – null si libre"
                + " sans plancher",
        example = "5000",
        nullable = true)
    private BigDecimal montant;

    @Schema(description = "true = je choisis le montant (EVENEMENTIELLE)")
    private boolean montantLibre;

    @Schema(description = "Plancher en mode libre", nullable = true)
    private BigDecimal montantMinimum;

    @Schema(description = "Date limite (= fin du tour en cours)")
    private LocalDate dateLimite;

    @Schema(
        description = "Situation de ma cotisation sur ce tour",
        allowableValues = {"A_PAYER", "EN_ATTENTE", "VALIDE"})
    private StatutCotisationDue statut;

    @Schema(description = "UUID de ma cotisation sur ce tour – null si A_PAYER", nullable = true)
    private UUID cotisationId;

    @Schema(
        description = "A_PAYER après un refus : motif du dernier refus sur ce tour (null sinon)",
        example = "Transfert non reçu",
        nullable = true)
    private String motifDernierRefus;
  }

  @Schema(description = "Gestionnaire d'une tontine")
  @Getter
  @Builder
  public static class Gestionnaire {

    @Schema(description = "UUID de l'utilisateur")
    private UUID id;

    @Schema(description = "Prénom", example = "Moussa")
    private String firstName;

    @Schema(description = "Nom de famille", example = "Diop")
    private String lastName;

    @Schema(description = "Téléphone normalisé (sans +)", example = "221770000001")
    private String phone;
  }
}
