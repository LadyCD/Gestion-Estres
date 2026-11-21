package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.model.Cuestionario;
import com.ucentral.gestionestres.model.EstadoLineaBase;
import com.ucentral.gestionestres.model.LineaBase;
import com.ucentral.gestionestres.model.NivelEstres;
import com.ucentral.gestionestres.model.VentanaSenal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los 10 casos de prueba de HU003 (wiki). Línea base de ejemplo: FC 72 ± 6 lpm y HRV 45 ± 10 ms,
 * umbrales FC > 78 y HRV < 35.
 */
class ClasificadorEstresTest {

    private static LineaBase lineaBase(EstadoLineaBase estado) {
        LineaBase lb = new LineaBase();
        lb.setEstado(estado);
        lb.setFcN(101);
        lb.setFcBase(72f);
        lb.setFcM2(36.0 * 100);    // σ = 6
        lb.setHrvN(101);
        lb.setHrvBase(45f);
        lb.setHrvM2(100.0 * 100);  // σ = 10
        return lb;
    }

    private static List<VentanaSenal> ventanas(Float fc, Float hrv) {
        if (fc == null) {
            return List.of();
        }
        VentanaSenal v = new VentanaSenal();
        v.setInicio(LocalDateTime.of(2026, 11, 10, 9, 0));
        v.setFcPromedio(fc);
        v.setHrvPromedio(hrv);
        return List.of(v);
    }

    private static Cuestionario chequeo(Integer estres, Boolean tension) {
        if (estres == null) {
            return null;
        }
        Cuestionario c = new Cuestionario();
        c.setPuntaje(estres);
        c.setTension(tension);
        return c;
    }

    @ParameterizedTest(name = "Caso {0}: FC={1}, HRV={2}, estrés={3}, tensión={4} → {5}, parcial={6}")
    @CsvSource(nullValues = "-", value = {
            // caso, FC, HRV, estrés, tensión, línea base lista, nivel esperado, parcial
            "1, 75, 44, 2, false, true,  BAJO,     false",
            "2, 80, 40, 5, false, true,  MODERADO, false",
            "3, 85, 30, 8, false, true,  ALTO,     false",
            "4, 85, 30, 2, true,  true,  ALTO,     false",
            "5, 74, 32, 3, false, true,  BAJO,     false",
            "6, -,  -,  8, false, true,  ALTO,     true",
            "7, 80, 33, -, -,     true,  ALTO,     true",
            "8, 80, -,  -, -,     true,  MODERADO, true",
            "9, 76, 40, 5, false, false, MODERADO, true",
    })
    void casosDeLaWiki(int caso, Float fc, Float hrv, Integer estres, Boolean tension, boolean lista,
                       NivelEstres esperado, boolean parcialEsperado) {
        var lb = lineaBase(lista ? EstadoLineaBase.LISTA : EstadoLineaBase.EN_CALCULO);

        var resultado = ClasificadorEstres.clasificar(lb, ventanas(fc, hrv), chequeo(estres, tension), 1.0);

        assertThat(resultado).as("caso %d", caso).isPresent();
        assertThat(resultado.get().nivel()).as("caso %d", caso).isEqualTo(esperado);
        assertThat(resultado.get().parcial()).as("caso %d", caso).isEqualTo(parcialEsperado);
    }

    @Test
    @DisplayName("Caso 10: sin datos del wearable ni chequeo no hay detección")
    void caso10SinDatos() {
        assertThat(ClasificadorEstres.clasificar(lineaBase(EstadoLineaBase.LISTA), List.of(), null, 1.0)).isEmpty();
    }

    @Test
    @DisplayName("Sin línea base y sin chequeo tampoco hay detección")
    void sinLineaBaseNiChequeo() {
        assertThat(ClasificadorEstres.clasificar(null, ventanas(90f, 20f), null, 1.0)).isEmpty();
    }

    @Test
    @DisplayName("La desviación de la HRV se guarda en desviaciones estándar")
    void desviacionHrv() {
        var r = ClasificadorEstres.clasificar(lineaBase(EstadoLineaBase.LISTA), ventanas(75f, 30f), null, 1.0);
        assertThat(r.get().desviacionHrv()).isEqualTo(-1.5f);
    }
}
