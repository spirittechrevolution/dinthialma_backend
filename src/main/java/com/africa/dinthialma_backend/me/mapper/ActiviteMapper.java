package com.africa.dinthialma_backend.me.mapper;

import com.africa.dinthialma_backend.me.dto.ActiviteResponse;
import com.africa.dinthialma_backend.me.repository.ActiviteRepository.ActiviteRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Conversion d'une ligne brute du fil d'activité (projection native) en réponse. */
@Mapper
public interface ActiviteMapper {

  @Mapping(target = "date", source = "dateEvenement")
  @Mapping(target = "membreNom", expression = "java(nomComplet(row))")
  @Mapping(target = "libelle", expression = "java(libelle(row))")
  ActiviteResponse toResponse(ActiviteRow row);

  /** « Prénom Nom », ou null si aucun des deux n'est renseigné. */
  default String nomComplet(ActiviteRow row) {
    String nom =
        ((row.getMembrePrenom() != null ? row.getMembrePrenom() : "")
                + " "
                + (row.getMembreNom() != null ? row.getMembreNom() : ""))
            .trim();
    return nom.isEmpty() ? null : nom;
  }

  /** Libellé prêt à afficher : « Cotisation Famille · tour 5 » / « Jackpot Famille · tour 5 ». */
  default String libelle(ActiviteRow row) {
    String prefixe = "JACKPOT_RECU".equals(row.getType()) ? "Jackpot " : "Cotisation ";
    return prefixe + row.getTontineNom() + " · tour " + row.getNumeroCycle();
  }
}
