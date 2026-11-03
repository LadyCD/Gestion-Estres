package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Integer> {
}
