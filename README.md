# FloodMonitor – Hochwasser-Frühwarnsystem

REST-Schnittstelle mit Spring Boot für die Messstationen eines Hochwasser-Frühwarnsystems.
Die Messwerte werden simuliert, die Warnstufe wird am Server berechnet und alle Daten
gibt es als **JSON** oder **XML**. Ein Webclient mit jQuery zeigt die Daten an.

## Starten

Voraussetzung: JDK 17 oder 21.

```bash
gradlew.bat bootRun        # Windows
./gradlew bootRun          # Linux / macOS
```

| Was | Adresse |
|---|---|
| Webclient | http://localhost:8080/ |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (YAML) | http://localhost:8080/v3/api-docs.yaml (Kopie in `docs/openapi.yaml`) |

Login für die API: Benutzer `viewer`, Passwort `viewer123`.
Das Passwort ist nur ein Entwicklungs-Standard. Mit der Umgebungsvariable
`FLOODMONITOR_PASSWORD` wird ein eigenes Passwort gesetzt.

Tests: `gradlew.bat test`

## Architektur

| Paket | Inhalt |
|---|---|
| `model` | `Station`, `Location`, `Measurement`, Enums `WarningLevel` und `StationStatus` |
| `dto` | Antwortobjekte: Listen, `Alert`, `Statistics` |
| `repository` | Speichert Stationen und Messungen im Arbeitsspeicher |
| `service` | Logik: Stationen, Messwerte, Warnstufe, Simulation, Prüfung der Parameter |
| `controller` | `FloodMonitorController` – nur HTTP, keine Logik |
| `exception` | `ApiError` und `GlobalExceptionHandler` (einheitliche Fehler) |
| `config` | Startdaten, Login, XML, OpenAPI |
| `resources/static` | Webclient (`index.html`, `style.css`, `app.js`) |

Ablauf einer Anfrage: **Controller → Service → Repository**. Fehler werden als Exception
geworfen und im `GlobalExceptionHandler` zu einer einheitlichen Fehlerantwort.

## Datenmodell

- **Station**: `id`, `name`, `river`, `location` (`latitude`, `longitude`), `normalWaterLevel`,
  `warningWaterLevel`, `criticalWaterLevel` (cm), `meanFlowRate` (m³/s), `isActive`, `status`,
  `currentWarningLevel`
- **Measurement**: `stationId`, `timestamp` (ISO-8601, `java.time.Instant`), `waterLevel` (cm),
  `flowRate` (m³/s), `rainfall` (mm/h), `temperature` (°C), `batteryLevel` (%), `status`, `warningLevel`
- **WarningLevel**: `NORMAL`, `WARNING`, `CRITICAL`, `UNKNOWN`
- **StationStatus**: `ONLINE`, `MAINTENANCE`, `OFFLINE`

| ID | Name | Fluss | Normal / Warnung / Kritisch (cm) | aktiv |
|---|---|---|---|---|
| ST-001 | Donaubrücke Linz | Donau | 350 / 550 / 700 | ja |
| ST-002 | Reichsbrücke Wien | Donau | 300 / 500 / 650 | ja |
| ST-003 | Staatsbrücke Salzburg | Salzach | 250 / 290 / 340 | ja |
| ST-004 | Innbrücke Innsbruck | Inn | 220 / 260 / 310 | ja |
| ST-005 | Hauptbrücke Graz | Mur | 200 / 300 / 380 | nein |

## Simulationsregeln

Alle `simulation.interval` ms (Standard 10000) bekommt jede aktive Station eine neue Messung.
Jeder neue Wert baut auf dem vorherigen auf:

1. **Regen** geht pro Messung auf 70 % zurück, unter 0,5 mm/h hört er auf. Mit 2 % Wahrscheinlichkeit
   beginnt Starkregen (+15 bis +40 mm/h).
2. **Wasserstand**: `neu = alt + Regen · 0,5 − (alt − Normalwert) · 0,05 + Rauschen`.
   Regen hebt den Pegel, ohne Regen sinkt er zum Normalwert. Höchstens ±15 cm pro Messung, nie negativ.
3. **Durchfluss**: `meanFlowRate · (Wasserstand / Normalwert)^1,6` – mehr Wasser, mehr Durchfluss.
4. **Temperatur** ändert sich nur leicht.
5. **Akku** sinkt langsam. Bei 10 % oder weniger → `MAINTENANCE`, dann wird er geladen, ab 80 % wieder `ONLINE`.
6. Deaktivierte oder `OFFLINE`-Stationen bekommen keine Messungen.

Beim Start werden Messwerte für die letzten 12 Stunden erzeugt (`simulation.history-hours`).
In den Tests ist die Simulation ausgeschaltet (`simulation.enabled=false`).

