package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.DispositivoDTO;
import com.ucentral.gestionestres.dto.DispositivoSolicitud;
import com.ucentral.gestionestres.service.DispositivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * HU002: estado, vinculación y desvinculación del wearable del usuario que inició sesión.
 * El usuario se toma de la sesión (Authentication.getName() = correo, ver HU020).
 */
@RestController
@RequestMapping("/api/dispositivo")
@Tag(name = "Dispositivo", description = "Vincular y desvincular el wearable por Health Connect (HU002)")
public class DispositivoController {

    private final DispositivoService dispositivoService;

    public DispositivoController(DispositivoService dispositivoService) {
        this.dispositivoService = dispositivoService;
    }

    @GetMapping
    @Operation(summary = "Estado del dispositivo y última sincronización")
    public DispositivoDTO obtener(Authentication autenticacion) {
        return dispositivoService.obtener(autenticacion.getName());
    }

    @PutMapping
    @Operation(summary = "Vincula (entrega tokenDispositivo) o desvincula el wearable",
            description = "409 si el usuario no tiene un consentimiento informado vigente.")
    public DispositivoDTO actualizar(Authentication autenticacion,
                                     @Valid @RequestBody DispositivoSolicitud solicitud) {
        return dispositivoService.actualizar(autenticacion.getName(), solicitud);
    }
}
