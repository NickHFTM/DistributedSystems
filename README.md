# Distributed Systems – Blog API

Dieses Projekt implementiert eine REST-API zur Verwaltung von Blog-Beiträgen mit **Quarkus**.

Die REST-Schnittstelle basiert auf **Jakarta REST**. Die Daten werden im JSON-Format übertragen und mit Hibernate ORM / Panache persistiert.

Zusätzlich verfügt die API über:

- Bean Validation
- Request- und Response-DTOs
- rollenbasierte Zugriffskontrolle
- Authentifizierung mit OpenID Connect / Keycloak
- benutzerbezogene Ownership-Prüfung
- automatisierte REST- und Security-Tests

---

# REST-Design

Die API stellt Funktionen zum Erstellen, Lesen, Aktualisieren, Löschen und Suchen von Blog-Beiträgen zur Verfügung.

## Endpoints

| HTTP-Methode | Pfad | Beschreibung | Zugriff |
|---|---|---|---|
| GET | `/blogs` | Gibt alle Blog-Beiträge zurück | öffentlich |
| GET | `/blogs?title={title}` | Sucht Blog-Beiträge anhand des Titels | öffentlich |
| GET | `/blogs/{id}` | Gibt einen bestimmten Blog-Beitrag zurück | öffentlich |
| POST | `/blogs` | Erstellt einen neuen Blog-Beitrag | `author`, `admin` |
| PUT | `/blogs/{id}` | Aktualisiert einen bestehenden Blog-Beitrag | `author`, `admin` + Ownership |
| DELETE | `/blogs/{id}` | Löscht einen bestehenden Blog-Beitrag | `admin` |

---

# Validierung

Für die Eingabevalidierung wird **Jakarta Bean Validation** verwendet.

Aktuell gelten folgende Regeln:

- `title` darf nicht leer sein.
- `content` darf nicht leer sein.

Ein ungültiger Request wie

```json
{
  "title": "",
  "content": ""
}
```

wird bei einem korrekt authentifizierten Benutzer mit

```text
400 Bad Request
```

abgelehnt.

Nicht authentifizierte Benutzer erhalten bei geschützten Endpunkten zuerst

```text
401 Unauthorized
```

---

# DTOs und Mapping

Für `POST /blogs` werden eigene Request- und Response-DTOs verwendet.

## CreateBlogRequest

Freigegebene Felder:

- `title`
- `content`

Nicht direkt freigegeben sind:

- `id`
- `comments`
- `owner`

Die `id` wird von der Datenbank erzeugt. Der `owner` wird aus dem angemeldeten Benutzer ermittelt.

## BlogResponse

Der Response enthält:

- `id`
- `title`
- `content`

Interne Felder wie `owner` werden nicht direkt freigegeben.

## Mapping

```text
CreateBlogRequest
        ↓
      Blog
        ↓
Owner aus SecurityIdentity
        ↓
   Datenbank
        ↓
      Blog
        ↓
  BlogResponse
```

Beispiel:

```java
Blog blog = new Blog(request.title, request.content);

String username = securityIdentity.getPrincipal().getName();
blog.setOwner(username);

blogService.addBlog(blog);
```

---

# Berechtigungskonzept

Die Blog-API verwendet eine Kombination aus:

1. rollenbasierter Zugriffskontrolle
2. benutzerbezogener Ownership-Prüfung

## Rollen

### reader

Ein `reader` kann Blog-Beiträge lesen.

Da die GET-Endpunkte öffentlich sind, ist dafür aktuell kein Login notwendig.

### author

Ein `author` darf:

- Blogs lesen
- neue Blogs erstellen
- eigene Blogs bearbeiten

Ein `author` darf:

- keine fremden Blogs bearbeiten
- keine Blogs löschen

### admin

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

# Umsetzung der Zugriffskontrolle

Für öffentliche Endpunkte wird verwendet:

```java
@PermitAll
```

Für das Erstellen und Bearbeiten:

