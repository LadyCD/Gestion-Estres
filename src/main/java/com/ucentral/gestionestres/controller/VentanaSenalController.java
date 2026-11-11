package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.VentanaSenalDTO;
import com.ucentral.gestionestres.dto.VentanaSenalRespuesta;
import com.ucentral.gestionestres.service.VentanaSenalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU026: recibe las ventanas de 15 minutos que envía la app en segundo plano. */
@RestController
@RequestMapping("/api/ventanas")
@Tag(name = "Sincronización", description = "Ventanas de 15 minutos de FC y HRV (HU026)")
public class VentanaSenalController {

    private final VentanaSenalService ventanaSenalService;

    public VentanaSenalController(VentanaSenalService ventanaSenalService) {
        this.ventanaSenalService = ventanaSenalService;
    }

    @PostMapping
    @Operation(summary = "Registra una ventana con la credencial del dispositivo",
            description = "401 si la credencial no es válida; 409 si ya existe una ventana con el mismo inicio.")
    public ResponseEntity<VentanaSenalRespuesta> registrarVentana(
            @RequestHeader(value = "X-Token-Dispositivo", required = false) String tokenDispositivo,
            @Valid @RequestBody VentanaSenalDTO dto) {
        VentanaSenalRespuesta creada = ventanaSenalService.registrarVentana(tokenDispositivo, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
