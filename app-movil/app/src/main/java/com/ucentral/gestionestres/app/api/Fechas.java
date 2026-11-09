package com.ucentral.gestionestres.app.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Conversión de fechas entre la app y el backend (hora de Colombia, sin zona en el texto). */
public final class Fechas {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter LEGIBLE = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a");

    private Fechas() { }

    public static String aTexto(Instant instante) {
        return LocalDateTime.ofInstant(instante, ZONA).format(ISO);
    }

    /** "2026-11-09T08:15:00" → "09/11/2026 8:15 a. m." */
    public static String legible(String isoDelBackend) {
        if (isoDelBackend == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(isoDelBackend).format(LEGIBLE);
        } catch (Exception e) {
            return isoDelBackend;
        }
    }
}
