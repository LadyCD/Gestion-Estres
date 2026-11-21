package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.DispositivoVinculado;
import com.ucentral.gestionestres.model.EstadoDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DispositivoVinculadoRepository extends JpaRepository<DispositivoVinculado, Integer> {

    Optional<DispositivoVinculado> findByUsuarioId(Integer usuarioId);

    /** HU026: busca el dispositivo dueño de una credencial (solo si sigue vinculado). */
    Optional<DispositivoVinculado> findByTokenHashAndEstado(String tokenHash, EstadoDispositivo estado);
}
