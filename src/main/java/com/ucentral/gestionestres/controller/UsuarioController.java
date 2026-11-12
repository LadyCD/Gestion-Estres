package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.CorreoSolicitud;
import com.ucentral.gestionestres.dto.RegistroSolicitud;
import com.ucentral.gestionestres.dto.VerificacionSolicitud;
import com.ucentral.gestionestres.dto.UsuarioRespuesta;
import com.ucentral.gestionestres.service.UsuarioService;
import com.ucentral.gestionestres.service.VerificacionCorreoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HU001: registro de usuarios y verificación del correo. Rutas públicas. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final VerificacionCorreoService verificacionCorreo;

    public UsuarioController(UsuarioService usuarioService, VerificacionCorreoService verificacionCorreo) {
        this.usuarioService = usuarioService;
        this.verificacionCorreo = verificacionCorreo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta registrar(@Valid @RequestBody RegistroSolicitud solicitud) {
        return usuarioService.registrar(solicitud);
    }

    /** 204 si el código es correcto; 400 si es incorrecto, venció o se agotaron los intentos. */
    @PostMapping("/verificar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificar(@Valid @RequestBody VerificacionSolicitud solicitud) {
        verificacionCorreo.verificar(solicitud.correo(), solicitud.codigo());
    }

    /** 202 siempre, exista o no la cuenta (no revela qué correos están registrados). */
    @PostMapping("/reenviar-codigo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reenviarCodigo(@Valid @RequestBody CorreoSolicitud solicitud) {
        verificacionCorreo.reenviar(solicitud.correo());
    }
}