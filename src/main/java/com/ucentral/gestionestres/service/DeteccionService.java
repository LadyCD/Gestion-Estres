package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.DeteccionDTO;
import com.ucentral.gestionestres.exception.RecursoNoEncontradoException;
import com.ucentral.gestionestres.model.Deteccion;
import com.ucentral.gestionestres.model.Rol;
import com.ucentral.gestionestres.model.TipoCuestionario;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.CuestionarioRepository;
import com.ucentral.gestionestres.repository.DeteccionRepository;
import com.ucentral.gestionestres.repository.LineaBaseRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import com.ucentral.gestionestres.repository.VentanaSenalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * HU003: ejecuta la clasificación (ClasificadorEstres) cada {@code app.deteccion.minutos-evaluacion}
 * minutos con la línea base (HU025), las ventanas de la última hora (HU026) y el chequeo del día (HU021).
 */
@Service
public class DeteccionService {

    private static final Logger log = LoggerFactory.getLogger(DeteccionService.class);

    private final UsuarioRepository usuarioRepository;
    private final LineaBaseRepository lineaBaseRepository;
    private final VentanaSenalRepository ventanaSenalRepository;
    private final CuestionarioRepository cuestionarioRepository;
    private final DeteccionRepository deteccionRepository;
    private final Clock reloj;
    private final int minutosEvaluacion;
    private final double desviacionesUmbral;

    public DeteccionService(UsuarioRepository usuarioRepository,
                            LineaBaseRepository lineaBaseRepository,
                            VentanaSenalRepository ventanaSenalRepository,
                            CuestionarioRepository cuestionarioRepository,
                            DeteccionRepository deteccionRepository,
                            Clock reloj,
                            @Value("${app.deteccion.minutos-evaluacion:60}") int minutosEvaluacion,
                            @Value("${app.deteccion.desviaciones-umbral:1.0}") double desviacionesUmbral) {
        this.usuarioRepository = usuarioRepository;
        this.lineaBaseRepository = lineaBaseRepository;
        this.ventanaSenalRepository = ventanaSenalRepository;
        this.cuestionarioRepository = cuestionarioRepository;
        this.deteccionRepository = deteccionRepository;
        this.reloj = reloj;
        this.minutosEvaluacion = minutosEvaluacion;
        this.desviacionesUmbral = desviacionesUmbral;
    }

    /** Clasificación programada para todos los trabajadores. */
    @Scheduled(fixedRateString = "${app.deteccion.minutos-evaluacion:60}",
            initialDelayString = "${app.deteccion.minutos-evaluacion:60}", timeUnit = TimeUnit.MINUTES)
    public void evaluarTodos() {
        int registradas = 0;
        for (Usuario usuario : usuarioRepository.findByRol(Rol.TRABAJADOR)) {
            if (evaluar(usuario).isPresent()) {
                registradas++;
            }
        }
        log.info("Clasificación horaria: {} detecciones registradas", registradas);
    }

    /** POST /api/detecciones/evaluar: clasifica ya al usuario de la sesión (útil para la demo). */
    @Transactional
    public Optional<DeteccionDTO> evaluarAhora(String correo) {
        return evaluar(buscarUsuario(correo));
    }

    /** GET /api/detecciones/ultima. */
    @Transactional(readOnly = true)
    public Optional<DeteccionDTO> ultima(String correo) {
        return deteccionRepository.findFirstByUsuarioIdOrderByFechaHoraDesc(buscarUsuario(correo).getId())
                .map(d -> new DeteccionDTO(d.getFechaHora(), d.getNivel(), Boolean.TRUE.equals(d.getEsParcial())));
    }

    @Transactional
    public Optional<DeteccionDTO> evaluar(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        var lineaBase = lineaBaseRepository.findByUsuarioId(usuario.getId()).orElse(null);
        var ventanas = ventanaSenalRepository.findByUsuarioIdAndInicioGreaterThanEqualOrderByInicioAsc(
                usuario.getId(), ahora.minusMinutes(minutosEvaluacion));
        var chequeo = cuestionarioRepository.findFirstByUsuarioIdAndTipoAndFechaHoraGreaterThanEqualOrderByFechaHoraDesc(
                usuario.getId(), TipoCuestionario.DIARIO, ahora.toLocalDate().atStartOfDay()).orElse(null);

        return ClasificadorEstres.clasificar(lineaBase, ventanas, chequeo, desviacionesUmbral).map(r -> {
            Deteccion d = new Deteccion();
            d.setUsuario(usuario);
            d.setFechaHora(ahora);
            d.setNivel(r.nivel());
            d.setEsParcial(r.parcial());
            d.setDesviacionBase(r.desviacionHrv());
            deteccionRepository.save(d);
            return new DeteccionDTO(ahora, r.nivel(), r.parcial());
        });
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
