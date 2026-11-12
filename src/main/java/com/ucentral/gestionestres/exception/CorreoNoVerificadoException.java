package com.ucentral.gestionestres.exception;

/** HU001: la contraseña es correcta pero el correo aún no se verificó. Responde 403. */
public class CorreoNoVerificadoException extends RuntimeException {

    public CorreoNoVerificadoException() {
        super("Debe verificar su correo antes de ingresar. Escriba el código que le enviamos.");
    }
}
