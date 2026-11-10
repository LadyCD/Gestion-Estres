package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.Consentimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Integer> {

    /** HU001, HU002: ¿el usuario tiene un consentimiento informado vigente? */
    boolean existsByUsuarioIdAndVigenteTrue(Integer usuarioId);
}