```java
@RolesAllowed({"author", "admin"})
```

Für das Löschen:

```java
@RolesAllowed("admin")
```

Zusätzlich wird `SecurityIdentity` verwendet, um den aktuell angemeldeten Benutzer zu identifizieren.

```java
@Inject
SecurityIdentity securityIdentity;
```

Benutzername:

```java
String username = securityIdentity.getPrincipal().getName();
```

Admin-Prüfung:

```java
boolean isAdmin = securityIdentity.hasRole("admin");
```

Ownership-Prüfung:

```java
boolean isOwner = username.equals(existingBlog.getOwner());

if (!isAdmin && !isOwner) {
    return Response.status(Response.Status.FORBIDDEN).build();
}
```

Damit reicht die Rolle `author` allein nicht aus. Ein Author muss zusätzlich Besitzer des Blogs sein.

---

# HTTP-Statuscodes der Security

| Situation | HTTP-Status |
|---|---|
| Öffentlicher Request erfolgreich | `200 OK` |
| Geschützter Endpoint ohne Login | `401 Unauthorized` |
| Benutzer angemeldet, Rolle fehlt | `403 Forbidden` |
| Author bearbeitet fremden Blog | `403 Forbidden` |
| Ressource existiert nicht | `404 Not Found` |
| Blog erfolgreich erstellt | `201 Created` |
| Blog erfolgreich gelöscht | `204 No Content` |

---

# Keycloak

Für Authentifizierung und Rollenverwaltung wird **Keycloak** verwendet.

Im Projekt wurden beide geforderten Varianten erfolgreich getestet:

1. eigener Keycloak-Docker-Container
2. Quarkus Dev Services for Keycloak

---

# Variante 1 – Eigener Keycloak-Container

## Docker-Setup

Das lokale Setup besteht aus zwei Containern:

| Container | Image | Aufgabe |
|---|---|---|
| `keycloak` | `quay.io/keycloak/keycloak:22.0.1` | Identity Provider / Authentifizierung |
| `keycloak-mysql` | `mysql:8.0` | persistente Keycloak-Datenbank |

Container starten:

```bash
docker start keycloak-mysql keycloak
```

Status prüfen:

```bash
docker ps
```

Keycloak ist erreichbar unter:

```text
http://localhost:8180
```

---

## Keycloak-Konfiguration

### Realm

```text
blog
```

### Client

```text
blog-api
```

Beim Client ist **Direct Access Grants** aktiviert.

### Realm-Rollen

- `reader`
- `author`
- `admin`

### Test-Benutzer

| Benutzer | Rolle | Verwendung |
|---|---|---|
| `reader1` | `reader` | Test fehlender Schreibrechte |
| `author1` | `author` | Erstellt und bearbeitet eigene Blogs |
| `author2` | `author` | Test der Ownership-Prüfung |
| `admin1` | `admin` | Test administrativer Zugriffe |

Für die Test-Benutzer werden nicht-temporäre Passwörter verwendet.

Die Passwörter werden nicht im Repository gespeichert.

---

# Quarkus mit eigenem Keycloak starten

In `application.properties` ist ein eigenes Profil definiert:

```properties
%keycloak.quarkus.oidc.auth-server-url=http://localhost:8180/realms/blog
%keycloak.quarkus.oidc.client-id=blog-api
```

Start:

```bash
./mvnw quarkus:dev -Dquarkus.profile=keycloak
```

---

# Access-Token mit HTTPie abrufen

Die Tests wurden unter WSL Ubuntu durchgeführt.

Beispiel für `author1`:

```bash
python3 -m httpie --form POST \
  http://localhost:8180/realms/blog/protocol/openid-connect/token \
  client_id=blog-api \
  username=author1 \
  password=PASSWORD \
  grant_type=password
```

Eine erfolgreiche Authentifizierung liefert:

```text
200 OK
```

und unter anderem:

```json
{
  "access_token": "eyJ..."
}
```

Passwörter und Access-Tokens werden nicht im Repository gespeichert.

