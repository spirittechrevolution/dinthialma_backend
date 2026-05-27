package com.africa.dinthialma_backend.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * Utilitaire de chiffrement AES-256-GCM pour le stockage sécurisé des refresh tokens Keycloak.
 *
 * <p>Format stocké : {@code Base64(IV[12 octets] || ciphertext || GCM-tag[16 octets])}
 *
 * <p>La clé est dérivée par SHA-256 depuis une clé secrète passée en paramètre. La clé doit être
 * configurée via {@code app.security.token-encryption-key} en production.
 */
@Slf4j
public final class TokenEncryptionUtil {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private TokenEncryptionUtil() {}

  /**
   * Chiffre un texte clair avec AES-256-GCM.
   *
   * @param plainText texte à chiffrer (refresh token Keycloak)
   * @param secretKey clé secrète brute (sera dérivée par SHA-256)
   * @return texte chiffré encodé en Base64
   */
  public static String encrypt(String plainText, String secretKey) {
    try {
      SecretKeySpec keySpec = deriveKey(secretKey);

      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

      // Préfixer l'IV au texte chiffré pour le stockage
      byte[] combined = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      log.error("Erreur de chiffrement AES-GCM : {}", e.getMessage());
      throw new IllegalStateException("Échec du chiffrement du token", e);
    }
  }

  /**
   * Déchiffre un texte chiffré avec AES-256-GCM.
   *
   * @param encryptedBase64 texte chiffré encodé en Base64
   * @param secretKey clé secrète brute (identique à celle utilisée pour le chiffrement)
   * @return texte en clair
   */
  public static String decrypt(String encryptedBase64, String secretKey) {
    try {
      SecretKeySpec keySpec = deriveKey(secretKey);

      byte[] combined = Base64.getDecoder().decode(encryptedBase64);
      byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
      byte[] cipherText = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

      byte[] decrypted = cipher.doFinal(cipherText);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Erreur de déchiffrement AES-GCM : {}", e.getMessage());
      throw new IllegalStateException("Échec du déchiffrement du token", e);
    }
  }

  /** Dérive une clé AES-256 depuis un secret brut via SHA-256. */
  private static SecretKeySpec deriveKey(String secret) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
    return new SecretKeySpec(keyBytes, "AES");
  }
}
