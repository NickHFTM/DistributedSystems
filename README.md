# DistributedSystems
# REST-Design des Blog-Systems

Das Blog-System stellt eine REST-API zur Verwaltung von Blog-Beiträgen zur Verfügung.

Die Daten werden als JSON übertragen. Die REST-Schnittstelle basiert auf Jakarta REST und wird mit Quarkus REST implementiert.

## Endpoints

| HTTP-Methode | Pfad                   | Beschreibung                                |
| ------------ | ---------------------- | ------------------------------------------- |
| GET          | `/blogs`               | Gibt alle Blog-Beiträge zurück              |
| GET          | `/blogs?title={title}` | Sucht Blog-Beiträge anhand des Titels       |
| GET          | `/blogs/{id}`          | Gibt einen bestimmten Blog-Beitrag zurück   |
| POST         | `/blogs`               | Erstellt einen neuen Blog-Beitrag           |
| PUT          | `/blogs/{id}`          | Aktualisiert einen bestehenden Blog-Beitrag |
| DELETE       | `/blogs/{id}`          | Löscht einen bestehenden Blog-Beitrag       |

## GET /blogs

Gibt alle vorhandenen Blog-Beiträge zurück.

Beispiel:

`GET /blogs`

Erwarteter Statuscode:

`200 OK`

Zusätzlich kann mit dem Query-Parameter `title` nach einem Blog gesucht werden.

Beispiel:

`GET /blogs?title=quarkus`

Es werden alle Blogs zurückgegeben, deren Titel den angegebenen Text enthält.

## GET /blogs/{id}

Liefert einen einzelnen Blog anhand seiner ID.

Beispiel:

`GET /blogs/1`

Falls der Blog existiert:

`200 OK`

Falls kein Blog mit dieser ID existiert:

`404 Not Found`

Beispiel einer Fehlerantwort:

```json
{
  "error": "Blog nicht gefunden",
  "message": "Es existiert kein Blog mit der ID 1"
}
```

## POST /blogs

Erstellt einen neuen Blog-Beitrag.

Beispiel:

`POST /blogs`

Request Body:

```json
{
  "title": "Quarkus REST",
  "content": "Mein erster REST-Beitrag"
}
```

Bei erfolgreicher Erstellung wird folgender Statuscode zurückgegeben:

`201 Created`

Titel und Inhalt werden mit Bean Validation geprüft. Beide Felder dürfen nicht leer sein.

## PUT /blogs/{id}

Aktualisiert einen bereits vorhandenen Blog.

Beispiel:

`PUT /blogs/1`

Request Body:

```json
{
  "title": "Neuer Titel",
  "content": "Aktualisierter Inhalt"
}
```

Falls der Blog existiert:

`200 OK`

Falls kein Blog mit der angegebenen ID existiert:

`404 Not Found`

Auch bei einer Aktualisierung werden Titel und Inhalt mit Bean Validation geprüft.

## DELETE /blogs/{id}

Löscht einen bestehenden Blog-Beitrag.

Beispiel:

`DELETE /blogs/1`

Bei erfolgreichem Löschen:

`204 No Content`

Falls kein entsprechender Blog existiert:

`404 Not Found`

## Validierung

Für die Eingabevalidierung wird Jakarta Bean Validation verwendet.

Zurzeit gelten folgende Regeln:

* `title` darf nicht leer sein.
* `content` darf nicht leer sein.

Ein ungültiger Request wird mit dem HTTP-Statuscode `400 Bad Request` abgelehnt.

## Noch nicht umgesetzt

Folgende Funktionen sind vorerst nicht Bestandteil der REST-API:

* Separate DTOs für Requests und Responses
* Erweiterte Validierungsregeln
* REST-Endpunkte zur Verwaltung der Kommentare
* Pagination
* Erweiterte Sortierung und Filterung

Diese Funktionen können in späteren Erweiterungen ergänzt werden.
