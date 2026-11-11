package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.VentanaSenalDTO;
import com.ucentral.gestionestres.exception.ReglaNegocioException;
import com.ucentral.gestionestres.exception.SolicitudInvalidaException;
import com.ucentral.gestionestres.exception.TokenDispositivoInvalidoException;
import com.ucentral.gestionestres.model.DispositivoVinculado;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.model.VentanaSenal;
import com.ucentral.gestionestres.repository.DispositivoVinculadoRepository;
import com.ucentral.gestionestres.repository.VentanaSenalRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Criterios de HU026 en la capa de servicio. */
@ExtendWith(MockitoExtension.class)
class VentanaSenalServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 10, 8, 30);

    @Mock private VentanaSenalRepository ventanaRepository;
    @Mock private DispositivoVinculadoRepository dispositivoRepository;
    @Mock private DispositivoService dispositivoService;

    private VentanaSenalService servicio;
    private DispositivoVinculado dispositivo;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        servicio = new VentanaSenalService(ventanaRepository, dispositivoRepository, dispositivoService, reloj);
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 7);
        dispositivo = new DispositivoVinculado();
        dispositivo.setUsuario(usuario);
    }

    private static VentanaSenalDTO ventana(LocalDateTime inicio, LocalDateTime fin) {
        return new VentanaSenalDTO(inicio, fin, 74.5f, 42.0f, 15);
    }

    @Test
    @DisplayName("Con credencial válida guarda la ventana y actualiza la última sincronización")
    void guardaYActualizaSincronizacion() {
        when(dispositivoService.buscarPorToken("token-ok")).thenReturn(Optional.of(dispositivo));
        when(ventanaRepository.existsByUsuarioIdAndInicio(7, INICIO)).thenReturn(false);
        when(ventanaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var r = servicio.registrarVentana("token-ok", ventana(INICIO, INICIO.plusMinutes(15)));

        assertThat(r.inicio()).isEqualTo(INICIO);
        assertThat(dispositivo.getUltimaSincronizacion()).isEqualTo(AHORA);
        verify(dispositivoRepository).save(dispositivo);
    }

    @Test
    @DisplayName("Sin credencial o con una anulada responde 401 y no guarda")
    void credencialInvalida() {
        when(dispositivoService.buscarPorToken(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrarVentana(null, ventana(INICIO, INICIO.plusMinutes(15))))
                .isInstanceOf(TokenDispositivoInvalidoException.class);
        verify(ventanaRepository, never()).save(any(VentanaSenal.class));
    }

    @Test
    @DisplayName("Una ventana repetida (mismo inicio) se rechaza con 409, sin duplicados")
    void ventanaRepetida() {
        when(dispositivoService.buscarPorToken("token-ok")).thenReturn(Optional.of(dispositivo));
        when(ventanaRepository.existsByUsuarioIdAndInicio(7, INICIO)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrarVentana("token-ok", ventana(INICIO, INICIO.plusMinutes(15))))
                .isInstanceOf(ReglaNegocioException.class);
        verify(ventanaRepository, never()).save(any(VentanaSenal.class));
    }

    @Test
    @DisplayName("El fin debe ser posterior al inicio (400)")
    void finAntesDeInicio() {
        when(dispositivoService.buscarPorToken("token-ok")).thenReturn(Optional.of(dispositivo));

        assertThatThrownBy(() -> servicio.registrarVentana("token-ok", ventana(INICIO, INICIO)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("La limpieza elimina las ventanas recibidas hace más de 24 horas")
    void limpieza() {
        ReflectionTestUtils.setField(servicio, "horasRetencion", 24);
        servicio.eliminarVentanasVencidas();
        verify(ventanaRepository).eliminarRecibidasAntesDe(AHORA.minusHours(24));
    }
}
