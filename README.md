# Distributed Systems – Blog API

Dieses Projekt implementiert eine REST-API zur Verwaltung von Blog-Beiträgen mit **Quarkus**.

Die REST-Schnittstelle basiert auf **Jakarta REST**. Die Daten werden im JSON-Format übertragen und mit Hibernate ORM / Panache persistiert.

Zusätzlich verfügt die Anwendung über:

- Bean Validation
- Request- und Response-DTOs
- rollenbasierte Zugriffskontrolle
- Authentifizierung mit OpenID Connect / Keycloak
- benutzerbezogene Ownership-Prüfung
- automatisierte REST- und Security-Tests
- Blackbox-Integrationstests gegen das fertige Container-Image
- Containerisierung mit Docker
- Veröffentlichung des Backend-Images über GitHub Container Registry (GHCR)
- eingebettete H2 In-Memory-Datenbank im Container-Betrieb
- durchgängiges Logging mit `io.quarkus.logging.Log`

---

# Architektur

Im Container-Betrieb besteht das System aus folgenden Komponenten:

```text
Client / HTTPie / Swagger UI
          |
          v
   Quarkus Blog Backend
          |
          +---- H2 In-Memory
          |
          +---- OIDC ----> Keycloak
```

Das Blog-Backend wird als Docker-Image über GitHub Container Registry bereitgestellt.

Die Blog-Datenbank läuft als **H2 In-Memory-Datenbank innerhalb des Backend-Prozesses**.

Keycloak läuft separat und übernimmt:

- Authentifizierung
- Benutzerverwaltung
- Rollenverwaltung
- Ausstellung von JWT Access-Tokens

---

# Container-Image

Das Quarkus-Backend wurde als JVM-Container-Image erstellt und auf GitHub Container Registry veröffentlicht.

Image:

```text
ghcr.io/nickhftm/blog-backend:latest
```

Das Image ist öffentlich verfügbar.

## Image herunterladen

```bash
docker pull ghcr.io/nickhftm/blog-backend:latest
```

## Container-Image-Konfiguration

Die Image-Metadaten sind in `application.properties` definiert:

```properties
quarkus.container-image.registry=ghcr.io
quarkus.container-image.group=nickhftm
quarkus.container-image.name=blog-backend
quarkus.container-image.tag=latest
```

Ein neues Image kann mit folgendem Befehl gebaut werden:

```bash
./mvnw clean package \
  -DskipTests \
  -Dquarkus.container-image.build=true
```

Für die vollständige Validierung inklusive Integrationstests wird stattdessen verwendet:

```bash
./mvnw verify \
  -DskipITs=false \
  -Dquarkus.container-image.build=true
```

Dabei wird das Container-Image gebaut und anschließend automatisiert als Blackbox getestet.

---

# Datenbank

## H2 In-Memory im Container-Betrieb

Das Backend verwendet im Prod-/Container-Profil eine eingebettete **H2 In-Memory-Datenbank**.

Dadurch ist für die Blog-Anwendung kein zusätzlicher Datenbank-Container notwendig.

```properties
%prod.quarkus.datasource.db-kind=h2
%prod.quarkus.datasource.jdbc.url=jdbc:h2:mem:blogdb
%prod.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
```

Das Schema wird beim Start automatisch erstellt.

## Test-Datenbank

Für automatisierte Tests wird ebenfalls H2 verwendet.

```properties
%test.quarkus.datasource.devservices.enabled=false
%test.quarkus.datasource.db-kind=h2
%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
%test.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
```

Damit benötigen auch die Blackbox-Integrationstests keinen externen Datenbankserver.

## Einschränkung

Die Daten befinden sich nur im Arbeitsspeicher.

Wird das Backend beendet oder der Container neu gestartet, gehen die gespeicherten Blog-Beiträge verloren.

H2 wird bewusst als einfache Lösung für diese Ausbildungs- und Containerumgebung eingesetzt.

Für einen produktiven Betrieb wäre eine persistente Datenbank wie PostgreSQL oder MySQL sinnvoll.

---

# Keycloak

Für Authentifizierung und Rollenverwaltung wird **Keycloak** verwendet.

Der Realm wurde aus der manuell konfigurierten und getesteten Keycloak-Instanz exportiert.

Damit können Realm, Client, Rollen und Test-Benutzer reproduzierbar importiert werden.

## Realm

```text
blog
```

## Client

