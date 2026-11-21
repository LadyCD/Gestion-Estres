package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.Cuestionario;
import com.ucentral.gestionestres.model.TipoCuestionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CuestionarioRepository extends JpaRepository<Cuestionario, Integer> {

    /** HU003: el chequeo diario más reciente del mismo día. */
    Optional<Cuestionario> findFirstByUsuarioIdAndTipoAndFechaHoraGreaterThanEqualOrderByFechaHoraDesc(
            Integer usuarioId, TipoCuestionario tipo, LocalDateTime desde);
}
