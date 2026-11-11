package com.africa.dinthialma_backend.member.repository;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.common.util.SearchUtils;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.member.codeList.MembreStatut;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Critères combinables pour la liste des membres d'une tontine ({@code null} = filtre absent). */
public final class TontineMembreSpecifications {

  private TontineMembreSpecifications() {}

  public static Specification<TontineMembre> deTontine(UUID tontineId) {
    return (root, query, cb) ->
        cb.and(
            cb.equal(root.get("tontine").get("id"), tontineId), cb.isNull(root.get("deletedAt")));
  }

  public static Specification<TontineMembre> statut(MembreStatut statut) {
    if (statut == null) return null;
    return (root, query, cb) -> cb.equal(root.get("statut"), statut);
  }

  /** Recherche insensible à la casse sur le prénom, le nom et le téléphone du membre. */
  public static Specification<TontineMembre> recherche(String q) {
    String motif = SearchUtils.motifLike(q);
    if (motif == null) return null;
    return (root, query, cb) -> {
      Join<TontineMembre, User> u = root.join("user");
      return cb.or(
          cb.like(cb.lower(u.get("firstName")), motif, '\\'),
          cb.like(cb.lower(u.get("lastName")), motif, '\\'),
          cb.like(u.get("phone"), motif, '\\'));
    };
  }

  /**
   * Membres ayant ({@code aPaye=true}) ou n'ayant pas ({@code false}) une cotisation <b>active</b>
   * (EN_ATTENTE ou VALIDE, hors REFUSE) sur le cycle donné. Ignoré si l'un des deux est null.
   */
  public static Specification<TontineMembre> paiementSurCycle(UUID cycleId, Boolean aPaye) {
    if (cycleId == null || aPaye == null) return null;
    return (root, query, cb) -> {
      Subquery<Integer> sub = query.subquery(Integer.class);
      Root<Cotisation> c = sub.from(Cotisation.class);
      sub.select(cb.literal(1))
          .where(
              cb.equal(c.get("membre"), root),
              cb.equal(c.get("cycle").get("id"), cycleId),
              cb.isNull(c.get("deletedAt")),
              c.get("statut").in(CotisationStatut.EN_ATTENTE, CotisationStatut.VALIDE));
      return aPaye ? cb.exists(sub) : cb.not(cb.exists(sub));
    };
  }
}
