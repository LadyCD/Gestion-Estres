package com.ucentral.gestionestres.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato único de error que reciben la app y Swagger. */
public record ErrorRespuesta(LocalDateTime fecha, int estado, String mensaje, Map<String, String> campos) {
}
