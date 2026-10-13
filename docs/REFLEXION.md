# Reflexion

> Vorlage – bitte an das anpassen, was bei dir wirklich passiert ist.

| Problem | Lösung |
|---|---|
| XML-Listen sahen aus wie `<List><item>…</item></List>` | Eigene Listen-Klassen (`StationList`, `MeasurementList`, `AlertList`) mit `@JacksonXmlRootElement` und `@JacksonXmlElementWrapper(useWrapping = false)` |
| Das Feld `isActive` hieß im JSON `active` | `@JsonProperty("isActive")` am Feld |
| Bei `Accept: text/csv` kam keine Fehlermeldung | Im Handler für 406 die Antwort fix als JSON schreiben |
| 401 kam nicht im gleichen Fehlerformat | Eigener `authenticationEntryPoint` in der `SecurityConfig` |
| Der Browser zeigte bei falschem Passwort ein eigenes Login-Fenster | Den Header `WWW-Authenticate` bei der 401-Antwort weglassen |
| Tests hätten zufällig fehlschlagen können | Simulation in den Tests ausschalten, Messungen im Test selbst anlegen, fester Zufalls-Seed |
| Der Regen ging nie ganz auf 0 zurück | Wegen der Rundung blieb er bei 0,1 mm/h hängen. Lösung: unter 0,5 mm/h auf 0 setzen |
| Gleichzeitiger Zugriff von Simulation und Anfragen | Methoden im Repository `synchronized`, Listen als Kopie zurückgeben |
