# Distributed Systems – Blog API

Dieses Projekt implementiert eine REST-API zur Verwaltung von Blog-Beiträgen mit **Quarkus**.

Die REST-Schnittstelle basiert auf **Jakarta REST**. Die Daten werden im JSON-Format übertragen und mit Hibernate ORM / Panache persistiert.

---

# REST-Design

Die API stellt Funktionen zum Erstellen, Lesen, Aktualisieren, Löschen und Suchen von Blog-Beiträgen zur Verfügung.

## Endpoints

| HTTP-Methode | Pfad | Beschreibung |
|---|---|---|
| GET | `/blogs` | Gibt alle Blog-Beiträge zurück |
| GET | `/blogs?title={title}` | Sucht Blog-Beiträge anhand des Titels |
| GET | `/blogs/{id}` | Gibt einen bestimmten Blog-Beitrag zurück |
| POST | `/blogs` | Erstellt einen neuen Blog-Beitrag |
| PUT | `/blogs/{id}` | Aktualisiert einen bestehenden Blog-Beitrag |
| DELETE | `/blogs/{id}` | Löscht einen bestehenden Blog-Beitrag |

---

## GET /blogs

Gibt alle vorhandenen Blog-Beiträge zurück.

Beispiel:

```text
GET /blogs
```

Erwarteter Statuscode:

```text
200 OK
```

Zusätzlich kann mit dem Query-Parameter `title` nach einem Blog gesucht werden.

Beispiel:

```text
GET /blogs?title=quarkus
```

Es werden alle Blogs zurückgegeben, deren Titel den angegebenen Text enthält.

---

## GET /blogs/{id}

Liefert einen einzelnen Blog anhand seiner ID.

Beispiel:

```text
GET /blogs/1
```

Falls der Blog existiert:

```text
200 OK
```

Falls kein Blog mit dieser ID existiert:

```text
404 Not Found
```

Beispiel einer Fehlerantwort:

```json
{
  "error": "Blog nicht gefunden",
  "message": "Es existiert kein Blog mit der ID 1"
}
```

---

## POST /blogs

Erstellt einen neuen Blog-Beitrag.

Beispiel:

```text
POST /blogs
```

Request Body:

```json
{
  "title": "Quarkus REST",
  "content": "Mein erster REST-Beitrag"
}
```

Bei erfolgreicher Erstellung:

```text
201 Created
```

Titel und Inhalt werden mit Jakarta Bean Validation geprüft und dürfen nicht leer sein.

---

## PUT /blogs/{id}

Aktualisiert einen bestehenden Blog-Beitrag.

Beispiel:

```text
PUT /blogs/1
```

Request Body:

```json
{
  "title": "Neuer Titel",
  "content": "Aktualisierter Inhalt"
}
```

Falls der Blog existiert:

```text
200 OK
```

Falls kein Blog mit der angegebenen ID existiert:

```text
404 Not Found
```

Auch bei einer Aktualisierung werden Titel und Inhalt mit Bean Validation geprüft.

---

## DELETE /blogs/{id}

Löscht einen bestehenden Blog-Beitrag.

Beispiel:

```text
DELETE /blogs/1
```

Bei erfolgreichem Löschen:

```text
204 No Content
```

Falls kein entsprechender Blog existiert:

```text
404 Not Found
```

---

# Validierung

Für die Eingabevalidierung wird **Jakarta Bean Validation** verwendet.

Zurzeit gelten folgende Regeln:

- `title` darf nicht leer sein.
- `content` darf nicht leer sein.

Dafür wird `@NotBlank` verwendet.

Ein ungültiger Request wie

```json
{
  "title": "",
  "content": ""
}
```

wird mit

```text
400 Bad Request
```

abgelehnt.

---

# DTOs und Mapping

Für den Endpoint `POST /blogs` werden eigene Request- und Response-DTOs verwendet.

Dadurch wird die interne Datenbank-Entity nicht direkt als Vertrag der REST-API verwendet.

## CreateBlogRequest

`CreateBlogRequest` definiert, welche Daten ein Client beim Erstellen eines Blog-Beitrags senden darf.

Freigegebene Felder:

- `title`
- `content`

Nicht direkt freigegeben sind beispielsweise:

- `id`
- `comments`

Die `id` wird von der Datenbank erzeugt und kann deshalb nicht durch den Client gesetzt werden.

Beispiel:

```json
{
  "title": "Quarkus REST",
  "content": "Mein Blog-Beitrag"
}
```

## BlogResponse

`BlogResponse` definiert die Daten, die nach dem Erstellen eines Blogs an den Client zurückgegeben werden.

Freigegebene Felder:

- `id`
- `title`
- `content`

Dadurch kann gezielt gesteuert werden, welche Informationen über die REST-API nach außen gelangen.

## Mapping

Beim Erstellen eines Blogs erfolgt das Mapping in mehreren Schritten:

```text
CreateBlogRequest
        ↓
      Blog
        ↓
   Datenbank
        ↓
      Blog
        ↓
  BlogResponse
```

