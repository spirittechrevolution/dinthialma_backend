#!/bin/bash
# ===================================================================
# PostgreSQL Init Script – Dinthialma Backend
# Exécuté une seule fois lors de la création du volume PostgreSQL
# ===================================================================
# ⚠️  Ce fichier doit avoir des fins de ligne LF (Unix), pas CRLF
#     Sur Windows : git config core.autocrlf false
# ===================================================================
set -e

echo ">>> Initialisation des bases de données Dinthialma..."

# ─── Création de db-keycloak ──────────────────────────────────────
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE "db-keycloak-dinthialma"'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db-keycloak-dinthialma')
    \gexec
EOSQL
echo ">>> Base db-keycloak-dinthialma : OK"

# ─── Schéma "dinthialma" dans la base principale ──────────────────
# Requis par Hibernate : spring.jpa.properties.hibernate.default_schema
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE SCHEMA IF NOT EXISTS dinthialma;
EOSQL
echo ">>> Schéma 'dinthialma' dans $POSTGRES_DB : OK"

echo ">>> Initialisation terminée."
