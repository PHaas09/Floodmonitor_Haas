# Fragestellungen (Punkt 12)

## 1. Was sind die Hauptmerkmale von JSON?

- **Textformat zum Datenaustausch**, leichtgewichtig, für Menschen lesbar und für Maschinen leicht zu parsen [1][2].
- Basiert auf zwei Strukturen: **Objekte** (ungeordnete Name/Wert-Paare in `{ }`) und **Arrays** (geordnete Listen in `[ ]`) [1].
- Wenige Datentypen: String, Zahl, `true`/`false`, `null`, Objekt, Array [2]. Keine Datumswerte → Zeitpunkte werden als ISO-8601-String übertragen (z. B. `"2026-05-01T10:15:00Z"`).
- Sprachunabhängig, Standard-Kodierung UTF-8, Medientyp `application/json` [2].
- Keine Kommentare, keine Attribute, keine eingebaute Schema-Prüfung (dafür gibt es z. B. JSON Schema).
- Kompakter als XML → weniger Datenvolumen; in JavaScript direkt nutzbar (`JSON.parse`).

## 2. Was sind die Hauptmerkmale von XML?

- **Auszeichnungssprache** für hierarchisch strukturierte Daten in Textform, W3C-Standard [3].
- Daten stehen in **Elementen** mit Start- und End-Tag (`<waterLevel>318.4</waterLevel>`), zusätzlich gibt es **Attribute**.
- Dokument muss **wohlgeformt** sein: genau ein Wurzelelement, korrekt verschachtelte und geschlossene Tags, Groß-/Kleinschreibung zählt [3].
- Kann gegen ein Schema (DTD oder XML Schema/XSD) **validiert** werden; unterstützt **Namespaces**, Kommentare und eine Deklaration `<?xml version="1.0" encoding="UTF-8"?>`.
- Viele Werkzeuge: XPath, XSLT, DOM/SAX/StAX-Parser.
- Ausführlicher (mehr Overhead) als JSON, Medientyp `application/xml`.

## 3. Wie unterscheidet die Applikation, ob JSON oder XML zurückgegeben wird?

Über **Content Negotiation** mit dem HTTP-Header **`Accept`** [4][5]:

1. Durch die Abhängigkeit `jackson-dataformat-xml` registriert Spring Boot neben dem JSON-Konverter
   (`MappingJackson2HttpMessageConverter`) auch einen XML-Konverter (`MappingJackson2XmlHttpMessageConverter`).
2. Die Controller deklarieren `produces = {application/json, application/xml}`.
3. Spring liest den `Accept`-Header (`ContentNegotiationManager` / `HeaderContentNegotiationStrategy`) und vergleicht ihn mit `produces`:
   - `Accept: application/xml` → XML-Konverter schreibt dasselbe Java-Objekt als XML.
   - `Accept: application/json` oder **kein** Accept-Header → JSON (erster Eintrag in `produces`).
   - nicht unterstützter Typ (z. B. `text/csv`) → `HttpMediaTypeNotAcceptableException` → **406 Not Acceptable** (im `GlobalExceptionHandler`).
4. Die Antwort enthält den passenden `Content-Type`-Header. Der Webclient setzt den `Accept`-Header je nach Auswahl im Dropdown.

## 4. Was zeichnet ein REST-Interface aus und warum wird es so oft als standardisierte Schnittstelle verwendet?

REST (Representational State Transfer) ist ein Architekturstil von Roy Fielding [6]. Merkmale:

- **Ressourcen** werden über eindeutige **URIs** adressiert (`/api/v1/stations/ST-001`); URLs enthalten Nomen, keine Verben.
- **Einheitliche Schnittstelle:** Standard-HTTP-Methoden (GET, POST, PUT, PATCH, DELETE) mit fester Bedeutung und **Statuscodes** (200, 201, 404, …) [7].
- **Repräsentationen:** dieselbe Ressource kann in verschiedenen Formaten (JSON, XML) geliefert werden.
- **Zustandslos (stateless):** jede Anfrage enthält alle nötigen Infos (z. B. den Authorization-Header); der Server speichert keine Sitzung.
- **Client-Server-Trennung**, **Cachebarkeit** (z. B. von GET-Antworten), **mehrschichtiges System** (Proxies, Load Balancer) [6].

Warum so verbreitet: baut auf **HTTP** auf, das jede Plattform/Programmiersprache spricht; einfach mit Browser, curl oder Postman testbar; leichtgewichtig im Vergleich zu SOAP; gut skalierbar wegen Zustandslosigkeit; lose Kopplung zwischen Client und Server; viele Werkzeuge (OpenAPI/Swagger) für Doku und Code-Generierung.

## 5. Unterschied zwischen GET, POST, PUT und PATCH

| Methode | Zweck | sicher | idempotent | Beispiel |
|---|---|---|---|---|
| **GET** | Ressource lesen, ändert nichts | ja | ja | `GET /api/v1/stations/ST-001` (alle Endpunkte dieses Projekts) |
| **POST** | neue Ressource anlegen / Verarbeitung anstoßen | nein | nein | `POST /api/v1/stations` → `201 Created` + `Location`-Header |
| **PUT** | Ressource **vollständig** ersetzen (oder unter bekannter URI anlegen) | nein | ja | `PUT /api/v1/stations/ST-001` mit allen Feldern |
| **PATCH** | Ressource **teilweise** ändern | nein | nicht garantiert | `PATCH /api/v1/stations/ST-001` mit `{"isActive": false}` |

*Sicher* = keine Änderung am Server; *idempotent* = mehrfaches Senden hat dieselbe Wirkung wie einmaliges Senden [7][8][9].

