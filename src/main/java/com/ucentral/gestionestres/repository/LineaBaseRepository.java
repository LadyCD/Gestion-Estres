package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.LineaBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LineaBaseRepository extends JpaRepository<LineaBase, Integer> {
}
