package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.LoginRespuesta;
import com.ucentral.gestionestres.dto.LoginSolicitud;
import com.ucentral.gestionestres.service.AuthServicio;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServicio auth;

    public AuthController(AuthServicio auth) { this.auth = auth; }

    @PostMapping("/login")
    public ResponseEntity<LoginRespuesta> login(@Valid @RequestBody LoginSolicitud solicitud) {
        return ResponseEntity.ok(auth.login(solicitud));
    }

    // JWT sin estado: el servidor no guarda sesión; la app descarta el token.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}