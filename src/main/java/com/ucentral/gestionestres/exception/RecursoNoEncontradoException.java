package com.ucentral.gestionestres.exception;

/** Se lanza desde un service cuando lo solicitado no existe. Responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
