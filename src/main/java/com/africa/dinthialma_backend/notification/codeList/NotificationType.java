package com.africa.dinthialma_backend.notification.codeList;

/** Types de notifications in-app Dinthialma. */
public enum NotificationType {
  /** Un membre vient de soumettre une cotisation (destinataire : admin de la tontine). */
  PAIEMENT_RECU,

  /** Une cotisation a été validée par l'admin (destinataire : membre cotisant). */
  COTISATION_VALIDEE,

  /** Une cotisation déclarée a été refusée par l'admin, avec motif (destinataire : membre). */
  COTISATION_REFUSEE,

  /** Le jackpot d'un cycle ROTATIVE a été attribué (destinataire : gagnant + admin). */
  JACKPOT_DISTRIBUE,

  /** Un cycle s'est clôturé avec des cotisations non payées (destinataire : admin). */
  PAIEMENT_EN_RETARD,

  /** Le tour du jackpot de ce membre approche dans N jours (destinataire : futur bénéficiaire). */
  TOUR_PROCHE,

  /** Un cycle arrive à échéance dans N jours (destinataire : admin). */
  CYCLE_BIENTOT_CLOTURE,

  /** Distribution finale d'une tontine EVENEMENTIELLE (destinataire : chaque membre). */
  DISTRIBUTION_FINALE,

  /**
   * Un admin a ajouté ce numéro à une tontine avant l'inscription (destinataire : futur membre).
   */
  INVITATION_TONTINE,

  /** Un cycle ROTATIVE MANUEL vient d'être ouvert (destinataire : membres actifs). */
  CYCLE_OUVERT,

  /** Le statut du membre dans une tontine a changé (suspendu, retiré, réactivé). */
  STATUT_MEMBRE,

  /** Confirmation de soumission d'une cotisation (destinataire : membre ayant soumis). */
  COTISATION_SOUMISE,

  /** Nouvelle demande d'adhésion via code d'invitation (destinataire : gestionnaire). */
  DEMANDE_ADHESION,

  /** Demande d'adhésion refusée (destinataire : demandeur). */
  ADHESION_REFUSEE,

  /** Rappel quotidien scheduler – cotisation en attente ou période non cotisée. */
  RAPPEL_COTISATION,

  /** Un membre demande à recevoir le prochain jackpot en priorité (destinataire : gestionnaire). */
  PRIORITE_JACKPOT_DEMANDEE,

  /**
   * Priorité de jackpot accordée (destinataires : le membre concerné et, par transparence, les
   * autres membres actifs).
   */
  PRIORITE_JACKPOT_ACCORDEE,

  /** Priorité de jackpot refusée ou annulée par le gestionnaire (destinataire : le membre). */
  PRIORITE_JACKPOT_REFUSEE,

  /** L'ordre des bénéficiaires a changé en cours de tontine (destinataires : membres actifs). */
  ORDRE_BENEFICIAIRE_MODIFIE,

  /**
   * Tour clôturé en ordre MANUEL ou HYBRIDE : les gagnants sont à désigner (destinataire :
   * gestionnaire).
   */
  GAGNANTS_A_DESIGNER,

  /**
   * Règles de la tontine modifiées en cours (B26 : jour ou heure d'échéance) ; s'appliquent aux
   * tours pas encore commencés (destinataires : membres actifs).
   */
  REGLES_MODIFIEES,

  /**
   * Échéance passée avec des membres actifs qui n'ont pas déclaré leur cotisation (B26) ; le statut
   * des cotisations ne change pas (destinataire : gestionnaire).
   */
  COTISATIONS_MANQUANTES
}
