package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.VentanaSenal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentanaSenalRepository extends JpaRepository<VentanaSenal, Long> {
}
