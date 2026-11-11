package com.ucentral.gestionestres.dto;

import com.ucentral.gestionestres.model.EstadoLineaBase;

/** GET /api/linea-base (HU025): progreso que muestra la app ("día X de 5 días válidos"). */
public record LineaBaseDTO(EstadoLineaBase estado, int diasValidos, int diasRequeridos) {}