Der Request-DTO wird zuerst in eine `Blog`-Entity umgewandelt:

```java
Blog blog = new Blog(request.title, request.content);
blogService.addBlog(blog);
```

Danach wird aus der Entity ein `BlogResponse` erzeugt:

```java
BlogResponse response = new BlogResponse(
    blog.getId(),
    blog.getTitle(),
    blog.getContent()
);
```

Der Client erhält dadurch nur die vorgesehenen Daten.

---

# Automatisierte Tests

Die REST-API wird mit **REST Assured** und `@QuarkusTest` automatisiert getestet.

Die Tests befinden sich unter:

```text
src/test/java/ch/hftm/
```

Getestet werden unter anderem:

- erfolgreiches Erstellen eines Blog-Beitrags
- Validierungsfehler bei ungültigen Eingaben
- erwartete HTTP-Statuscodes
- REST-Responses

Die Tests können mit folgendem Befehl ausgeführt werden:

```bash
./mvnw test
```

Ein gültiger POST-Request muss beispielsweise `201 Created` zurückgeben.

Ein Request mit leerem Titel oder Inhalt muss dagegen mit `400 Bad Request` abgelehnt werden.

---

# CORS

CORS wird verwendet, um festzulegen, welche Frontends von einer anderen Origin auf die REST-API zugreifen dürfen.

Die Konfiguration befindet sich in:

```text
src/main/resources/application.properties
```

Beispiel:

```properties
quarkus.http.cors.enabled=true
quarkus.http.cors.origins=http://localhost:3000
quarkus.http.cors.methods=GET,POST,PUT,DELETE
quarkus.http.cors.headers=Content-Type,Authorization
```

Damit darf das konfigurierte Frontend auf die API zugreifen.

Für eine produktive Umgebung muss die Origin an die tatsächliche Frontend-Adresse angepasst werden.

Eine unnötig offene CORS-Konfiguration sollte vermieden werden.

---

# API Security

Für die Blog-API sind verschiedene Risiken aus der OWASP API Security Top 10 relevant.

## Broken Object Level Authorization

Die API verwendet IDs in Endpoints wie:

```text
GET /blogs/{id}
PUT /blogs/{id}
DELETE /blogs/{id}
```

Ohne Autorisierung könnte ein Benutzer auf fremde Blog-Einträge zugreifen oder diese verändern.

**Status:** Noch nicht vollständig abgedeckt.

Eine Zugriffskontrolle über Benutzerrollen ist vorgesehen.

---

## Broken Authentication

Für schreibende Zugriffe wird eine sichere Authentifizierung benötigt.

Für die Authentifizierung wird Keycloak verwendet.

**Status:** In Vorbereitung.

---

## Broken Object Property Level Authorization

Wenn Entities direkt als Request-Objekte verwendet werden, könnten Eigenschaften übergeben werden, die ein Client nicht verändern können sollte.

Durch `CreateBlogRequest` wird festgelegt, welche Eigenschaften beim Erstellen eines Blogs akzeptiert werden:

- `title`
- `content`

Felder wie `id` oder `comments` können nicht direkt gesetzt werden.

**Status:** Teilweise durch DTOs abgedeckt.

---

## Unrestricted Resource Consumption

Ein Client könnte sehr viele Requests senden oder sehr große Datenmengen abrufen.

Aktuell existieren noch:

- kein Rate Limiting
- keine Pagination
- keine Begrenzung der Anzahl Requests

**Status:** Noch nicht abgedeckt.

---

## Security Misconfiguration

Fehlerhafte Konfigurationen können dazu führen, dass eine API unnötig offen ist.

Ein Beispiel dafür ist eine zu offene CORS-Konfiguration.

CORS wird deshalb auf die benötigte Origin und die benötigten HTTP-Methoden eingeschränkt.

**Status:** Teilweise abgedeckt.

---

# Berechtigungskonzept

Für die Blog-API wird ein rollenbasiertes Berechtigungskonzept verwendet.

## Öffentliche Endpunkte

Das Lesen von Blog-Beiträgen soll ohne Login möglich sein.

| Methode | Endpoint | Zugriff |
|---|---|---|
| GET | `/blogs` | öffentlich |
| GET | `/blogs/{id}` | öffentlich |

---

## Benutzerrollen

Für die Blog-API werden folgende Rollen verwendet:

### reader

Ein `reader` kann Blog-Beiträge lesen.

Da die GET-Endpunkte öffentlich erreichbar sind, ist für das reine Lesen aktuell kein Login notwendig.

### author

Ein `author` darf:

- Blog-Beiträge lesen
- neue Blog-Beiträge erstellen
- bestehende Blog-Beiträge aktualisieren

### admin

Ein `admin` darf:

- Blog-Beiträge lesen
- Blog-Beiträge erstellen
- Blog-Beiträge aktualisieren
- Blog-Beiträge löschen

---

## Zugriffsregeln

