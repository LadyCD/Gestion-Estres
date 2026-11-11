package com.ucentral.gestionestres.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST /api/usuarios (HU001). El rol no se recibe: siempre es TRABAJADOR. */
public record RegistroSolicitud(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String contrasena,

        @NotNull(message = "El cargo es obligatorio")
        Integer cargoId,

        @NotNull(message = "El área es obligatoria")
        Integer areaId,

        @NotBlank(message = "Debe aceptar el consentimiento informado")
        @Size(max = 20)
        String versionConsentimiento
) {}