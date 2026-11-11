package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.LineaBaseDTO;
import com.ucentral.gestionestres.service.LineaBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU025: progreso de la línea base del usuario que inició sesión. */
@RestController
@RequestMapping("/api/linea-base")
@Tag(name = "Línea base", description = "Línea base individual de FC y HRV (HU025)")
public class LineaBaseController {

    private final LineaBaseService lineaBaseService;

    public LineaBaseController(LineaBaseService lineaBaseService) {
        this.lineaBaseService = lineaBaseService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TRABAJADOR')")
    @Operation(summary = "Estado de la línea base: EN_CALCULO (día X de 5) o LISTA")
    public LineaBaseDTO obtener(Authentication autenticacion) {
        return lineaBaseService.obtener(autenticacion.getName());
    }
}
