package com.africa.dinthialma_backend.common.util;

import com.africa.dinthialma_backend.common.exception.BadRequestException;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Réponses HTTP des exports CSV (B24). */
public final class CsvExport {

  private CsvExport() {}

  /** Seul le format CSV est pris en charge. */
  public static void verifierFormat(String format) throws CustomException {
    if (format == null || !"csv".equalsIgnoreCase(format.trim())) {
      throw new BadRequestException(
          "Format d'export non pris en charge (seul csv est disponible)",
          ErrorCode.INVALID_PARAMETER);
    }
  }

  public static ResponseEntity<byte[]> reponse(byte[] contenu, String nomFichier) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(nomFichier).build().toString())
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(contenu);
  }
}