```text
blog-api
```

Beim Client ist **Direct Access Grants** aktiviert.

## Rollen

- `reader`
- `author`
- `admin`

## Test-Benutzer

| Benutzer | Rolle | Verwendung |
|---|---|---|
| `reader1` | `reader` | Test fehlender Schreibrechte |
| `author1` | `author` | Erstellt und bearbeitet eigene Blogs |
| `author2` | `author` | Test der Ownership-Prüfung |
| `admin1` | `admin` | Administrative Zugriffe |

Die Benutzer dienen ausschließlich der lokalen Test- und Ausbildungsumgebung.

---

# Keycloak starten

Die exportierte Realm-Konfiguration kann beim Start automatisch importiert werden.

Beispiel:

```bash
docker run --name blog-keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -v "$HOME/blog-keycloak:/opt/keycloak/data/import" \
  quay.io/keycloak/keycloak:22.0.1 \
  start-dev --import-realm
```

Falls der Container bereits existiert:

```bash
docker start blog-keycloak
```

Status prüfen:

```bash
docker ps
```

Keycloak:

```text
http://localhost:8180
```

Nach dem Import muss der Realm `blog` vorhanden sein.

---

# OIDC-Konfiguration

Die lokale Standardkonfiguration lautet:

```properties
quarkus.oidc.auth-server-url=http://localhost:8180/realms/blog
quarkus.oidc.client-id=blog-api
```

Innerhalb des Backend-Containers zeigt `localhost` jedoch auf den Backend-Container selbst.

Deshalb wird die Keycloak-Adresse beim Containerstart überschrieben:

```text
QUARKUS_OIDC_AUTH_SERVER_URL
```

Unter Docker Desktop wird verwendet:

```text
http://host.docker.internal:8180/realms/blog
```

---

# Backend lokal als JAR starten

Projekt bauen:

```bash
./mvnw clean package
```

Danach:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

Die Anwendung ist erreichbar unter:

```text
http://localhost:8080
```

---

# Backend als Container starten

Zuerst das veröffentlichte Image laden:

```bash
docker pull ghcr.io/nickhftm/blog-backend:latest
```

Danach:

```bash
docker run --rm \
  --name blog-backend \
  -p 8080:8080 \
  -e QUARKUS_OIDC_AUTH_SERVER_URL=http://host.docker.internal:8180/realms/blog \
  ghcr.io/nickhftm/blog-backend:latest
```

Das Backend läuft im Container im Prod-Profil und verwendet die eingebettete H2-Datenbank.

---

# REST-Design

## Endpoints

| HTTP-Methode | Pfad | Beschreibung | Zugriff |
|---|---|---|---|
| GET | `/blogs` | Alle Blogs laden | öffentlich |
| GET | `/blogs?title={title}` | Blogs nach Titel suchen | öffentlich |
| GET | `/blogs/{id}` | Einzelnen Blog laden | öffentlich |
| POST | `/blogs` | Blog erstellen | `author`, `admin` |
| PUT | `/blogs/{id}` | Blog bearbeiten | `author`, `admin` + Ownership |
| DELETE | `/blogs/{id}` | Blog löschen | `admin` |

---

# Validierung

Für die Eingabevalidierung wird **Jakarta Bean Validation** verwendet.

Regeln:

- `title` darf nicht leer sein
- `content` darf nicht leer sein

Beispiel:

```json
{
  "title": "",
  "content": ""
}
```

Ein authentifizierter Benutzer erhält:

```text
400 Bad Request
```

Ein nicht authentifizierter Benutzer erhält bei einem geschützten Endpoint bereits vorher:

```text
401 Unauthorized
```

---

# DTOs

Für `POST /blogs` werden eigene Request- und Response-DTOs verwendet.

## CreateBlogRequest

Vom Client dürfen gesetzt werden:

- `title`
- `content`

Nicht direkt setzbar sind:

- `id`
- `owner`
- `comments`

Der Besitzer wird serverseitig aus dem authentifizierten Benutzer ermittelt.

## BlogResponse

Der Response enthält:

- `id`
- `title`
- `content`

Interne Informationen wie `owner` werden nicht direkt freigegeben.

---

# Berechtigungskonzept

Die Zugriffskontrolle besteht aus:

1. Rollenprüfung
2. Ownership-Prüfung

## reader

Ein `reader` kann Blogs lesen.

