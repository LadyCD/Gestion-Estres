package com.ucentral.gestionestres.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /api/usuarios/reenviar-codigo (HU001). */
public record CorreoSolicitud(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String correo
) {}
