package com.ucentral.gestionestres.dto;

/** Respuesta del chequeo guardado. No incluye datos del usuario. */
public record ChequeoDiarioRespuesta(Integer id, Integer estres, Integer energia, Boolean tension) {}
