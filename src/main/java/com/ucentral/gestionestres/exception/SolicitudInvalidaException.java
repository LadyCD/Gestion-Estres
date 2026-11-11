package com.ucentral.gestionestres.exception;

/**
 * Se lanza desde un service cuando la solicitud incumple una regla de entrada
 * (por ejemplo, dominio de correo no permitido en HU001). Responde 400.
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}