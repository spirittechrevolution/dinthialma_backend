# ===================================================================
# Dinthialma Backend – Dockerfile multi-stage
# ===================================================================
# Stage 1 : BUILD  – compile & package avec Maven
# Stage 2 : RUNTIME – image minimale JRE (sans outils de build)
# ===================================================================

# ─── Stage 1 : Build ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copier uniquement les descripteurs Maven en premier pour profiter
# du cache Docker sur les dépendances (recompilation rapide si seul
# le code source change)
COPY mvnw ./
COPY .mvn/ .mvn/
COPY pom.xml ./

# Rendre le wrapper exécutable et télécharger les dépendances
RUN chmod +x mvnw \
    && ./mvnw dependency:go-offline -B --no-transfer-progress

# Copier le code source
COPY src/ src/

# Construire le JAR en ignorant les tests (exécutés en CI séparément)
RUN ./mvnw package -DskipTests -B --no-transfer-progress

# Extraire les couches Spring Boot pour un cache Docker optimal
RUN java -Djarmode=layertools \
         -jar target/dinthialma-backend-0.0.1-SNAPSHOT.jar extract


# ─── Stage 2 : Runtime ───────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Métadonnées OCI
LABEL org.opencontainers.image.title="Dinthialma Backend"
LABEL org.opencontainers.image.version="0.0.1-SNAPSHOT"
LABEL org.opencontainers.image.vendor="Spirit Tech Revolution"

# Utilisateur non-root pour la sécurité (least-privilege)
RUN addgroup -S dinthialma && adduser -S dinthialma -G dinthialma
USER dinthialma

WORKDIR /app

# Copier les couches extraites dans l'ordre Spring Boot recommandé
# (des plus stables aux plus volatiles → meilleur cache Docker)
COPY --from=builder --chown=dinthialma:dinthialma /workspace/dependencies          ./
COPY --from=builder --chown=dinthialma:dinthialma /workspace/spring-boot-loader    ./
COPY --from=builder --chown=dinthialma:dinthialma /workspace/snapshot-dependencies ./
COPY --from=builder --chown=dinthialma:dinthialma /workspace/application           ./

# Port exposé (context-path /api configuré dans application.yml)
EXPOSE 8080

# Healthcheck via Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

# Point d'entrée avec optimisations JVM pour un conteneur
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