Da GET-Endpunkte öffentlich sind, ist dafür kein Login erforderlich.

## author

Ein `author` darf:

- Blogs lesen
- Blogs erstellen
- eigene Blogs bearbeiten

Ein `author` darf nicht:

- fremde Blogs bearbeiten
- Blogs löschen

## admin

Ein `admin` darf:

- Blogs lesen
- Blogs erstellen
- alle Blogs bearbeiten
- Blogs löschen

## Zugriffsmatrix

| Funktion | öffentlich | reader | author | admin |
|---|---:|---:|---:|---:|
| Blogs lesen | Ja | Ja | Ja | Ja |
| Blog erstellen | Nein | Nein | Ja | Ja |
| Eigenen Blog bearbeiten | Nein | Nein | Ja | Ja |
| Fremden Blog bearbeiten | Nein | Nein | Nein | Ja |
| Blog löschen | Nein | Nein | Nein | Ja |

---

# Umsetzung der Security

Öffentliche Endpunkte:

```java
@PermitAll
```

Erstellen und Bearbeiten:

```java
@RolesAllowed({"author", "admin"})
```

Löschen:

```java
@RolesAllowed("admin")
```

Der angemeldete Benutzer wird über `SecurityIdentity` ermittelt:

```java
@Inject
SecurityIdentity securityIdentity;
```

```java
String username = securityIdentity.getPrincipal().getName();
```

Ownership-Prüfung:

```java
boolean isOwner = username.equals(existingBlog.getOwner());
boolean isAdmin = securityIdentity.hasRole("admin");

if (!isAdmin && !isOwner) {
    return Response.status(Response.Status.FORBIDDEN).build();
}
```

Damit kann ein `author` nur eigene Blogs bearbeiten.

---

# HTTP-Statuscodes

| Situation | Status |
|---|---|
| Öffentlicher Request erfolgreich | `200 OK` |
| Geschützter Endpoint ohne Token | `401 Unauthorized` |
| Rolle nicht ausreichend | `403 Forbidden` |
| Author bearbeitet fremden Blog | `403 Forbidden` |
| Blog nicht vorhanden | `404 Not Found` |
| Blog erstellt | `201 Created` |
| Blog gelöscht | `204 No Content` |

---

# Access-Token mit HTTPie abrufen

Beispiel mit `author1`:

```bash
python3 -m httpie --form POST \
  http://localhost:8180/realms/blog/protocol/openid-connect/token \
  client_id=blog-api \
  username=author1 \
  password=PASSWORD \
  grant_type=password
```

Keycloak liefert unter anderem:

```json
{
  "access_token": "eyJ..."
}
```

Passwörter und vollständige Access-Tokens werden nicht im Repository oder in Logs gespeichert.

---

# Token in Shell-Variable speichern

```bash
TOKEN=$(python3 -m httpie --body --form POST \
  http://localhost:8180/realms/blog/protocol/openid-connect/token \
  client_id=blog-api \
  username=author1 \
  password=PASSWORD \
  grant_type=password | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

---

# Öffentlichen Endpoint mit HTTPie testen

```bash
python3 -m httpie GET http://localhost:8080/blogs
```

Erwartet:

```text
200 OK
```

---

# Geschützten Endpoint mit HTTPie testen

```bash
python3 -m httpie POST http://localhost:8080/blogs \
  Authorization:"Bearer $TOKEN" \
  title="Container Test" \
  content="Dieser Blog wurde über HTTPie erstellt"
```

Erwartet:

```text
201 Created
```

---

# Ownership testen

`author1` erstellt einen Blog.

Versucht anschließend `author2`, diesen Blog zu verändern:

```text
403 Forbidden
```

Ein `admin` darf denselben Blog bearbeiten:

```text
200 OK
```

Damit wird die Ownership-Prüfung zusätzlich zur Rollenprüfung durchgesetzt.

---

# Swagger UI

Swagger UI ist auch im Prod-/Container-Build enthalten.

```properties
quarkus.swagger-ui.always-include=true
```

Swagger UI:

```text
http://localhost:8080/q/swagger-ui
```

OpenAPI:

```text
http://localhost:8080/q/openapi
```

---

# Logging

Die Anwendung verwendet durchgängig:

```java
io.quarkus.logging.Log
```

Es wird kein eigener Logger über `LoggerFactory` erzeugt.

## Logging-Konzept

### DEBUG

Technische Details:

- Suchanfragen
- Datenbankzugriffe
- Anzahl geladener Einträge

### INFO

Wichtige System- und Geschäftsereignisse:

- Backend gestartet
- Blog erstellt
- Blog aktualisiert
- Blog gelöscht
- erfolgreicher Zugriff

### WARN

Auffällige oder unerlaubte Ereignisse:

- Blog nicht gefunden
- Author versucht fremden Blog zu verändern
- unerlaubte Zugriffsversuche

### ERROR

Technische Fehler und unerwartete Exceptions.

---

# Startup-Logging

Beim Start werden wichtige Systemzustände protokolliert.

Beispiel:

```text
Blog Backend wurde erfolgreich gestartet
Persistenz ist konfiguriert
Authentifizierung: OIDC / Keycloak konfiguriert
```

---

# Logging-Konfiguration

```properties
quarkus.log.level=INFO

