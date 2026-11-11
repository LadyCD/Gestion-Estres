package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.ChequeoDiarioDTO;
import com.ucentral.gestionestres.dto.ChequeoDiarioRespuesta;
import com.ucentral.gestionestres.service.ChequeoDiarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU021: chequeo diario del usuario que inició sesión. */
@RestController
@RequestMapping("/api/cuestionarios/diario")
@Tag(name = "Autorreporte", description = "Chequeo diario de 3 preguntas (HU021)")
public class ChequeoDiarioController {

    private final ChequeoDiarioService chequeoDiarioService;

    public ChequeoDiarioController(ChequeoDiarioService chequeoDiarioService) {
        this.chequeoDiarioService = chequeoDiarioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TRABAJADOR')")
    @Operation(summary = "Guarda el chequeo diario del usuario autenticado")
    public ResponseEntity<ChequeoDiarioRespuesta> registrarChequeo(Authentication autenticacion,
                                                                   @Valid @RequestBody ChequeoDiarioDTO dto) {
        ChequeoDiarioRespuesta creado = chequeoDiarioService.registrarChequeo(autenticacion.getName(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
