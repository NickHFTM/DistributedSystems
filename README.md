# DistributedSystems

# REST-Design des Blog-Systems

Das Blog-System stellt eine REST-API zur Verwaltung von Blog-Beiträgen zur Verfügung.

Die Daten werden als JSON übertragen. Die REST-Schnittstelle basiert auf Jakarta REST und wird mit Quarkus REST implementiert.

## Endpoints

| HTTP-Methode | Pfad | Beschreibung |
| ------------ | ---- | ------------ |
| GET | `/blogs` | Gibt alle Blog-Beiträge zurück |
| GET | `/blogs?title={title}` | Sucht Blog-Beiträge anhand des Titels |
| GET | `/blogs/{id}` | Gibt einen bestimmten Blog-Beitrag zurück |
| POST | `/blogs` | Erstellt einen neuen Blog-Beitrag |
| PUT | `/blogs/{id}` | Aktualisiert einen bestehenden Blog-Beitrag |
| DELETE | `/blogs/{id}` | Löscht einen bestehenden Blog-Beitrag |

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
## DTOs und Mapping

Für den Endpoint `POST /blogs` werden eigene Request- und Response-DTOs verwendet.

### CreateBlogRequest

Der Request-DTO gibt nur folgende Felder frei:

- `title`
- `content`

Felder wie `id` oder `comments` können beim Erstellen eines Blogs nicht direkt vom Client gesetzt werden.

### BlogResponse

Der Response-DTO gibt folgende Felder zurück:

- `id`
- `title`
- `content`

### Mapping

Das Mapping erfolgt in der REST-Ressource.

Ablauf:

`CreateBlogRequest -> Blog -> BlogResponse`

Beispiel:

```java
Blog blog = new Blog(request.title, request.content);
blogService.addBlog(blog);

BlogResponse response = new BlogResponse(
    blog.getId(),
    blog.getTitle(),
    blog.getContent()
);