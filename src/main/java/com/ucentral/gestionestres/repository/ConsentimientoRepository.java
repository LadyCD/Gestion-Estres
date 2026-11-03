package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.Consentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Integer> {
}
