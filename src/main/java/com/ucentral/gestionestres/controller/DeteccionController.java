package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.DeteccionDTO;
import com.ucentral.gestionestres.service.DeteccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU003: nivel de estrés del usuario que inició sesión. Solo el rol TRABAJADOR ve sus detecciones;
 * cualquier otro rol recibe 403 (HU020).
 */
@RestController
@RequestMapping("/api/detecciones")
@Tag(name = "Detección", description = "Clasificación del nivel de estrés (HU003)")
@PreAuthorize("hasRole('TRABAJADOR')")
public class DeteccionController {

    private final DeteccionService deteccionService;

    public DeteccionController(DeteccionService deteccionService) {
        this.deteccionService = deteccionService;
    }

    @GetMapping("/ultima")
    @Operation(summary = "Última detección (204 si todavía no hay ninguna)")
    public ResponseEntity<DeteccionDTO> ultima(Authentication autenticacion) {
        return deteccionService.ultima(autenticacion.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/evaluar")
    @Operation(summary = "Clasifica ahora con los datos de la última hora (204 si no hay datos suficientes)")
    public ResponseEntity<DeteccionDTO> evaluar(Authentication autenticacion) {
        return deteccionService.evaluarAhora(autenticacion.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