---

# Token überprüfen

Das Access-Token wurde auf `jwt.io` überprüft.

Für `author1` ist die Realm-Rolle unter `realm_access.roles` sichtbar.

Beispiel:

```json
{
  "realm_access": {
    "roles": [
      "author"
    ]
  }
}
```

Damit wurde verifiziert, dass die konfigurierte Benutzerrolle korrekt in das JWT übernommen wird.

---

# Manuell getestete Security-Fälle mit eigenem Keycloak

Folgende Szenarien wurden erfolgreich getestet:

| Test | Ergebnis |
|---|---|
| `GET /blogs` ohne Token | `200 OK` |
| `POST /blogs` ohne Token | `401 Unauthorized` |
| `POST /blogs` mit `reader1` | `403 Forbidden` |
| `POST /blogs` mit `author1` | `201 Created` |
| `author2` bearbeitet Blog von `author1` | `403 Forbidden` |
| `admin1` bearbeitet Blog von `author1` | `200 OK` |

Damit wurden sowohl Rollenprüfung als auch Ownership-Prüfung erfolgreich verifiziert.

---

# Variante 2 – Quarkus Dev Services for Keycloak

Zusätzlich wurde die Authentifizierung über **Quarkus Dev Services for Keycloak** erfolgreich getestet.

Für diese Variante wird Quarkus ohne das externe `keycloak`-Profil gestartet:

```bash
./mvnw quarkus:dev
```

Docker muss dafür laufen.

Dev Services startet automatisch einen Keycloak-Container für die Entwicklungsumgebung.

Für die Testumgebung sind Benutzer und Rollen in `application.properties` definiert.

Beispiel:

```properties
%dev.quarkus.keycloak.devservices.enabled=true

%dev.quarkus.keycloak.devservices.users.reader1=reader1
%dev.quarkus.keycloak.devservices.roles.reader1=reader

%dev.quarkus.keycloak.devservices.users.author1=author1
%dev.quarkus.keycloak.devservices.roles.author1=author

%dev.quarkus.keycloak.devservices.users.author2=author2
%dev.quarkus.keycloak.devservices.roles.author2=author

%dev.quarkus.keycloak.devservices.users.admin1=admin1
%dev.quarkus.keycloak.devservices.roles.admin1=admin
```

Die Dev-Services-Variante wurde erfolgreich gestartet und die geschützten REST-Endpunkte wurden damit getestet.

Damit funktionieren beide geforderten Varianten:

```text
./mvnw quarkus:dev
→ Quarkus Dev Services for Keycloak

./mvnw quarkus:dev -Dquarkus.profile=keycloak
→ eigener Keycloak unter localhost:8180
```

---

# Swagger UI

Swagger UI ist erreichbar unter:

```text
http://localhost:8080/q/swagger-ui
```

OpenAPI:

```text
http://localhost:8080/q/openapi
```

Die geschützten Methoden wurden mit Swagger UI sowie mit HTTPie bzw. Bearer-Tokens getestet.

---

# Automatisierte Tests

Die REST- und Security-Funktionalität wird mit folgenden Technologien getestet:

- JUnit
- `@QuarkusTest`
- REST Assured
- `@TestSecurity`

Tests starten:

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

## Abgedeckte REST-Tests

Unter anderem werden geprüft:

- erfolgreicher POST
- Bean Validation
- ungültige Daten → `400 Bad Request`
- REST-Responses
- erwartete HTTP-Statuscodes

Da `POST /blogs` geschützt ist, werden diese Tests mit einem simulierten Benutzer ausgeführt:

```java
@TestSecurity(user = "author1", roles = {"author"})
```

---

## Abgedeckte Security-Tests

Automatisch geprüft werden:

- öffentlicher GET ohne Login
- POST ohne Login
- `reader` darf keinen Blog erstellen
- `author` darf Blog erstellen
- `admin` darf Blog erstellen
- `author` darf eigenen Blog bearbeiten
- `author` darf fremden Blog nicht bearbeiten
- `admin` darf fremden Blog bearbeiten
- `author` darf keinen Blog löschen
- `admin` darf Blog löschen

