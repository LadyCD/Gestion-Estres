package com.ucentral.gestionestres.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginSolicitud(
        @NotBlank @Email String correo,
        @NotBlank String contrasena) {
}