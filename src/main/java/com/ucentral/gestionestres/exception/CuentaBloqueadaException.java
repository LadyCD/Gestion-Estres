package com.ucentral.gestionestres.exception;

public class CuentaBloqueadaException extends RuntimeException {
    public CuentaBloqueadaException(long minutosRestantes) {
        super("Cuenta bloqueada temporalmente. Intenta de nuevo en " + minutosRestantes + " minuto(s)");
    }
}