## Warnstufe

| Bedingung | Warnstufe |
|---|---|
| Wasserstand kleiner als Warnwert | `NORMAL` |
| Wasserstand ab Warnwert | `WARNING` |
| Wasserstand ab kritischem Wert | `CRITICAL` |
| keine Messung, ungültiger Wert, letzte Messung älter als 5 Minuten oder Station inaktiv | `UNKNOWN` |

## Endpunkte

| Methode | Endpunkt | Parameter |
|---|---|---|
| GET | `/api/v1/stations` | `river`, `isActive`, `warningLevel` |
| GET | `/api/v1/stations/{stationId}` | – |
| GET | `/api/v1/stations/{stationId}/measurements/latest` | – |
| GET | `/api/v1/stations/{stationId}/measurements` | `from`, `to`, `limit` (1–1000, Standard 100), `warningLevel` |
| GET | `/api/v1/stations/{stationId}/statistics` | `from`, `to` |
| GET | `/api/v1/alerts` | `warningLevel`, `river` |

Die Statistik enthält niedrigsten, höchsten und durchschnittlichen Wasserstand, durchschnittliche
Durchflussmenge, Summe des Niederschlags, Anzahl der Messungen, Anzahl der Warnungen
(WARNING und CRITICAL) und Anzahl der kritischen Warnungen.

**Format:** Ohne `Accept`-Header kommt JSON, mit `Accept: application/xml` XML.
Bei einem anderen Format antwortet die API mit `406 Not Acceptable`.

**Statuscodes:** 200 OK, 400 ungültiger Parameter, 401 nicht angemeldet, 404 nicht gefunden,
405 falsche Methode, 406 Format nicht unterstützt, 500 interner Fehler.

Fehler kommen immer in diesem Format (als JSON oder XML, nie mit Stacktrace):

```json
{
  "timestamp": "2026-05-01T10:20:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Station ST-999 wurde nicht gefunden",
  "path": "/api/v1/stations/ST-999"
}
```

## Beispiele

Weitere Beispiele stehen in `requests.http` (in IntelliJ direkt ausführbar).

```bash
curl -u viewer:viewer123 http://localhost:8080/api/v1/stations/ST-001/measurements/latest
```
```json
{"stationId":"ST-001","timestamp":"2026-10-13T06:49:21Z","waterLevel":352.3,"flowRate":1515.8,"rainfall":0.0,"temperature":16.2,"batteryLevel":85,"status":"ONLINE","warningLevel":"NORMAL"}
```

```bash
curl -u viewer:viewer123 -H "Accept: application/xml" http://localhost:8080/api/v1/stations/ST-001/measurements/latest
```
```xml
<?xml version='1.0' encoding='UTF-8'?><measurement><stationId>ST-001</stationId><timestamp>2026-10-13T06:49:21Z</timestamp><waterLevel>352.3</waterLevel><flowRate>1515.8</flowRate><rainfall>0.0</rainfall><temperature>16.2</temperature><batteryLevel>85</batteryLevel><status>ONLINE</status><warningLevel>NORMAL</warningLevel></measurement>
```

```bash
curl -u viewer:viewer123 "http://localhost:8080/api/v1/stations/ST-003/measurements?from=2026-10-13T08:00:00Z&to=2026-10-13T12:00:00Z&limit=100"
```

## Webclient

Anmelden, Format JSON oder XML wählen, Station und Filter (Zeitraum, Warnstufe, Limit) setzen.
Angezeigt werden der aktuelle Messwert, die aktuellen Warnungen und die historischen Messwerte als Tabelle.
Die Daten werden alle 10 Sekunden neu geladen. Bei XML wandelt `app.js` die Antwort in dieselben
JavaScript-Objekte um wie bei JSON. Farben: NORMAL grün, WARNING orange, CRITICAL rot, UNKNOWN grau.
Screenshots: `docs/screenshots/`.

## Tests

- `FloodMonitorApiTest`: die 12 Tests aus der Angabe (JSON, XML, Station, unbekannte Station,
  aktueller Messwert, Zeitraum, ungültiger Zeitraum, NORMAL, WARNING, CRITICAL, Format, Limit) und Login
- `WarningLevelCalculatorTest`: Grenzwerte der Warnstufen
- `SimulationTest`: Simulationsregeln (fester Zufalls-Seed)

## Weitere Dokumente

- `docs/FRAGEN.md` – Antworten auf die Fragestellungen
- `docs/REFLEXION.md` – Probleme und Lösungen
- `docs/KI-PROMPTS.md` – verwendete KI-Prompts
