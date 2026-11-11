package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.ChequeoDiarioDTO;
import com.ucentral.gestionestres.model.Cuestionario;
import com.ucentral.gestionestres.service.ChequeoDiarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chequeo-diario")
public class ChequeoDiarioController {

    private final ChequeoDiarioService chequeoDiarioService;

    public ChequeoDiarioController(ChequeoDiarioService chequeoDiarioService) {
        this.chequeoDiarioService = chequeoDiarioService;
    }

    @PostMapping
    public ResponseEntity<Cuestionario> registrarChequeo(@Valid @RequestBody ChequeoDiarioDTO dto) {
        Cuestionario creado = chequeoDiarioService.registrarChequeo(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}