package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.model.EstadoLineaBase;
import com.ucentral.gestionestres.model.LineaBase;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.model.VentanaSenal;
import com.ucentral.gestionestres.repository.LineaBaseRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

/** Caso de prueba de HU025 (wiki): 5 días de 16 ventanas con FC 66/78 y HRV 35/55 alternadas. */
@ExtendWith(MockitoExtension.class)
class LineaBaseServiceTest {

    @Mock private LineaBaseRepository lineaBaseRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private LineaBaseService servicio;
    private Usuario usuario;
    private LineaBase lineaBase;

    @BeforeEach
    void preparar() {
        ZoneId bogota = ZoneId.of("America/Bogota");
        Clock reloj = Clock.fixed(LocalDateTime.of(2026, 11, 10, 9, 0).atZone(bogota).toInstant(), bogota);
        servicio = new LineaBaseService(lineaBaseRepository, usuarioRepository, reloj, 5, 16);
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 7);
        lineaBase = new LineaBase();
        lineaBase.setUsuario(usuario);
        when(lineaBaseRepository.findByUsuarioId(7)).thenReturn(Optional.of(lineaBase));
    }

    /** Carga {@code dias} días de 16 ventanas desde el 2 de noviembre a las 8:00. */
    private void cargarDias(int dias) {
        for (int d = 0; d < dias; d++) {
            for (int v = 0; v < 16; v++) {
                VentanaSenal ventana = new VentanaSenal();
                ventana.setUsuario(usuario);
                ventana.setInicio(LocalDateTime.of(2026, 11, 2 + d, 8, 0).plusMinutes(15L * v));
                ventana.setFin(ventana.getInicio().plusMinutes(15));
                boolean par = v % 2 == 0;
                ventana.setFcPromedio(par ? 66f : 78f);
                ventana.setHrvPromedio(par ? 35f : 55f);
                ventana.setNumLecturas(15);
                servicio.registrarVentana(usuario, ventana);
            }
        }
    }

    @Test
    @DisplayName("Con 5 días válidos queda LISTA: FC 72 ± 6,0 y HRV 45 ± 10,1")
    void cincoDiasDejanLaLineaBaseLista() {
        cargarDias(5);

        assertThat(lineaBase.getEstado()).isEqualTo(EstadoLineaBase.LISTA);
        assertThat(lineaBase.getDiasValidos()).isEqualTo(5);
        assertThat(lineaBase.getFechaCalculo()).isNotNull();
        assertThat(lineaBase.getFcBase()).isCloseTo(72f, within(0.01f));
        assertThat(lineaBase.getHrvBase()).isCloseTo(45f, within(0.01f));
        assertThat(LineaBaseService.desviacion(lineaBase.getFcN(), lineaBase.getFcM2())).isCloseTo(6.0, within(0.05));
        assertThat(LineaBaseService.desviacion(lineaBase.getHrvN(), lineaBase.getHrvM2())).isCloseTo(10.1, within(0.05));
    }

    @Test
    @DisplayName("Con 4 días válidos sigue EN_CALCULO")
    void cuatroDiasSigueEnCalculo() {
        cargarDias(4);

        assertThat(lineaBase.getEstado()).isEqualTo(EstadoLineaBase.EN_CALCULO);
        assertThat(lineaBase.getDiasValidos()).isEqualTo(4);
    }

    @Test
    @DisplayName("Un día con menos de 16 ventanas no cuenta como válido")
    void diaIncompletoNoCuenta() {
        for (int v = 0; v < 15; v++) {
            VentanaSenal ventana = new VentanaSenal();
            ventana.setInicio(LocalDateTime.of(2026, 11, 2, 8, 0).plusMinutes(15L * v));
            ventana.setFcPromedio(70f);
            servicio.registrarVentana(usuario, ventana);
        }
        assertThat(lineaBase.getDiasValidos()).isZero();
        assertThat(lineaBase.getHrvN()).isZero();   // sin HRV, solo se calcula FC
    }
}
