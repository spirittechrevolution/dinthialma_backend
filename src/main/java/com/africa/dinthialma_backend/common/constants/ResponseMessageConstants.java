package com.africa.dinthialma_backend.common.constants;

/** Constantes des messages de réponse – convention : DOMAINE_OPERATION_SUCCES|FAILURE */
public final class ResponseMessageConstants {

  private ResponseMessageConstants() {}

  // Auth
  public static final String AUTH_LOGIN_SUCCESS = "Connexion réussie";
  public static final String AUTH_LOGIN_FAILURE = "Identifiants incorrects";
  public static final String AUTH_LOGOUT_SUCCESS = "Déconnexion réussie";
  public static final String AUTH_REGISTER_SUCCESS = "Inscription réussie";
  public static final String AUTH_OTP_SENT = "OTP envoyé avec succès";
  public static final String AUTH_OTP_VERIFIED = "OTP vérifié avec succès";
  public static final String AUTH_OTP_INVALID = "OTP invalide ou expiré";
  public static final String AUTH_PASSWORD_RESET_SUCCESS = "Mot de passe réinitialisé avec succès";
  public static final String AUTH_PASSWORD_RESET_EMAIL_SENT = "Email de réinitialisation envoyé";
  public static final String AUTH_PROFILE_UPDATED = "Profil mis à jour avec succès";
  public static final String AUTH_USER_NOT_FOUND = "Utilisateur introuvable";
  public static final String AUTH_PHONE_ALREADY_USED = "Ce numéro de téléphone est déjà utilisé";
  public static final String AUTH_EMAIL_ALREADY_USED = "Cet email est déjà utilisé";
  public static final String AUTH_OTP_NOT_VERIFIED = "Veuillez d'abord vérifier votre OTP";
  public static final String AUTH_ACCOUNT_DISABLED = "Ce compte est désactivé";
  public static final String AUTH_ACCOUNT_NOT_ACTIVATED =
      "Votre compte a été créé par votre gestionnaire de tontine."
          + " Inscrivez-vous sur Dinthialma pour vous connecter et accéder à vos tontines.";

  // PIN
  public static final String AUTH_PIN_SETUP_SUCCESS = "Code PIN configuré avec succès";
  public static final String AUTH_PIN_ALREADY_SET =
      "Un code PIN est déjà configuré. Utilisez la réinitialisation pour le changer";
  public static final String AUTH_PIN_LOGIN_SUCCESS = "Connexion PIN réussie";
  public static final String AUTH_PIN_INVALID = "Code PIN incorrect";
  public static final String AUTH_PIN_LOCKED =
      "Code PIN verrouillé suite à trop de tentatives. Réessayez dans %d minutes";
  public static final String AUTH_PIN_NOT_CONFIGURED =
      "Aucun code PIN n'est configuré. Veuillez d'abord vous connecter avec votre mot de passe";
  public static final String AUTH_PIN_RESET_SUCCESS = "Code PIN réinitialisé avec succès";
  public static final String AUTH_PIN_MISMATCH = "Les deux codes PIN ne correspondent pas";
  public static final String AUTH_PIN_TOO_SIMPLE =
      "Ce code PIN est trop simple. Choisissez une combinaison plus unique";
  public static final String AUTH_PIN_INVALID_FORMAT =
      "Le code PIN doit contenir exactement 6 chiffres";

  // Session
  public static final String AUTH_SESSION_EXPIRED =
      "Session expirée. Veuillez vous reconnecter avec votre mot de passe";
  public static final String AUTH_SESSION_NOT_FOUND =
      "Aucune session active. Veuillez d'abord vous connecter avec votre mot de passe";
  public static final String AUTH_SESSION_REVOKED = "Session révoquée avec succès";
  public static final String AUTH_SESSION_ALL_REVOKED = "Toutes les sessions ont été révoquées";

  // Tontine
  public static final String TONTINE_CREATE_SUCCESS = "Tontine créée avec succès";
  public static final String TONTINE_UPDATE_SUCCESS = "Tontine mise à jour avec succès";
  public static final String TONTINE_DELETE_SUCCESS = "Tontine supprimée avec succès";
  public static final String TONTINE_GET_SUCCESS = "Tontine récupérée avec succès";
  public static final String TONTINE_LIST_SUCCESS = "Liste des tontines récupérée avec succès";
  public static final String TONTINE_NOT_FOUND = "Tontine introuvable";
  public static final String TONTINE_ACCESS_DENIED = "Accès non autorisé à cette tontine";
  public static final String TONTINE_ACTIVATED = "Tontine activée avec succès";
  public static final String TONTINE_SUSPENDED = "Tontine suspendue avec succès";

