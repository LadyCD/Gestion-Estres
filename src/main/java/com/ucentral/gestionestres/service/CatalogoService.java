package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.CatalogoDTO;
import com.ucentral.gestionestres.repository.AreaRepository;
import com.ucentral.gestionestres.repository.CargoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogos de áreas y cargos. Los usa el formulario de registro (HU001).
 * Ejemplo de referencia de las 3 capas: controller -> service -> repository.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;

    public CatalogoService(AreaRepository areaRepository, CargoRepository cargoRepository) {
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
    }

    public List<CatalogoDTO> listarAreas() {
        return areaRepository.findAllByOrderByNombreAsc().stream()
                .map(a -> new CatalogoDTO(a.getId(), a.getNombre()))
                .toList();
    }

    public List<CatalogoDTO> listarCargos() {
        return cargoRepository.findAllByOrderByNombreAsc().stream()
                .map(c -> new CatalogoDTO(c.getId(), c.getNombre()))
                .toList();
    }
}