| Methode | Endpoint | Berechtigung |
|---|---|---|
| GET | `/blogs` | öffentlich |
| GET | `/blogs/{id}` | öffentlich |
| POST | `/blogs` | `author`, `admin` |
| PUT | `/blogs/{id}` | `author`, `admin` |
| DELETE | `/blogs/{id}` | `admin` |

Die Regeln sollen in der nächsten Einheit mit Quarkus OIDC und `@RolesAllowed` umgesetzt werden.

Beispiel:

```java
@RolesAllowed({"author", "admin"})
@POST
public Response addBlog(...) {
    // ...
}
```

Für das Löschen ist ausschließlich die Rolle `admin` vorgesehen:

```java
@RolesAllowed("admin")
@DELETE
@Path("/{id}")
public Response deleteBlog(...) {
    // ...
}
```

---

# Keycloak

Für Authentifizierung und Rollenverwaltung wird lokal Keycloak verwendet.

## Docker-Setup

Das lokale Setup besteht aus zwei Docker-Containern:

| Container | Image | Aufgabe |
|---|---|---|
| `keycloak` | `quay.io/keycloak/keycloak:22.0.1` | Identity Provider / Authentifizierung |
| `keycloak-mysql` | `mysql:8.0` | Persistente Datenbank für Keycloak |

Die laufenden Container können mit folgendem Befehl geprüft werden:

```bash
docker ps
```

Keycloak ist lokal über folgende Adresse erreichbar:

```text
http://localhost:8180
```

Die Keycloak Admin Console ist ebenfalls über diese Adresse erreichbar.

## Container starten

Falls die Container bereits erstellt wurden, können sie mit folgendem Befehl gestartet werden:

```bash
docker start keycloak-mysql keycloak
```

Der Status kann anschließend mit folgendem Befehl geprüft werden:

```bash
docker ps
```

---

## Keycloak-Konfiguration

Für die Blog-API wird der Realm

```text
blog
```

verwendet.

### Client

Der Client heißt:

```text
blog-api
```

Für den Token-Abruf mit Benutzername und Passwort muss beim Client **Direct Access Grants** aktiviert sein.

### Realm-Rollen

Im Realm `blog` sind folgende Rollen eingerichtet:

- `reader`
- `author`
- `admin`

### Test-Benutzer

Für lokale Tests werden folgende Benutzer verwendet:

| Benutzer | Rolle |
|---|---|
| `reader1` | `reader` |
| `author1` | `author` |
| `admin1` | `admin` |

Für die Test-Benutzer werden nicht-temporäre Passwörter gesetzt.

Anschließend wird den Benutzern über **Role Mapping** die passende Realm-Rolle zugewiesen.

### Manuelle Konfiguration

Falls die Keycloak-Konfiguration neu erstellt werden muss, sind folgende Schritte notwendig:

1. Realm `blog` erstellen.
2. Realm-Rollen `reader`, `author` und `admin` erstellen.
3. Client `blog-api` erstellen.
4. Beim Client `Direct Access Grants` aktivieren.
5. Benutzer `reader1`, `author1` und `admin1` erstellen.
6. Jedem Benutzer ein nicht-temporäres Passwort vergeben.
7. Jedem Benutzer über **Role Mapping** die passende Realm-Rolle zuweisen.

---

# Access-Token mit HTTPie abrufen

Für den Test wurde der Benutzer `author1` verwendet.

Der Token wird über den OpenID-Connect-Token-Endpoint des Realms `blog` abgerufen.

Beispiel:

```powershell
python -m httpie --form POST http://localhost:8180/realms/blog/protocol/openid-connect/token client_id=blog-api username=author1 password=PASSWORD grant_type=password
```

Das tatsächliche Passwort wird nicht im Repository gespeichert.

Bei erfolgreicher Authentifizierung antwortet Keycloak mit:

```text
200 OK
```

und liefert unter anderem ein Access-Token:

```json
{
  "access_token": "eyJ..."
}
```

---

# Token überprüfen

Das erhaltene Access-Token wurde auf `jwt.io` überprüft.

Beim Benutzer `author1` ist die zugewiesene Realm-Rolle im Payload unter `realm_access.roles` enthalten.

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

Damit wurde verifiziert, dass die in Keycloak konfigurierte Rolle korrekt in das Access-Token übernommen wird.

---

# OpenAPI und Swagger UI

Die REST-API kann während der Entwicklung über Swagger UI getestet werden.

Swagger UI:

```text
http://localhost:8080/q/swagger-ui
```

OpenAPI-Definition:

```text
http://localhost:8080/q/openapi
```

Über Swagger UI können die vorhandenen GET-, POST-, PUT- und DELETE-Endpunkte direkt getestet werden.

---

# Projekt starten

Die Quarkus-Anwendung kann im Development Mode gestartet werden:

```bash
./mvnw quarkus:dev
```

Anschließend ist die REST-API standardmäßig unter

```text
http://localhost:8080
```

erreichbar.