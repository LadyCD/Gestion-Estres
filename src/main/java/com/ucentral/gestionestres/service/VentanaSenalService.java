package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.VentanaSenalDTO;
import com.ucentral.gestionestres.dto.VentanaSenalRespuesta;
import com.ucentral.gestionestres.exception.ReglaNegocioException;
import com.ucentral.gestionestres.exception.SolicitudInvalidaException;
import com.ucentral.gestionestres.exception.TokenDispositivoInvalidoException;
import com.ucentral.gestionestres.model.DispositivoVinculado;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.model.VentanaSenal;
import com.ucentral.gestionestres.repository.DispositivoVinculadoRepository;
import com.ucentral.gestionestres.repository.VentanaSenalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * HU026: guarda las ventanas de 15 minutos que envía la app y actualiza la última sincronización.
 * Las ventanas se conservan solo {@code app.sincronizacion.horas-retencion} horas.
 */
@Service
public class VentanaSenalService {

    private static final Logger log = LoggerFactory.getLogger(VentanaSenalService.class);

    private final VentanaSenalRepository ventanaSenalRepository;
    private final DispositivoVinculadoRepository dispositivoRepository;
    private final DispositivoService dispositivoService;
    private final Clock reloj;

    @Value("${app.sincronizacion.horas-retencion:24}")
    private int horasRetencion;

    public VentanaSenalService(VentanaSenalRepository ventanaSenalRepository,
                               DispositivoVinculadoRepository dispositivoRepository,
                               DispositivoService dispositivoService,
                               Clock reloj) {
        this.ventanaSenalRepository = ventanaSenalRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.dispositivoService = dispositivoService;
        this.reloj = reloj;
    }

    @Transactional
    public VentanaSenalRespuesta registrarVentana(String tokenDispositivo, VentanaSenalDTO dto) {
        DispositivoVinculado dispositivo = dispositivoService.buscarPorToken(tokenDispositivo)
                .orElseThrow(TokenDispositivoInvalidoException::new);
        Usuario usuario = dispositivo.getUsuario();

        if (!dto.fin().isAfter(dto.inicio())) {
            throw new SolicitudInvalidaException("El fin de la ventana debe ser posterior a su inicio");
        }
        if (ventanaSenalRepository.existsByUsuarioIdAndInicio(usuario.getId(), dto.inicio())) {
            throw new ReglaNegocioException("Ya se recibió una ventana con ese inicio");
        }

        VentanaSenal ventana = new VentanaSenal();
        ventana.setUsuario(usuario);
        ventana.setInicio(dto.inicio());
        ventana.setFin(dto.fin());
        ventana.setFcPromedio(dto.fcPromedio());
        ventana.setHrvPromedio(dto.hrvPromedio());
        ventana.setNumLecturas(dto.numLecturas());
        VentanaSenal guardada = ventanaSenalRepository.save(ventana);

        // La app muestra esta fecha como "Última sincronización" (HU002)
        dispositivo.setUltimaSincronizacion(LocalDateTime.now(reloj));
        dispositivoRepository.save(dispositivo);

        return new VentanaSenalRespuesta(guardada.getId(), guardada.getInicio(), guardada.getFin());
    }

    /** Cada hora elimina las ventanas con más de 24 horas (privacidad: no se acumula la señal). */
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void eliminarVentanasVencidas() {
        int eliminadas = ventanaSenalRepository.eliminarRecibidasAntesDe(
                LocalDateTime.now(reloj).minusHours(horasRetencion));
        if (eliminadas > 0) {
            log.info("Se eliminaron {} ventanas con más de {} horas", eliminadas, horasRetencion);
        }
    }
}
