package com.africa.dinthialma_backend.common.util;

import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Exports CSV (B24) : validation du format (côté service) et exposition HTTP (côté contrôleur). */
public final class CsvExport {

  private CsvExport() {}

  /** Seul le format CSV est pris en charge. Appelé par les services d'export. */
  public static void verifierFormat(String format) throws CustomException {
    if (format == null || !"csv".equalsIgnoreCase(format.trim())) {
      throw new BadRequestException(
          "Format d'export non pris en charge (seul csv est disponible)",
          ErrorCode.INVALID_PARAMETER);
    }
  }

  /** Expose un fichier produit par un service, en pièce jointe. */
  public static ResponseEntity<byte[]> reponse(FichierExport fichier) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fichier.nomFichier()).build().toString())
        .contentType(MediaType.parseMediaType(fichier.typeMime()))
        .body(fichier.contenu());
  }
}
