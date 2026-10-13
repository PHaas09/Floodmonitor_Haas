package com.example.floodmonitor.model;

/** Technischer Zustand einer Messstation. */
public enum StationStatus {
    ONLINE,       // misst normal
    MAINTENANCE,  // Akku fast leer, wird geladen
    OFFLINE       // ausgefallen, misst nicht
}
