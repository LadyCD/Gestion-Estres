package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.VentanaSenalDTO;
import com.ucentral.gestionestres.model.VentanaSenal;
import com.ucentral.gestionestres.service.VentanaSenalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventanas-senal")
public class VentanaSenalController {

    private final VentanaSenalService ventanaSenalService;

    public VentanaSenalController(VentanaSenalService ventanaSenalService) {
        this.ventanaSenalService = ventanaSenalService;
    }

    @PostMapping
    public ResponseEntity<VentanaSenal> registrarVentana(@Valid @RequestBody VentanaSenalDTO dto) {
        VentanaSenal creada = ventanaSenalService.registrarVentana(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}