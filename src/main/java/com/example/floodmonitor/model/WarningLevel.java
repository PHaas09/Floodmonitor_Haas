package com.example.floodmonitor.model;

/** Hochwasser-Warnstufe. Wird nur am Server berechnet. */
public enum WarningLevel {
    NORMAL,     // Wasserstand unter dem Warnwert
    WARNING,    // Wasserstand ab dem Warnwert
    CRITICAL,   // Wasserstand ab dem kritischen Wert
    UNKNOWN     // keine aktuelle oder gueltige Messung
}
