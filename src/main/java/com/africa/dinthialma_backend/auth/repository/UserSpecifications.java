package com.africa.dinthialma_backend.auth.repository;

import com.africa.dinthialma_backend.auth.codeList.AccountStatus;
import com.africa.dinthialma_backend.auth.codeList.UserRole;
import com.africa.dinthialma_backend.auth.entity.User;
import com.africa.dinthialma_backend.auth.entity.UserRoleAssignment;
import com.africa.dinthialma_backend.common.util.SearchUtils;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/** Critères combinables pour la liste des utilisateurs (console SUPER_ADMIN). */
public final class UserSpecifications {

  private UserSpecifications() {}

  public static Specification<User> nonSupprime() {
    return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
  }

  /** Statut de compte ; ACTIVE inclut les comptes historiques au statut NULL. */
  public static Specification<User> accountStatus(AccountStatus status) {
    if (status == null) return null;
    if (status == AccountStatus.ACTIVE) {
      return (root, query, cb) ->
          cb.or(
              cb.isNull(root.get("accountStatus")),
              cb.equal(root.get("accountStatus"), AccountStatus.ACTIVE));
    }
    return (root, query, cb) -> cb.equal(root.get("accountStatus"), status);
  }

  /** Utilisateurs possédant le rôle applicatif donné. */
  public static Specification<User> role(UserRole role) {
    if (role == null) return null;
    return (root, query, cb) -> {
      Subquery<Integer> sub = query.subquery(Integer.class);
      Root<UserRoleAssignment> a = sub.from(UserRoleAssignment.class);
      sub.select(cb.literal(1)).where(cb.equal(a.get("user"), root), cb.equal(a.get("role"), role));
      return cb.exists(sub);
    };
  }

  /** Recherche insensible à la casse sur le prénom, le nom, le téléphone et l'email. */
  public static Specification<User> recherche(String q) {
    String motif = SearchUtils.motifLike(q);
    if (motif == null) return null;
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("firstName")), motif, '\\'),
            cb.like(cb.lower(root.get("lastName")), motif, '\\'),
            cb.like(root.get("phone"), motif, '\\'),
            cb.like(cb.lower(root.get("email")), motif, '\\'));
  }
}
