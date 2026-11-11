package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.LineaBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LineaBaseRepository extends JpaRepository<LineaBase, Integer> {

    Optional<LineaBase> findByUsuarioId(Integer usuarioId);
}
