package com.africa.dinthialma_backend.notification.codeList;

/** Types de notifications in-app Dinthialma. */
public enum NotificationType {
  /** Un membre vient de soumettre une cotisation (destinataire : admin de la tontine). */
  PAIEMENT_RECU,

  /** Une cotisation a été validée par l'admin (destinataire : membre cotisant). */
  COTISATION_VALIDEE,

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

  /** Rappel quotidien scheduler – cotisation en attente ou période non cotisée. */
  RAPPEL_COTISATION
}
