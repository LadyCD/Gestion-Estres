package com.ucentral.gestionestres.controller;

import com.ucentral.gestionestres.dto.CatalogoDTO;
import com.ucentral.gestionestres.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
@Tag(name = "Catálogos", description = "Áreas y cargos para el formulario de registro (HU001)")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/areas")
    @Operation(summary = "Lista las áreas de la organización")
    public List<CatalogoDTO> listarAreas() {
        return catalogoService.listarAreas();
    }

    @GetMapping("/cargos")
    @Operation(summary = "Lista los cargos")
    public List<CatalogoDTO> listarCargos() {
        return catalogoService.listarCargos();
    }
}
