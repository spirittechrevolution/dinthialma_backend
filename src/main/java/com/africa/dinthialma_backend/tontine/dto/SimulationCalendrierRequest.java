package com.africa.dinthialma_backend.tontine.dto;

import com.africa.dinthialma_backend.tontine.codeList.TontineType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Paramètres (query string) de la simulation du calendrier (B26) : mêmes champs que la création
 * d'une tontine, pour que le front affiche les échéances calculées par le backend.
 */
@Schema(description = "Simulation du calendrier des tours (B26), sans effet en base")
@Getter
@Setter
public class SimulationCalendrierRequest {

  @Schema(description = "« Tous les N … »", example = "3")
  private Integer rythmeIntervalle;

  @Schema(description = "JOUR | SEMAINE | MOIS", example = "JOUR")
  private String rythmeUnite;

  @Schema(description = "SEMAINE : LUNDI…DIMANCHE ; MOIS : 1…31 ou DERNIER", example = "DIMANCHE")
  private String jourEcheance;

  @Schema(description = "HH:mm, défaut 23:59", example = "23:59")
  private String heureEcheance;

  @Schema(description = "Fréquence historique, si aucun rythme n'est fourni", example = "MENSUEL")
  private String frequence;

  @Schema(description = "Date de démarrage", example = "2026-11-12")
  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate dateDebut;

  @Schema(description = "Défaut ROTATIVE", example = "ROTATIVE")
  private TontineType tontineType;

  @Schema(description = "ROTATIVE : nombre de membres (obligatoire)", example = "10")
  @Min(1)
  private Integer nombreMembres;

  @Schema(description = "ROTATIVE : gagnants par tour (défaut 1)", example = "1")
  @Min(1)
  private Integer nombreGagnants;

  @Schema(
      description = "EVENEMENTIELLE : date de l'événement (obligatoire)",
      example = "2027-06-16")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate dateEcheance;
}
