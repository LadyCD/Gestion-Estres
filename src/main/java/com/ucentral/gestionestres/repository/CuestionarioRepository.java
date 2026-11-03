package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.Cuestionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CuestionarioRepository extends JpaRepository<Cuestionario, Integer> {
}
