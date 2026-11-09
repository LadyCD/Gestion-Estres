package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.Deteccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeteccionRepository extends JpaRepository<Deteccion, Integer> {
}
