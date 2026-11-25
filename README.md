# 🚨 Safe Route — MS4 SoS Service (Architecture Microservices Complète)

**Étudiante responsable :** Imen  
**Projet :** Safe Route (Module AWD — Architecture Web Distribuée, 4A ESPRIT)  
**Microservice :** `MS4 – SoS Service` (Gestion des urgences SOS & contacts d'urgence)  
**Stack :** Spring Boot `4.0.10` · Spring Cloud `2025.1.4` · Java `17` · Eureka · Spring Cloud Gateway · OpenFeign · RabbitMQ · Keycloak · PostgreSQL / H2 · Docker Compose · OpenAPI / Swagger

---

## 📋 Table des matières
1. [Vue d'ensemble de l'architecture](#1-vue-densemble-de-larchitecture)
2. [Structure du projet](#2-structure-du-projet)
3. [Endpoints REST exposés & RFC 9457](#3-endpoints-rest-exposés--rfc-9457)
4. [Communication Synchrone (S6 - OpenFeign) & Asynchrone (S7 - RabbitMQ)](#4-communication-synchrone-s6---openfeign--asynchrone-s7---rabbitmq)
5. [Sécurité Keycloak & Rôles (S8)](#5-sécurité-keycloak--rôles-s8)
6. [Observabilité Spring Boot Actuator & Liaison Eureka (Chapitre 4)](#6-observabilité-spring-boot-actuator--liaison-eureka-chapitre-4)
7. [Lancement en Mode Démo Local (H2 — sans Docker)](#7-lancement-en-mode-démo-local-h2--sans-docker)
8. [Lancement Complet avec Docker Compose (S9)](#8-lancement-complet-avec-docker-compose-s9)
9. [Tests automatisés](#9-tests-automatisés)

---

## 1. Vue d'ensemble de l'architecture

```
       [ Front-end / Swagger UI / Postman ]
                        │  Bearer JWT (Keycloak)
                        ▼
        ┌───────────────────────────────────┐        ┌──────────────────────────────┐
        │     API Gateway (port 8080)       ├───────►│   Keycloak (port 8180)       │
        │  Spring Cloud Gateway + JWT RBAC  │        │   Realm: saferoute           │
        └─────────────────┬─────────────────┘        └──────────────────────────────┘
                          │ Routage dynamique / explicite
                          ▼
        ┌───────────────────────────────────┐        ┌──────────────────────────────┐
        │     MS4 SoS Service (port 8084)   ├───────►│   Eureka Server (port 8761)  │
        └──┬──────────────┬──────────────┬──┘        │   Service Discovery          │
           │              │              │           └──────────────────────────────┘
           │ JPA          │ OpenFeign    │ AMQP (Async)
           ▼              ▼              ▼
    ┌────────────┐  ┌──────────┐  ┌─────────────────────────────┐
    │ PostgreSQL │  │   MS1    │  │ RabbitMQ (5672 / UI 15672)  │
    │  (sos_db)  │  │  (User)  │  │ Exchange: sos.events.exchange│
    │ ou H2 local│  └──────────┘  └─────────────────────────────┘
    └────────────┘
```

---

## 2. Structure du projet

```
projet SOS/
├── infrastructure/
│   ├── eureka-server/                 # Registre Eureka (Port 8761) + Dockerfile
│   ├── api-gateway/                   # Spring Cloud Gateway + Keycloak JWT (Port 8080) + Dockerfile
│   └── keycloak/
│       └── saferoute-realm.json       # Export automatique du realm Keycloak (rôles + utilisateurs)
├── microservices/
│   └── sos-service/                   # MS4 SoS Service (Port 8084) + Dockerfile
│       └── src/
│           ├── main/java/com/saferoute/sos/
│           │   ├── client/            # S6 - Client OpenFeign vers MS1 (UserServiceClient)
│           │   ├── config/            # OpenAPI/Swagger (avec Bearer JWT) & DataInitializer
│           │   ├── controller/        # SosController & EmergencyController
│           │   ├── dto/               # CreateSosRequest, UpdateLocationRequest, PatchSosRequest, CreateEmergencyContactRequest
│           │   ├── entity/            # SosAlert, SosStatus, EmergencyContact
│           │   ├── exception/         # GlobalExceptionHandler (RFC 9457 Problem Details)
│           │   ├── messaging/         # S7 - RabbitMQConfig, SosAlertEvent, SosEventPublisher
│           │   ├── repository/        # SosAlertRepository, EmergencyContactRepository
│           │   ├── security/          # S8 - SecurityConfig, KeycloakJwtAuthenticationConverter
│           │   └── service/           # SosService, EmergencyContactService, UserIntegrationService
│           ├── main/resources/
│           │   ├── application.properties          # Profil local (H2, sans Docker)
│           │   ├── application-postgres.properties # Profil PostgreSQL local (localhost:5432/sos_db)
│           │   ├── application-docker.properties   # Profil Docker (PostgreSQL + RabbitMQ + Keycloak)
│           │   └── application-secured.properties  # Profil local sécurisé (Keycloak sur localhost:8180)
│           └── test/java/com/saferoute/sos/        # Tests REST, Observabilité Actuator, Sécurité RBAC, Feign Fallback
├── docker-compose.yml                 # S9 - Orchestration complète (Postgres, RabbitMQ, Keycloak, Eureka, MS4, Gateway)
├── start-all.bat / start-sos-postgres.bat / start-*.bat  # Scripts de démarrage rapide (Windows)
└── README.md
```

---

## 3. Endpoints REST exposés & RFC 9457

### Ressource `/sos` (`SosController`)
| Méthode | URL | Rôle | Rôle Keycloak (mode sécurisé) | Codes HTTP |
| :--- | :--- | :--- | :--- | :--- |
| **POST** | `/sos` | Déclencher une alerte SOS | Authentifié (`USER` / `ADMIN`) | `201 Created` + header `Location`, `400` |
| **GET** | `/sos/me` | Consulter mes alertes SOS (`?userId=` ou header `X-User-Id`) | Authentifié | `200 OK` |
| **GET** | `/sos/{id}` | Consulter une alerte par ID | Authentifié | `200 OK`, `400`, `404` |
| **PUT** | `/sos/{id}/location` | Mise à jour GPS temps réel | Authentifié | `200 OK`, `400`, `404`, `409` |
| **PATCH** | `/sos/{id}` | Modification partielle (position, message, statut) | Authentifié | `200 OK`, `400`, `404`, `409` |
| **PUT** | `/sos/{id}/cancel` | Annuler mon alerte SOS | Authentifié | `200 OK`, `404`, `409` |
| **PUT** | `/sos/{id}/resolve` | Marquer comme résolue | **`ADMIN` uniquement** | `200 OK`, `403`, `404`, `409` |
| **GET** | `/sos/active` | Lister les alertes `ACTIVE` / `EN_COURS` | **`ADMIN` uniquement** | `200 OK`, `403` |

### Ressource `/emergency` (`EmergencyController`)
| Méthode | URL | Rôle | Codes HTTP |
| :--- | :--- | :--- | :--- |
| **POST** | `/emergency/sos` | Alias de déclenchement d'alerte SOS | `201 Created` + `Location`, `400` |
| **GET** | `/emergency/contacts` | Lister les contacts d'urgence | `200 OK` |
| **POST** | `/emergency/contacts` | Ajouter un contact d'urgence (validé par DTO) | `201 Created` + `Location`, `400` |
| **DELETE** | `/emergency/contacts/{id}` | Supprimer un contact d'urgence | `204 No Content`, `404` |

### Gestion normalisée des erreurs (RFC 9457 — `application/problem+json`)
Toutes les erreurs (`400`, `401`, `403`, `404`, `409`, `500`) retournent un corps JSON standard `ProblemDetail` sans aucune stack trace ni message SQL :
```json
{
  "type": "about:blank",
  "title": "Conflit métier",
  "status": 409,
  "detail": "Impossible de annuler l'alerte #3 : elle est déjà clôturée avec le statut RESOLUE.",
  "instance": "/sos/3",
  "timestamp": "2026-11-25T10:30:00Z"
}
```

---

## 4. Communication Synchrone (S6 - OpenFeign) & Asynchrone (S7 - RabbitMQ)

- **Synchrone (OpenFeign → MS1 `user-service` - Foued)** :
  - Interface `UserServiceClient` (`@FeignClient(name = "user-service", path = "/users")`).
  - `UserIntegrationService` interroge MS1 pour récupérer les contacts d'urgence et bascule automatiquement sur le référentiel local (`EmergencyContactRepository`) si MS1 est indisponible (tolérance aux pannes).
- **Asynchrone (RabbitMQ → Notification / Temps réel)** :
  - Exchange Topic : `sos.events.exchange`
  - Queues & routing keys :
    - `sos.triggered.queue` (`sos.alert.triggered`)
    - `sos.location.updated.queue` (`sos.alert.location.updated`)
    - `sos.status.changed.queue` (`sos.alert.status.changed`)
  - En cas de panne du broker, l'alerte SOS reste sauvegardée (découplage temporel).

---

## 5. Sécurité Keycloak & Rôles (S8)

Le realm `saferoute` est importé automatiquement par Docker Compose via `infrastructure/keycloak/saferoute-realm.json` :
- **Console Admin Keycloak** : `http://localhost:8180` (`admin` / `admin`)
- **Client OIDC** : `saferoute-client`
- **Comptes de démonstration** :
  - Citoyen : `user` / `user123` (rôle `USER`)
  - Administrateur : `admin` / `admin123` (rôles `ADMIN`, `USER`)

**Obtenir un jeton JWT pour tester via Postman / Swagger ("Authorize") :**
```bash
curl -X POST http://localhost:8180/realms/saferoute/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=saferoute-client" \
  -d "username=admin" \
  -d "password=admin123"
```

---

## 6. Observabilité Spring Boot Actuator & Liaison Eureka (Chapitre 4)

Chaque microservice expose les endpoints **Spring Boot Actuator** et remonte sa **vraie santé** dans le dashboard Eureka (`eureka.client.healthcheck.enabled=true`) :

| Endpoint | Rôle | Sécurité (mode Keycloak) |
| :--- | :--- | :--- |
| `GET /actuator/health` | Santé globale + composants (`db`, `diskSpace`, `ping`, **`sosEmergencyEngine`** personnalisé) | Public (Eureka / Docker) |
| `GET /actuator/health/liveness` & `/readiness` | Sondes orchestrateur (Docker / Kubernetes) | Public |
| `GET /actuator/info` | Métadonnées (`app.name`, `app.version`, `app.owner`, `app.team`) ouvertes au clic dans Eureka | Public |
| `GET /actuator/metrics` | Métriques JVM, HTTP (`http.server.requests`) et **métriques métier** (`sos.alerts.triggered.total`, `sos.alerts.resolved.total`, `sos.alerts.cancelled.total`) | **`ADMIN` uniquement** |
| `GET` / `POST /actuator/loggers/{pkg}` | Consulter ou modifier le niveau de log **à chaud sans redémarrer** | **`ADMIN` uniquement** |

**Modifier le niveau de log à chaud (sans redémarrer) :**
```bash
curl -X POST http://localhost:8084/actuator/loggers/com.saferoute.sos \
  -H "Content-Type: application/json" \
  -d "{\"configuredLevel\": \"DEBUG\"}"
```

**Lancer une 2ᵉ instance de MS4 (`sos-service`) sur un autre port (Atelier Eureka — montée en charge) :**
```powershell
cd "microservices\sos-service"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8094"
```
Les deux instances (`sos-service:8084` et `sos-service:8094`) apparaissent simultanément dans Eureka (`http://localhost:8761`), et l'API Gateway répartit les requêtes automatiquement (Round-Robin).

---

## 7. Lancement en Mode Démo Local (H2 — sans Docker)

Idéal pour la démonstration rapide (Validation 1 / développement local sans installer PostgreSQL, RabbitMQ ni Keycloak) :
1. Double-cliquer sur **`start-all.bat`** (ou lancer dans l'ordre `start-eureka.bat` → `start-sos.bat` → `start-gateway.bat`).
   - *Pour utiliser un serveur **PostgreSQL local** (`localhost:5432/sos_db`) au lieu de H2 : lancer **`start-sos-postgres.bat`** (profil `postgres`).*
2. Vérifier :
   - **Eureka Dashboard** : `http://localhost:8761`
   - **Swagger UI** : `http://localhost:8084/swagger-ui/index.html`
   - **API Gateway** : `http://localhost:8080/sos/active` ou `http://localhost:8080/sos-service/sos/active`
   - **Console H2** : `http://localhost:8084/h2-console` (JDBC URL : `jdbc:h2:mem:sosdb`, user `sa`, password vide)

---

## 8. Lancement Complet avec Docker Compose (S9)

Active automatiquement **PostgreSQL (`sos_db`)**, **RabbitMQ**, **Keycloak**, **Eureka**, **MS4 SoS Service** et **API Gateway** :
```bash
docker compose up --build
```

URLs disponibles :
- **API Gateway** : `http://localhost:8080`
- **Eureka Dashboard** : `http://localhost:8761`
- **Swagger UI (MS4)** : `http://localhost:8084/swagger-ui/index.html`
- **Keycloak** : `http://localhost:8180` (`admin` / `admin`)
- **RabbitMQ Management** : `http://localhost:15672` (`guest` / `guest`)

---

## 9. Tests automatisés

Pour exécuter tous les tests unitaires, d'intégration REST (RFC 9457), de repli OpenFeign et de sécurité RBAC Keycloak :
```powershell
cd "microservices\sos-service"; .\mvnw.cmd test
cd "infrastructure\eureka-server"; .\mvnw.cmd test
cd "infrastructure\api-gateway"; .\mvnw.cmd test
```
# SOS
