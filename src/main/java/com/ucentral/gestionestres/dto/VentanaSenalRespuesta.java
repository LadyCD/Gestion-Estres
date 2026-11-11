package com.ucentral.gestionestres.dto;

import java.time.LocalDateTime;

/** Respuesta de la ventana recibida. No incluye datos del usuario. */
public record VentanaSenalRespuesta(Long id, LocalDateTime inicio, LocalDateTime fin) {}
