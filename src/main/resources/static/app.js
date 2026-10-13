$(function () {

    let auth = '';      // Base64 von "benutzer:passwort"
    let timer = null;

    function format() {
        return $('#format').val();     // "json" oder "xml"
    }

    function station() {
        return $('#station').val();
    }

    // GET-Anfrage an die API mit Accept-Header und Login
    function load(path, params) {
        return $.ajax({
            url: 'api/v1' + path,
            data: params,
            dataType: format(),
            headers: {
                'Accept': 'application/' + format(),
                'Authorization': 'Basic ' + auth
            }
        });
    }

    // Wandelt ein XML-Element in ein JavaScript-Objekt um (gleiche Struktur wie bei JSON)
    function xmlToObject(element) {
        const obj = {};
        $(element).children().each(function () {
            const text = $(this).text();
            if ($(this).children().length > 0) {
                obj[this.tagName] = xmlToObject(this);          // z.B. <location>
            } else if (text === 'true' || text === 'false') {
                obj[this.tagName] = (text === 'true');
            } else if (text !== '' && !isNaN(text)) {
                obj[this.tagName] = Number(text);
            } else {
                obj[this.tagName] = text;
            }
        });
        return obj;
    }

    // Liste aus der Antwort holen: JSON direkt, XML ueber die Elementnamen
    function toList(data, jsonKey, xmlTag) {
        if (format() === 'json') {
            return data[jsonKey];
        }
        return $(data).find(xmlTag).map(function () {
            return xmlToObject(this);
        }).get();
    }

    // Einzelnes Objekt aus der Antwort holen
    function toObject(data) {
        if (format() === 'json') {
            return data;
        }
        return xmlToObject($(data).children()[0]);
    }

    function time(iso) {
        return new Date(iso).toLocaleString('de-AT');
    }

    function badge(level) {
        return $('<span>').addClass('badge ' + level).text(level);
    }

    function td(text) {
        return $('<td>').text(text);
    }

    // Zeigt eine verstaendliche Fehlermeldung (aus JSON oder XML)
    function showError(xhr) {
        let message = 'Server nicht erreichbar';
        if (xhr && xhr.status > 0) {
            message = 'Fehler ' + xhr.status;
            const text = xhr.responseText || '';
            try {
                const body = text.startsWith('<') ? xmlToObject($.parseXML(text).documentElement) : JSON.parse(text);
                if (body.message) {
                    message += ': ' + body.message;
                }
            } catch (e) {
                // Antwort ohne lesbare Fehlermeldung
            }
        }
        $('#error').text(message).show();
    }

    function loadStations() {
        return load('/stations').done(function (data) {
            const selected = station();
            $('#station').empty();
            toList(data, 'stations', 'station').forEach(function (s) {
                const text = s.id + ' – ' + s.name + ' (' + s.currentWarningLevel + ')';
                $('#station').append($('<option>').val(s.id).text(text));
            });
            if (selected) {
                $('#station').val(selected);
            }
        });
    }

    function loadLatest() {
        return load('/stations/' + station() + '/measurements/latest').done(function (data) {
            const m = toObject(data);
            $('#latest').empty().append(
                $('<p>').text('Zeitpunkt: ' + time(m.timestamp)),
                $('<p>').text('Wasserstand: ' + m.waterLevel + ' cm'),
                $('<p>').text('Durchflussmenge: ' + m.flowRate + ' m³/s'),
                $('<p>').text('Niederschlag: ' + m.rainfall + ' mm/h'),
                $('<p>').text('Temperatur: ' + m.temperature + ' °C'),
                $('<p>').text('Akkustand: ' + m.batteryLevel + ' %'),
                $('<p>').text('Status: ' + m.status),
                $('<p>').append('Warnstufe: ', badge(m.warningLevel))
            );
        }).fail(function () {
            $('#latest').text('Kein aktueller Messwert vorhanden.');
        });
    }

    function loadHistory() {
        const params = {};
        if ($('#from').val()) params.from = new Date($('#from').val()).toISOString();
        if ($('#to').val()) params.to = new Date($('#to').val()).toISOString();
        if ($('#level').val()) params.warningLevel = $('#level').val();
        if ($('#limit').val()) params.limit = $('#limit').val();

        return load('/stations/' + station() + '/measurements', params).done(function (data) {
            const rows = toList(data, 'measurements', 'measurement');
            const body = $('#history').empty();
            rows.forEach(function (m) {
                $('<tr>').append(
                    td(m.stationId),
                    td(time(m.timestamp)),
                    td(m.waterLevel + ' cm'),
                    td(m.flowRate + ' m³/s'),
                    td(m.rainfall + ' mm/h'),
                    td(m.batteryLevel + ' %'),
                    td(m.status),
                    $('<td>').append(badge(m.warningLevel))
                ).appendTo(body);
            });
            if (rows.length === 0) {
                body.append('<tr><td colspan="8">Keine Messwerte gefunden.</td></tr>');
            }
        }).fail(function () {
            $('#history').empty();
        });
    }

    function loadAlerts() {
        return load('/alerts').done(function (data) {
            const list = $('#alerts').empty();
            const alerts = toList(data, 'alerts', 'alert');
            if (alerts.length === 0) {
                list.append('<li>Keine Warnungen</li>');
            }
            alerts.forEach(function (a) {
                list.append($('<li>').append(badge(a.warningLevel),
                    ' ' + a.stationName + ' (' + a.river + '): ' + a.waterLevel + ' cm'));
            });
        });
    }

    // Laedt alle Daten neu
    function refresh() {
        if (!auth) {
            return;
        }
        $('#error').hide();
        $('#loading').show();
        loadStations()
            .then(function () {
                return $.when(loadLatest(), loadHistory(), loadAlerts());
            })
            .fail(showError)
            .always(function () {
                $('#loading').hide();
                $('#updated').text('Aktualisiert: ' + new Date().toLocaleTimeString('de-AT'));
            });
    }

    $('#loginForm').on('submit', function (e) {
        e.preventDefault();
        auth = btoa($('#user').val() + ':' + $('#password').val());
        refresh();
        clearInterval(timer);
        timer = setInterval(function () {
            if ($('#auto').is(':checked')) {
                refresh();
            }
        }, 10000);
    });

    $('#filterForm').on('submit', function (e) {
        e.preventDefault();
        refresh();
    });

    $('#station, #format').on('change', refresh);
});
