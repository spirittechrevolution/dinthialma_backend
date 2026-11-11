package com.africa.dinthialma_backend.common.util;

/**
 * Fichier produit par un service d'export, prêt à être exposé tel quel par un contrôleur.
 *
 * @param nomFichier nom proposé au téléchargement
 * @param contenu octets du fichier
 * @param typeMime type de contenu (ex. {@code text/csv;charset=UTF-8})
 */
public record FichierExport(String nomFichier, byte[] contenu, String typeMime) {

  public static FichierExport csv(String nomFichier, byte[] contenu) {
    return new FichierExport(nomFichier, contenu, "text/csv;charset=UTF-8");
  }
}
