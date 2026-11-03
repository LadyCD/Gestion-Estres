package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.RegistroAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {
}
