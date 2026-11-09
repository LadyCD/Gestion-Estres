package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.DispositivoVinculado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DispositivoVinculadoRepository extends JpaRepository<DispositivoVinculado, Integer> {
}
