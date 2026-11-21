package com.ucentral.gestionestres.dto;

import com.ucentral.gestionestres.model.Rol;

/** Respuesta 201 del registro: nunca incluye el hash de la contraseña. */
public record UsuarioRespuesta(Integer id, String nombreCompleto, String correo, Rol rol) {}