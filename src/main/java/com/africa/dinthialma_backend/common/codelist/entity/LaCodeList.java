package com.africa.dinthialma_backend.common.codelist.entity;

import com.africa.dinthialma_backend.common.base.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Référentiel de valeurs persistées – table {@code la_code_list}.
 *
 * <p>Permet de stocker des listes de valeurs configurables sans recompiler l'application (ex :
 * FREQUENCE_TONTINE, METHODE_PAIEMENT, STATUT_COTISATION, etc.).
 *
 * <p>Usage frontend : {@code GET /v1/code-list/type/{type}} peuple les selects/dropdowns.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "la_code_list", schema = "dinthialma")
public class LaCodeList extends BaseEntity {

  /** Type du référentiel (ex : {@code FREQUENCE_TONTINE}, {@code METHODE_PAIEMENT}). */
  @Basic(optional = false)
  @Column(name = "type", length = 100, nullable = false)
  private String type;

  /** Valeur technique utilisée dans le code (ex : {@code HEBDOMADAIRE}, {@code WAVE}). */
  @Basic(optional = false)
  @Column(name = "value", length = 100, nullable = false)
  private String value;

  /** Libellé lisible par l'utilisateur (ex : {@code "Chaque semaine"}, {@code "Wave"}). */
  @Basic(optional = false)
  @Column(name = "description", length = 500, nullable = false)
  private String description;

  /**
   * {@code true} = valeur initialisée par le système (non supprimable via l'API). {@code false} =
   * valeur créée manuellement par un admin (supprimable).
   */
  @Basic(optional = false)
  @Column(name = "is_system_assign", nullable = false)
  private boolean isSystemAssign;

  @Override
  public String toString() {
    return "LaCodeList{"
        + "id="
        + getId()
        + ", type='"
        + type
        + '\''
        + ", value='"
        + value
        + '\''
        + ", description='"
        + description
        + '\''
        + ", isSystemAssign="
        + isSystemAssign
        + '}';
  }
}
