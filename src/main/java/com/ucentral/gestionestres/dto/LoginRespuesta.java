package com.ucentral.gestionestres.dto;

import com.ucentral.gestionestres.model.Rol;

public record LoginRespuesta(String token, Rol rol, String nombreCompleto, long expiraEnMinutos) {
}