Beispielhafte Erwartungen:

```text
GET ohne Login         -> 200
POST ohne Login        -> 401
reader POST            -> 403
author POST            -> 201
author eigener Blog    -> 200
author fremder Blog    -> 403
admin fremder Blog     -> 200
author DELETE          -> 403
admin DELETE           -> 204
```

Damit werden sowohl Rollen als auch Ownership kontinuierlich getestet.

---

# CORS

CORS ist in

```text
src/main/resources/application.properties
```

konfiguriert.

Beispiel:

```properties
quarkus.http.cors.enabled=true
quarkus.http.cors.origins=http://localhost:3000
quarkus.http.cors.methods=GET,POST,PUT,DELETE
quarkus.http.cors.headers=Content-Type,Authorization
```

Der `Authorization`-Header ist explizit erlaubt, damit Bearer-Tokens von einem Frontend an die REST-API gesendet werden können.

---

# API Security / OWASP

## Broken Object Level Authorization

Die API schützt objektbezogene Zugriffe zusätzlich über eine Ownership-Prüfung.

Ein `author` darf nur eigene Blogs bearbeiten.

Ein `admin` darf alle Blogs bearbeiten.

**Status:** Teilweise bis weitgehend abgedeckt.

## Broken Authentication

Die Authentifizierung erfolgt über Keycloak und OpenID Connect.

Geschützte Endpunkte benötigen ein gültiges Access-Token.

**Status:** Grundlegende Authentifizierung umgesetzt.

## Broken Object Property Level Authorization

Request-DTOs definieren, welche Eigenschaften gesetzt werden dürfen.

Nicht frei setzbar sind unter anderem:

- `id`
- `owner`
- `comments`

**Status:** Teilweise durch DTOs abgedeckt.

## Unrestricted Resource Consumption

Aktuell fehlen noch:

- Rate Limiting
- Pagination
- Request-Limits

**Status:** Noch nicht vollständig abgedeckt.

## Security Misconfiguration

CORS ist eingeschränkt konfiguriert.

Zusätzlich werden keine Passwörter oder Access-Tokens im Repository gespeichert.

**Status:** Teilweise abgedeckt.

---

# Projekt starten

## Dev Services

```bash
./mvnw quarkus:dev
```

## Eigener Keycloak

```bash
docker start keycloak-mysql keycloak
```

Danach:

```bash
./mvnw quarkus:dev -Dquarkus.profile=keycloak
```

---

# Wichtige URLs

API:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/q/swagger-ui
```

OpenAPI:

```text
http://localhost:8080/q/openapi
```

Eigener Keycloak:

```text
http://localhost:8180
```

---

# Noch offene Erweiterungen

Mögliche zukünftige Erweiterungen:

- Pagination
- Rate Limiting
- DTOs für weitere Endpoints
- REST-Endpunkte für Kommentare
- produktive Secret-Verwaltung
- automatisierter Realm-Import
- weitere Security-Grenzfälle
- zentrale Exception-Mapper

---

# Security-Zusammenfassung

Die Blog-API verwendet mehrere Sicherheitsebenen:

```text
Access-Token
     ↓
Keycloak / OIDC
     ↓
Authentifizierung
     ↓
Realm-Rollen
     ↓
@RolesAllowed
     ↓
SecurityIdentity
     ↓
Ownership-Prüfung
     ↓
Geschäftslogik
```

Damit wird geprüft:

- ob ein Benutzer angemeldet ist
- welche Rolle er besitzt
- welcher Benutzer konkret angemeldet ist
- ob ihm der Blog gehört
- ob ein Administrator unabhängig vom Besitzer zugreifen darf

Beide Keycloak-Varianten wurden erfolgreich getestet und die Regeln sind zusätzlich durch 14 automatisierte Tests abgesichert.