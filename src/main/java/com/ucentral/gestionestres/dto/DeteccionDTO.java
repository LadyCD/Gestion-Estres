package com.ucentral.gestionestres.dto;

import com.ucentral.gestionestres.model.NivelEstres;

import java.time.LocalDateTime;

/** GET /api/detecciones/ultima (HU003). Solo el indicador, sin señal ni datos del usuario. */
public record DeteccionDTO(LocalDateTime fechaHora, NivelEstres nivel, boolean parcial) {}
