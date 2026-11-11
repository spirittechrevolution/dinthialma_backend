package com.africa.dinthialma_backend.contribution.repository;

import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.contribution.codeList.CotisationStatut;
import com.africa.dinthialma_backend.contribution.entity.Cotisation;
import com.africa.dinthialma_backend.member.entity.TontineMembre;
import jakarta.persistence.criteria.Join;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Critères combinables pour les listes de cotisations filtrées.
 *
 * <p>Chaque méthode renvoie {@code null} quand le filtre est absent : Spring Data ignore alors le
 * critère dans {@link Specification#where(Specification)} / {@code and(...)}.
 */
public final class CotisationSpecifications {

  private CotisationSpecifications() {}

  /** Cotisations non supprimées. */
  public static Specification<Cotisation> nonSupprimee() {
    return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
  }

  /** Cotisations d'une tontine. */
  public static Specification<Cotisation> deTontine(UUID tontineId) {
    if (tontineId == null) return null;
    return (root, query, cb) -> cb.equal(root.get("tontine").get("id"), tontineId);
  }

  /** Cotisations des tontines (non supprimées) créées par un utilisateur. */
  public static Specification<Cotisation> deTontinesGereesPar(UUID userId) {
    return (root, query, cb) ->
        cb.and(
            cb.equal(root.get("tontine").get("creePar").get("id"), userId),
            cb.isNull(root.get("tontine").get("deletedAt")));
  }

  /** Cotisations d'un cycle. */
  public static Specification<Cotisation> deCycle(UUID cycleId) {
    if (cycleId == null) return null;
    return (root, query, cb) -> cb.equal(root.get("cycle").get("id"), cycleId);
  }

  /** Cotisations d'un membre (tontine_membres.id). */
  public static Specification<Cotisation> deMembre(UUID membreId) {
    if (membreId == null) return null;
    return (root, query, cb) -> cb.equal(root.get("membre").get("id"), membreId);
  }

  /** Cotisations dont le statut fait partie de la liste (liste vide ou null = pas de filtre). */
  public static Specification<Cotisation> statutParmi(Collection<CotisationStatut> statuts) {
    if (statuts == null || statuts.isEmpty()) return null;
    return (root, query, cb) -> root.get("statut").in(statuts);
  }

  /**
   * Recherche libre (insensible à la casse) sur le prénom, le nom, le téléphone du cotisant et la
   * référence de transaction. Le « + » initial d'un numéro est ignoré (téléphones stockés sans).
   */
  public static Specification<Cotisation> recherche(String q) {
    if (q == null || q.isBlank()) return null;
    String terme = q.trim();
    if (terme.startsWith("+")) terme = terme.substring(1);
    String motif = "%" + terme.toLowerCase(Locale.ROOT) + "%";
    return (root, query, cb) -> {
      Join<Cotisation, TontineMembre> membre = root.join("membre");
      Join<TontineMembre, User> user = membre.join("user");
      return cb.or(
          cb.like(cb.lower(user.get("firstName")), motif),
          cb.like(cb.lower(user.get("lastName")), motif),
          cb.like(user.get("phone"), motif),
          cb.like(cb.lower(root.get("referenceTransaction")), motif));
    };
  }
}
