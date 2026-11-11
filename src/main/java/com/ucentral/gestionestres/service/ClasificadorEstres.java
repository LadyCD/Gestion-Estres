package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.model.Cuestionario;
import com.ucentral.gestionestres.model.EstadoLineaBase;
import com.ucentral.gestionestres.model.LineaBase;
import com.ucentral.gestionestres.model.NivelEstres;
import com.ucentral.gestionestres.model.VentanaSenal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * HU003: reglas de clasificación del nivel de estrés (sin aprendizaje automático).
 *
 * <ul>
 *   <li>Puntaje fisiológico (0 a 2): +1 si la FC promedio de la hora supera la FC base + k·σ;
 *       +1 si la HRV promedio es menor que la HRV base − k·σ.</li>
 *   <li>Puntaje de autorreporte (0 a 2): estrés 0-3 → 0, 4-6 → 1, 7-10 → 2; tensión «Sí» suma 1, máximo 2.</li>
 *   <li>Con las dos fuentes: total 0-1 = BAJO, 2 = MODERADO, 3-4 = ALTO.</li>
 *   <li>Con una sola fuente: 0 = BAJO, 1 = MODERADO, 2 = ALTO, y la detección es parcial.
 *       Sin HRV solo se evalúa la regla de FC y la detección es parcial.</li>
 *   <li>Sin ninguna fuente: no hay detección.</li>
 * </ul>
 * Clase sin estado ni base de datos: se prueba con los 10 casos de la wiki.
 */
public final class ClasificadorEstres {

    /** Resultado de una clasificación. desviacionHrv en desviaciones estándar (null sin HRV). */
    public record Resultado(NivelEstres nivel, boolean parcial, Float desviacionHrv) {}

    private ClasificadorEstres() { }

    /**
     * @param lineaBase  línea base del usuario (puede ser null o estar EN_CALCULO)
     * @param ventanas   ventanas de la última hora (puede estar vacía)
     * @param chequeo    chequeo diario más reciente del mismo día (puede ser null)
     * @param k          número de desviaciones estándar del umbral (1.0)
     */
    public static Optional<Resultado> clasificar(LineaBase lineaBase, List<VentanaSenal> ventanas,
                                                 Cuestionario chequeo, double k) {
        boolean hayLineaBase = lineaBase != null && lineaBase.getEstado() == EstadoLineaBase.LISTA;
        boolean hayVentanas = ventanas != null && !ventanas.isEmpty();
        boolean hayFisiologico = hayLineaBase && hayVentanas;
        boolean hayChequeo = chequeo != null && chequeo.getPuntaje() != null;

        if (!hayFisiologico && !hayChequeo) {
            return Optional.empty();   // «Sin datos suficientes en la última hora»
        }

        int fisiologico = 0;
        boolean sinHrv = false;
        Float desviacionHrv = null;
        if (hayFisiologico) {
            double fcPromedio = ventanas.stream().mapToDouble(VentanaSenal::getFcPromedio).average().orElseThrow();
            double fcDesv = LineaBaseService.desviacion(lineaBase.getFcN(), lineaBase.getFcM2());
            if (fcPromedio > lineaBase.getFcBase() + k * fcDesv) {
                fisiologico++;
            }

            List<Float> hrv = ventanas.stream().map(VentanaSenal::getHrvPromedio).filter(Objects::nonNull).toList();
            double hrvDesv = LineaBaseService.desviacion(lineaBase.getHrvN(), lineaBase.getHrvM2());
            if (hrv.isEmpty() || lineaBase.getHrvBase() == null || Double.isNaN(hrvDesv) || hrvDesv == 0) {
                sinHrv = true;
            } else {
                double hrvPromedio = hrv.stream().mapToDouble(Float::doubleValue).average().orElseThrow();
                if (hrvPromedio < lineaBase.getHrvBase() - k * hrvDesv) {
                    fisiologico++;
                }
                desviacionHrv = (float) ((hrvPromedio - lineaBase.getHrvBase()) / hrvDesv);
            }
        }

        int autorreporte = 0;
        if (hayChequeo) {
            int estres = chequeo.getPuntaje();
            autorreporte = estres <= 3 ? 0 : estres <= 6 ? 1 : 2;
            if (Boolean.TRUE.equals(chequeo.getTension())) {
                autorreporte = Math.min(2, autorreporte + 1);
            }
        }

        NivelEstres nivel;
        boolean parcial;
        if (hayFisiologico && hayChequeo) {
            int total = fisiologico + autorreporte;
            nivel = total <= 1 ? NivelEstres.BAJO : total == 2 ? NivelEstres.MODERADO : NivelEstres.ALTO;
            parcial = sinHrv;
        } else {
            int unico = hayFisiologico ? fisiologico : autorreporte;
            nivel = unico == 0 ? NivelEstres.BAJO : unico == 1 ? NivelEstres.MODERADO : NivelEstres.ALTO;
            parcial = true;
        }
        return Optional.of(new Resultado(nivel, parcial, desviacionHrv));
    }
}
