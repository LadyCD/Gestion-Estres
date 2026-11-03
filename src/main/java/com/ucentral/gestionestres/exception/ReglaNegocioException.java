package com.ucentral.gestionestres.exception;

/**
 * Se lanza desde un service cuando se incumple una regla de negocio de una HU
 * (por ejemplo, correo ya registrado en HU001). Responde 409.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
