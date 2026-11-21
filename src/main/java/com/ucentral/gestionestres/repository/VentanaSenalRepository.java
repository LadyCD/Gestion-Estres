package com.ucentral.gestionestres.repository;

import com.ucentral.gestionestres.model.VentanaSenal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentanaSenalRepository extends JpaRepository<VentanaSenal, Long> {

    /** HU026: el servidor rechaza una ventana con el mismo inicio para el mismo usuario. */
    boolean existsByUsuarioIdAndInicio(Integer usuarioId, LocalDateTime inicio);

    /** HU003: ventanas de la última hora del usuario. */
    List<VentanaSenal> findByUsuarioIdAndInicioGreaterThanEqualOrderByInicioAsc(Integer usuarioId, LocalDateTime desde);

    /** HU026: las ventanas se eliminan a las 24 horas. */
    @Modifying
    @Query("delete from VentanaSenal v where v.recibidaEn < :limite")
    int eliminarRecibidasAntesDe(@Param("limite") LocalDateTime limite);
}
