package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.RegistroSolicitud;
import com.ucentral.gestionestres.dto.UsuarioRespuesta;
import com.ucentral.gestionestres.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HU001: registro de usuarios. Ruta pública. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta registrar(@Valid @RequestBody RegistroSolicitud solicitud) {
        return usuarioService.registrar(solicitud);
    }
}