  // Cycle
  public static final String CYCLE_LIST_SUCCESS = "Liste des cycles récupérée avec succès";
  public static final String CYCLE_GET_SUCCESS = "Cycle récupéré avec succès";
  public static final String CYCLE_NOT_FOUND = "Cycle introuvable";
  public static final String CYCLE_OPENED = "Cycle ouvert avec succès";
  public static final String CYCLE_CLOSED = "Cycle clôturé avec succès";
  public static final String BENEFICIAIRE_LIST_SUCCESS =
      "Historique des bénéficiaires récupéré avec succès";

  // User search
  public static final String USER_SEARCH_FOUND = "Utilisateur trouvé";
  public static final String USER_SEARCH_NOT_FOUND = "Aucun compte avec ce numéro de téléphone";
  public static final String MEMBER_NAME_REQUIRED =
      "Le prénom et le nom sont obligatoires pour ajouter un membre sans compte Dinthialma";

  // Membre
  public static final String MEMBER_ADD_SUCCESS = "Membre ajouté avec succès";
  public static final String MEMBER_REMOVE_SUCCESS = "Membre retiré avec succès";
  public static final String MEMBER_LIST_SUCCESS = "Liste des membres récupérée avec succès";
  public static final String MEMBER_NOT_FOUND = "Membre introuvable";
  public static final String MEMBER_ALREADY_EXISTS = "Ce membre est déjà dans la tontine";
  public static final String MEMBER_STATUT_UPDATED = "Statut du membre mis à jour avec succès";

  // Cotisation
  public static final String CONTRIBUTION_RECORD_SUCCESS = "Cotisation enregistrée avec succès";
  public static final String CONTRIBUTION_LIST_SUCCESS =
      "Historique des cotisations récupéré avec succès";
  public static final String CONTRIBUTION_GET_SUCCESS = "Cotisation récupérée avec succès";
  public static final String CONTRIBUTION_NOT_FOUND = "Cotisation introuvable";
  public static final String CONTRIBUTION_VALIDATED = "Cotisation validée avec succès";
  public static final String CONTRIBUTION_ADMIN_RECORD_SUCCESS =
      "Cotisation enregistrée et validée avec succès";
  public static final String CONTRIBUTION_UPDATE_SUCCESS = "Cotisation mise à jour avec succès";
  public static final String CONTRIBUTION_RECAP_SUCCESS =
      "Récapitulatif des cotisations récupéré avec succès";
  public static final String CONTRIBUTION_CANNOT_EDIT =
      "Cette cotisation ne peut pas être modifiée" + " (cycle clôturé ou cotisation en retard)";

  // Commission
  public static final String COMMISSION_CREATE_SUCCESS = "Commission créée avec succès";
  public static final String COMMISSION_UPDATE_SUCCESS = "Commission mise à jour avec succès";
  public static final String COMMISSION_DELETE_SUCCESS = "Commission supprimée avec succès";
  public static final String COMMISSION_LIST_SUCCESS =
      "Liste des commissions récupérée avec succès";
  public static final String COMMISSION_NOT_FOUND = "Commission introuvable";
  public static final String COMMISSION_ALREADY_EXISTS =
      "Une commission de ce type existe déjà sur cette tontine";

  // Admin Dashboard
  public static final String ADMIN_DASHBOARD_SUCCESS = "Dashboard récupéré avec succès";
  public static final String ADMIN_MY_DASHBOARD_SUCCESS =
      "Mon tableau de bord récupéré avec succès";
  public static final String ADMIN_USERS_LIST_SUCCESS =
      "Liste des utilisateurs récupérée avec succès";
  public static final String ADMIN_USER_GET_SUCCESS = "Utilisateur récupéré avec succès";
  public static final String ADMIN_USER_DISABLED = "Compte utilisateur désactivé avec succès";
  public static final String ADMIN_USER_ENABLED = "Compte utilisateur réactivé avec succès";
  public static final String ADMIN_USER_ROLES_UPDATED = "Rôles mis à jour avec succès";
  public static final String ADMIN_ACCESS_DENIED = "Accès refusé – rôle SUPER_ADMIN requis";

  // Notification
  public static final String NOTIFICATION_SENT = "Notification envoyée avec succès";
  public static final String NOTIFICATION_SEND_FAILURE = "Échec de l'envoi de la notification";

  // CodeList
  public static final String CODELIST_GET_SUCCESS = "Référentiel récupéré avec succès";
  public static final String CODELIST_GET_FAILURE_NOT_FOUND = "Code list introuvable";
  public static final String CODELIST_GET_FAILURE_BAD_REQUEST = "Identifiant de code list invalide";
  public static final String CODELIST_POST_SUCCESS = "Code list créé avec succès";
  public static final String CODELIST_POST_FAILURE = "Échec de la création du code list";
  public static final String CODELIST_POST_DUPLICATE = "Ce couple type/valeur existe déjà";
  public static final String CODELIST_PUT_SUCCESS = "Code list mis à jour avec succès";
  public static final String CODELIST_PUT_FAILURE = "Échec de la mise à jour du code list";
  public static final String CODELIST_PUT_FAILURE_BAD_REQUEST =
      "L'id du body ne correspond pas à l'id du path";
}
