package com.ucentral.gestionestres.exception;

/** HU026: la credencial del dispositivo falta, no existe o fue anulada al desvincular. Responde 401. */
public class TokenDispositivoInvalidoException extends RuntimeException {

    public TokenDispositivoInvalidoException() {
        super("La credencial del dispositivo no es válida. Vincule el dispositivo de nuevo.");
    }
}