%dev.quarkus.log.category."ch.hftm".level=DEBUG
%dev.quarkus.log.category."org.hibernate.SQL".level=DEBUG

%prod.quarkus.log.category."ch.hftm".level=INFO
```

Im Container werden Logs auf `stdout` und `stderr` geschrieben.

Dadurch können sie durch Docker oder später beispielsweise Kubernetes und Grafana Loki gesammelt werden.

## Sicherheitsregel

Folgende Informationen werden nicht geloggt:

- Passwörter
- vollständige Access-Tokens
- Client-Secrets
- andere sensible Zugangsdaten

---

# Automatisierte Tests

Die Anwendung verwendet:

- JUnit
- `@QuarkusTest`
- REST Assured
- `@TestSecurity`

Tests ausführen:

```bash
./mvnw test
```

Aktueller Stand:

```text
Tests run: 14
Failures: 0
Errors: 0
```

---

# Abgedeckte Security-Tests

Unter anderem werden folgende Fälle automatisiert geprüft:

```text
GET ohne Login          -> 200
POST ohne Login         -> 401
reader POST             -> 403
author POST             -> 201
author eigener Blog     -> 200
author fremder Blog     -> 403
admin fremder Blog      -> 200
author DELETE           -> 403
admin DELETE            -> 204
```

Damit werden Rollen und Ownership automatisch geprüft.

---

# Blackbox-Integrationstests

Als optionale Vertiefung wurden zusätzlich **Blackbox-Integrationstests mit `@QuarkusIntegrationTest`** umgesetzt.

Die Tests befinden sich unter anderem in:

```text
src/test/java/ch/hftm/BlogResourceIT.java
```

Sie testen nicht nur die Anwendung innerhalb des Quarkus-Testkontexts, sondern die fertig gebaute Auslieferung.

## Ablauf

Maven führt dabei folgende Schritte aus:

```text
compile
   ↓
test
   ↓
package
   ↓
Container-Image bauen
   ↓
Container starten
   ↓
@QuarkusIntegrationTest
   ↓
verify
```

Ausführung:

```bash
./mvnw verify \
  -DskipITs=false \
  -Dquarkus.container-image.build=true
```

Quarkus baut dabei:

```text
ghcr.io/nickhftm/blog-backend:latest
```

und startet anschließend genau dieses Image automatisch als Docker-Container.

Für den Integrationstest wird die Keycloak-Adresse in den gestarteten Container übergeben:

```properties
quarkus.test.env.QUARKUS_OIDC_AUTH_SERVER_URL=http://host.docker.internal:8180/realms/blog
```

## Getestete Blackbox-Fälle

`BlogResourceIT` prüft:

```text
GET /blogs             -> 200
GET /q/swagger-ui      -> 200
POST /blogs ohne Token -> 401
```

Der letzte Testlauf war erfolgreich:

```text
BlogResourceIT:
Tests run: 3
Failures: 0
Errors: 0

Gesamte Integrationstests:
Tests run: 4
Failures: 0
Errors: 0

