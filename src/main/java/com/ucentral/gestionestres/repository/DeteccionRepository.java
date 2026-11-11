package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.Deteccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeteccionRepository extends JpaRepository<Deteccion, Integer> {

    /** HU003: la detección más reciente del usuario (pantalla de inicio de la app). */
    Optional<Deteccion> findFirstByUsuarioIdOrderByFechaHoraDesc(Integer usuarioId);
}
