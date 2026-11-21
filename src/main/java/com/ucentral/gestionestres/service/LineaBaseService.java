package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.LineaBaseDTO;
import com.ucentral.gestionestres.exception.RecursoNoEncontradoException;
import com.ucentral.gestionestres.model.EstadoLineaBase;
import com.ucentral.gestionestres.model.LineaBase;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.model.VentanaSenal;
import com.ucentral.gestionestres.repository.LineaBaseRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HU025: línea base individual de FC y HRV.
 *
 * Cada ventana de 15 minutos (HU026) actualiza de forma incremental el promedio y la suma de
 * cuadrados (algoritmo de Welford), así no se acumula la señal. Un día es válido al llegar a
 * {@code ventanas-minimas-por-dia} ventanas (16 = 4 horas) y la línea base queda LISTA al completar
 * {@code dias-requeridos} días válidos (5). En el prototipo no se recalcula una vez lista.
 */
@Service
public class LineaBaseService {

    private final LineaBaseRepository lineaBaseRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;
    private final int diasRequeridos;
    private final int ventanasMinimasPorDia;

    public LineaBaseService(LineaBaseRepository lineaBaseRepository,
                            UsuarioRepository usuarioRepository,
                            Clock reloj,
                            @Value("${app.linea-base.dias-requeridos:5}") int diasRequeridos,
                            @Value("${app.linea-base.ventanas-minimas-por-dia:16}") int ventanasMinimasPorDia) {
        this.lineaBaseRepository = lineaBaseRepository;
        this.usuarioRepository = usuarioRepository;
        this.reloj = reloj;
        this.diasRequeridos = diasRequeridos;
        this.ventanasMinimasPorDia = ventanasMinimasPorDia;
    }

    /** Llamado por VentanaSenalService con cada ventana recibida. */
    @Transactional
    public void registrarVentana(Usuario usuario, VentanaSenal ventana) {
        LineaBase lb = lineaBaseRepository.findByUsuarioId(usuario.getId()).orElseGet(() -> {
            LineaBase nueva = new LineaBase();
            nueva.setUsuario(usuario);
            return nueva;
        });
        if (lb.getEstado() == EstadoLineaBase.LISTA) {
            return;   // en el prototipo la línea base no se recalcula
        }

        LocalDate dia = ventana.getInicio().toLocalDate();
        if (!dia.equals(lb.getDiaEnCurso())) {
            lb.setDiaEnCurso(dia);
            lb.setVentanasDiaEnCurso(0);
        }

        // Welford para FC
        int nFc = lb.getFcN() + 1;
        double mediaFc = lb.getFcBase();
        double deltaFc = ventana.getFcPromedio() - mediaFc;
        mediaFc += deltaFc / nFc;
        lb.setFcM2(lb.getFcM2() + deltaFc * (ventana.getFcPromedio() - mediaFc));
        lb.setFcN(nFc);
        lb.setFcBase((float) mediaFc);

        // Welford para HRV (si el wearable la entrega)
        if (ventana.getHrvPromedio() != null) {
            int nHrv = lb.getHrvN() + 1;
            double mediaHrv = lb.getHrvBase() == null ? 0 : lb.getHrvBase();
            double deltaHrv = ventana.getHrvPromedio() - mediaHrv;
            mediaHrv += deltaHrv / nHrv;
            lb.setHrvM2(lb.getHrvM2() + deltaHrv * (ventana.getHrvPromedio() - mediaHrv));
            lb.setHrvN(nHrv);
            lb.setHrvBase((float) mediaHrv);
        }

        int ventanasDia = lb.getVentanasDiaEnCurso() + 1;
        lb.setVentanasDiaEnCurso(ventanasDia);
        if (ventanasDia == ventanasMinimasPorDia) {
            int dias = lb.getDiasValidos() + 1;
            lb.setDiasValidos(dias);
            if (dias >= diasRequeridos) {
                lb.setEstado(EstadoLineaBase.LISTA);
                lb.setFechaCalculo(LocalDateTime.now(reloj));
            }
        }
        lineaBaseRepository.save(lb);
    }

    /** GET /api/linea-base del usuario de la sesión. */
    @Transactional(readOnly = true)
    public LineaBaseDTO obtener(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return lineaBaseRepository.findByUsuarioId(usuario.getId())
                .map(lb -> new LineaBaseDTO(lb.getEstado(), lb.getDiasValidos(), diasRequeridos))
                .orElse(new LineaBaseDTO(EstadoLineaBase.EN_CALCULO, 0, diasRequeridos));
    }

    /** Desviación estándar muestral a partir de n y M2 (igual a la columna generada en MySQL). */
    public static double desviacion(int n, double m2) {
        return n > 1 ? Math.sqrt(m2 / (n - 1)) : Double.NaN;
    }
}