## 6. Ich will eine neue Request-Methode / einen neuen Pfad anbieten – welche Stellen muss ich ändern?

1. **Controller** (`FloodMonitorController`): neue Methode mit `@GetMapping`, `@PostMapping` usw. und dem Pfad; Parameter mit `@PathVariable`, `@RequestParam` oder `@RequestBody`.
2. **Service**: die Logik dazu – der Controller ruft nur den Service auf.
3. **Repository**, falls neue Daten gelesen oder gespeichert werden.
4. **Model/DTO**: neue Antwort- oder Request-Klasse (für XML mit `@JacksonXmlRootElement`).
5. **Fehlerbehandlung**: falls nötig neue Exception und Handler im `GlobalExceptionHandler`.
6. **Sicherheit**: in `SecurityConfig` prüfen, ob der Pfad geschützt sein muss.
7. **Dokumentation**: `@Operation` für die OpenAPI-Doku, README.
8. **Tests** und gegebenenfalls der **Webclient** (`app.js`).

## 7. Kann man OpenAPI und Swagger vergleichen?

Nur eingeschränkt – es sind **nicht zwei gleichartige Dinge**, sondern Spezifikation vs. Werkzeuge [10][11][12]:

- **OpenAPI** ist die **Spezifikation** (Standard), wie eine REST-API in einer YAML/JSON-Datei beschrieben wird: Endpunkte, Methoden, Parameter, Request/Response-Bodies, Statuscodes, Authentifizierung. Sie wird von der OpenAPI Initiative (Linux Foundation) weiterentwickelt [11].
- **Swagger** war ursprünglich der Name dieser Spezifikation (bis Version 2.0). 2015 wurde sie an die OpenAPI Initiative übergeben und in „OpenAPI Specification“ umbenannt (ab Version 3.0) [10].
- Heute bezeichnet **Swagger** die **Werkzeuge** von SmartBear rund um OpenAPI: Swagger UI (interaktive Doku), Swagger Editor, Swagger Codegen [10].
- Zusammenhang wie im IBM-Video [12]: Die OpenAPI-Definition (z. B. `icecream.yaml`) beschreibt die API; Tools wie Swagger UI erzeugen daraus interaktive Dokumentation, SDKs oder Validierungen.
- In diesem Projekt: `springdoc-openapi` erzeugt aus dem Controller die **OpenAPI**-Beschreibung (`/v3/api-docs`), **Swagger UI** (`/swagger-ui.html`) stellt sie dar.

## 8. Wo liegt im Code-Snippet der Fehler?

```java
@GetMapping("/stations/add")        // erste Zeile in der Angabe nur schwach lesbar
public String createStation(@RequestParam String name, @RequestParam double warning) {
    stationService.save(new Station(name, warning));
    return "Station successfully created!";
}
```

Fehler / Probleme:

1. **Falsche HTTP-Methode:** Eine Ressource wird mit **GET** angelegt. GET muss *sicher* und *idempotent* sein [7] – Browser, Crawler, Caches oder ein Prefetch dürfen GET jederzeit (mehrfach) ausführen und würden dabei Stationen anlegen. Richtig: **`@PostMapping`**.
2. **Verb in der URL:** `/stations/add` ist nicht RESTful. Die Aktion steckt in der Methode, der Pfad ist die Collection: `POST /api/v1/stations`.
3. **Daten als Query-Parameter** statt als **Request-Body** (`@RequestBody` mit JSON/XML) – unvollständige Daten (keine ID, kein Fluss, kein kritischer Wert), keine Prüfung, Daten landen in URLs und Logs.
4. **Falsche Antwort:** Rückgabe eines festen Strings mit **200 OK** statt **201 Created** + `Location`-Header + angelegter Ressource (JSON/XML). Kein 409 bei bereits vorhandener ID, keine Content Negotiation.

So wäre es richtig:

```java
@PostMapping(value = "/api/v1/stations", consumes = {"application/json", "application/xml"})
public ResponseEntity<Station> createStation(@RequestBody Station station) {
    Station created = stationService.save(station);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(created.getId()).toUri();
    return ResponseEntity.created(location).body(created);
}
```

---

## Quellen

1. JSON.org – Einführung in JSON: https://www.json.org/json-de.html
2. RFC 8259 – The JavaScript Object Notation (JSON) Data Interchange Format: https://www.rfc-editor.org/rfc/rfc8259
3. W3C – Extensible Markup Language (XML) 1.0: https://www.w3.org/TR/xml/
4. MDN – Content negotiation: https://developer.mozilla.org/en-US/docs/Web/HTTP/Content_negotiation
5. Spring Framework Reference – Content Types (Content Negotiation): https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/content-negotiation.html
6. R. Fielding – Architectural Styles … Chapter 5: REST: https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm
7. RFC 9110 – HTTP Semantics (Kap. 9 Methods): https://www.rfc-editor.org/rfc/rfc9110#name-methods
8. RFC 5789 – PATCH Method for HTTP: https://www.rfc-editor.org/rfc/rfc5789
9. MDN – HTTP request methods: https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods
10. Swagger – What Is the Difference Between Swagger and OpenAPI?: https://swagger.io/difference-between-swagger-and-openapi
11. OpenAPI Specification (OpenAPI Initiative): https://spec.openapis.org/oas/latest.html
12. IBM Technology – REST API & OpenAPI (Video aus der Angabe): https://www.youtube.com/watch?v=pRS9LRBgjYg
13. Spring Guides – Building a RESTful Web Service: https://spring.io/guides/gs/rest-service