BUILD SUCCESS
```

Damit wird die tatsächlich ausgelieferte Container-Version vor dem Push automatisiert validiert.

---

# CORS

```properties
quarkus.http.cors.enabled=true
quarkus.http.cors.origins=http://localhost:3000
quarkus.http.cors.methods=GET,POST,PUT,DELETE
quarkus.http.cors.headers=Content-Type,Authorization
```

Der `Authorization`-Header ist explizit erlaubt.

---

# API Security / OWASP

## Broken Object Level Authorization

Ein `author` darf nur eigene Blogs bearbeiten.

Ein `admin` darf alle Blogs bearbeiten.

**Status:** weitgehend durch Rollen und Ownership-Prüfung abgedeckt.

## Broken Authentication

Authentifizierung erfolgt über Keycloak und OpenID Connect.

**Status:** umgesetzt.

## Broken Object Property Level Authorization

DTOs verhindern, dass Clients beispielsweise `id`, `owner` oder `comments` beim Erstellen frei setzen können.

**Status:** teilweise durch DTOs abgedeckt.

## Unrestricted Resource Consumption

Noch offen:

- Rate Limiting
- Pagination
- Request-Limits

## Security Misconfiguration

CORS ist eingeschränkt.

Secrets und Tokens werden nicht in Logs oder im Repository gespeichert.

---

# Anwendung komplett starten

## 1. Keycloak starten

Falls bereits vorhanden:

```bash
docker start blog-keycloak
```

Ansonsten mit Realm-Import:

```bash
docker run --name blog-keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -v "$HOME/blog-keycloak:/opt/keycloak/data/import" \
  quay.io/keycloak/keycloak:22.0.1 \
  start-dev --import-realm
```

## 2. Backend herunterladen

```bash
docker pull ghcr.io/nickhftm/blog-backend:latest
```

## 3. Backend starten

```bash
docker run --rm \
  --name blog-backend \
  -p 8080:8080 \
  -e QUARKUS_OIDC_AUTH_SERVER_URL=http://host.docker.internal:8180/realms/blog \
  ghcr.io/nickhftm/blog-backend:latest
```

## 4. API testen

```bash
python3 -m httpie GET http://localhost:8080/blogs
```

---

# Wichtige URLs

| Dienst | URL |
|---|---|
| Blog API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/q/swagger-ui` |
| OpenAPI | `http://localhost:8080/q/openapi` |
| Keycloak | `http://localhost:8180` |

---

# Aktueller Stand

```text
[✓] Quarkus Container-Image erstellt
[✓] finales Image auf GitHub Container Registry veröffentlicht
[✓] Image öffentlich verfügbar
[✓] H2 für Container-Betrieb konfiguriert
[✓] Keycloak-Realm exportiert und reproduzierbar importierbar
[✓] Start des Gesamtsystems dokumentiert
[✓] HTTPie-Aufrufe inklusive Authentifizierung dokumentiert
[✓] durchgängiges Logging mit io.quarkus.logging.Log
[✓] Startup-, Geschäfts- und Security-Logging
[✓] keine Secrets oder vollständigen JWTs in Logs
[✓] 14 automatisierte REST- und Security-Tests erfolgreich
[✓] Blackbox-Integrationstests mit @QuarkusIntegrationTest umgesetzt
[✓] Container-Image automatisiert gestartet und getestet
[✓] 3 Blog-Blackbox-Tests erfolgreich
[✓] Maven verify mit BUILD SUCCESS
[✓] final getestetes Container-Image erneut nach GHCR gepusht
```

---

# Weitere mögliche Vertiefungen

Noch nicht umgesetzt sind beispielsweise:

- natives GraalVM-Container-Image
- Vergleich von JVM- und Native-Startzeit
- Vergleich des Ressourcenverbrauchs
- `.env`-Datei oder eigene Config-Mappings für Secrets
- persistente PostgreSQL-/MySQL-Datenbank
- Pagination
- Rate Limiting
- zentrale Exception-Mapper

---

# Zusammenfassung

Die Anwendung kann vollständig containerisiert betrieben werden.

Das Backend wird über

```text
ghcr.io/nickhftm/blog-backend:latest
```

bereitgestellt.

Keycloak übernimmt Authentifizierung und Rollenverwaltung. Die Blog-Daten werden im Container-Betrieb über H2 In-Memory gespeichert.

Geschützte REST-Endpunkte verwenden Rollen und eine zusätzliche Ownership-Prüfung.

Systemzustände, Geschäftsereignisse und Sicherheitsverletzungen werden mit `io.quarkus.logging.Log` protokolliert.

Die REST- und Security-Regeln werden durch 14 automatisierte Tests geprüft.

Zusätzlich wird das fertig gebaute Container-Image mit `@QuarkusIntegrationTest` als Blackbox gestartet und über HTTP getestet, bevor die Auslieferung veröffentlicht wird.