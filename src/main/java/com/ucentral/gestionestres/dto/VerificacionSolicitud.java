package com.ucentral.gestionestres.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de POST /api/usuarios/verificar (HU001). */
public record VerificacionSolicitud(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo,

        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "\\d{6}", message = "El código tiene 6 dígitos")
        String codigo
) {}
