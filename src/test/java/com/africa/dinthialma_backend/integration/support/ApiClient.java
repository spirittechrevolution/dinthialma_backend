package com.africa.dinthialma_backend.integration.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client HTTP des tests d'intégration (client du JDK : gère PATCH et les réponses binaires). Les
 * statuts d'erreur ne lèvent pas d'exception : chaque test vérifie lui-même le statut et le {@code
 * code} renvoyés.
 */
public class ApiClient {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final String baseUrl;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public ApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Reponse get(String chemin, String jeton) {
    return envoyer("GET", chemin, jeton, null);
  }

  public Reponse post(String chemin, String jeton, Object corps) {
    return envoyer("POST", chemin, jeton, corps);
  }

  public Reponse put(String chemin, String jeton, Object corps) {
    return envoyer("PUT", chemin, jeton, corps);
  }

  public Reponse patch(String chemin, String jeton, Object corps) {
    return envoyer("PATCH", chemin, jeton, corps);
  }

  public Reponse delete(String chemin, String jeton) {
    return envoyer("DELETE", chemin, jeton, null);
  }

  /** Corps brut (ex. JSON volontairement illisible). */
  public Reponse postBrut(String chemin, String jeton, String corpsBrut) {
    return executer("POST", chemin, jeton, HttpRequest.BodyPublishers.ofString(corpsBrut));
  }

  private Reponse envoyer(String methode, String chemin, String jeton, Object corps) {
    try {
      HttpRequest.BodyPublisher publisher =
          corps == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(corps));
      return executer(methode, chemin, jeton, publisher);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Reponse executer(
      String methode, String chemin, String jeton, HttpRequest.BodyPublisher corps) {
    try {
      HttpRequest.Builder req =
          HttpRequest.newBuilder(URI.create(baseUrl + chemin))
              .timeout(Duration.ofSeconds(60))
              .header("Content-Type", "application/json")
              .method(methode, corps);
      if (jeton != null) req.header("Authorization", "Bearer " + jeton);
      HttpResponse<byte[]> res = http.send(req.build(), HttpResponse.BodyHandlers.ofByteArray());
      return new Reponse(res.statusCode(), res.body(), res.headers().map());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  /** Réponse HTTP : statut, corps JSON ({@link #json()}) ou brut ({@link #octets()}), en-têtes. */
  public record Reponse(int statut, byte[] octets, Map<String, List<String>> entetes) {

    public JsonNode json() {
      try {
        return octets.length == 0 ? NullNode.getInstance() : JSON.readTree(octets);
      } catch (IOException e) {
        throw new IllegalStateException("Réponse non JSON : " + texte(), e);
      }
    }

    /** Champ {@code data} de l'enveloppe CustomResponse. */
    public JsonNode data() {
      return json().path("data");
    }

    /** Champ {@code code} (erreurs) – texte vide si absent. */
    public String code() {
      return json().path("code").asText("");
    }

    public String texte() {
      return new String(octets, StandardCharsets.UTF_8);
    }

    public String entete(String nom) {
      return entetes.entrySet().stream()
          .filter(e -> e.getKey().equalsIgnoreCase(nom))
          .map(e -> String.join(",", e.getValue()))
          .findFirst()
          .orElse("");
    }

    @Override
    public String toString() {
      return "HTTP " + statut + " " + texte();
    }
  }
}
