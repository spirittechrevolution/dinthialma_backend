package com.africa.dinthialma_backend.tontine.repository;

import com.africa.dinthialma_backend.common.util.SearchUtils;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import com.africa.dinthialma_backend.tontine.codeList.TontineStatut;
import com.africa.dinthialma_backend.tontine.entity.Tontine;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Critères combinables pour les listes de tontines ({@code null} = filtre absent). */
public final class TontineSpecifications {

  private TontineSpecifications() {}

  public static Specification<Tontine> nonSupprimee() {
    return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
  }

  /** Tontines créées par l'utilisateur. */
  public static Specification<Tontine> creeePar(UUID userId) {
    return (root, query, cb) -> cb.equal(root.get("creePar").get("id"), userId);
  }

  /** Tontines où l'utilisateur est inscrit comme cotisant (membership non supprimé). */
  public static Specification<Tontine> cotiseePar(UUID userId) {
    return (root, query, cb) -> {
      Subquery<Integer> sub = query.subquery(Integer.class);
      Root<TontineMembre> m = sub.from(TontineMembre.class);
      sub.select(cb.literal(1))
          .where(
              cb.equal(m.get("tontine"), root),
              cb.equal(m.get("user").get("id"), userId),
              cb.isNull(m.get("deletedAt")));
      return cb.exists(sub);
    };
  }

  /** Tontines visibles : créées par l'utilisateur ou dont il est cotisant. */
  public static Specification<Tontine> visiblePar(UUID userId) {
    return creeePar(userId).or(cotiseePar(userId));
  }

  public static Specification<Tontine> statut(TontineStatut statut) {
    if (statut == null) return null;
    return (root, query, cb) -> cb.equal(root.get("statut"), statut);
  }

  /** Recherche insensible à la casse sur le nom de la tontine et le nom de l'événement. */
  public static Specification<Tontine> recherche(String q) {
    String motif = SearchUtils.motifLike(q);
    if (motif == null) return null;
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("nom")), motif, '\\'),
            cb.like(cb.lower(root.get("nomEvenement")), motif, '\\'));
  }
}
