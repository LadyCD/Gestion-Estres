package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.DispositivoDTO;
import com.ucentral.gestionestres.dto.DispositivoSolicitud;
import com.ucentral.gestionestres.exception.ReglaNegocioException;
import com.ucentral.gestionestres.model.DispositivoVinculado;
import com.ucentral.gestionestres.model.EstadoDispositivo;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.ConsentimientoRepository;
import com.ucentral.gestionestres.repository.DispositivoVinculadoRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Pruebas de los criterios de aceptación de HU002 en la capa de servicio. */
@ExtendWith(MockitoExtension.class)
class DispositivoServiceTest {

    private static final String CORREO = "ana.perez@ucentral.edu.co";

    @Mock private DispositivoVinculadoRepository dispositivoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ConsentimientoRepository consentimientoRepository;

    private DispositivoService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servicio = new DispositivoService(dispositivoRepository, usuarioRepository, consentimientoRepository);
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 7);
        lenient().when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
    }

    @Test
    @DisplayName("Sin consentimiento vigente no se vincula (409)")
    void noVinculaSinConsentimiento() {
        when(consentimientoRepository.existsByUsuarioIdAndVigenteTrue(7)).thenReturn(false);

        assertThatThrownBy(() -> servicio.actualizar(CORREO,
                new DispositivoSolicitud(EstadoDispositivo.VINCULADO, true)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(DispositivoService.MENSAJE_SIN_CONSENTIMIENTO);
        verify(dispositivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Al vincular se entrega la credencial y solo se guarda su hash")
    void vinculaYEntregaCredencial() {
        when(consentimientoRepository.existsByUsuarioIdAndVigenteTrue(7)).thenReturn(true);
        when(dispositivoRepository.findByUsuarioId(7)).thenReturn(Optional.empty());

        DispositivoDTO respuesta = servicio.actualizar(CORREO,
                new DispositivoSolicitud(EstadoDispositivo.VINCULADO, false));

        assertThat(respuesta.estado()).isEqualTo(EstadoDispositivo.VINCULADO);
        assertThat(respuesta.hrvDisponible()).isFalse();   // sin HRV: detección parcial (HU003)
        assertThat(respuesta.tokenDispositivo()).hasSize(43);

        verify(dispositivoRepository).save(argThat(d ->
                d.getTokenHash().equals(DispositivoService.hash(respuesta.tokenDispositivo()))
                        && d.getTokenHash().length() == 64
                        && !d.getTokenHash().equals(respuesta.tokenDispositivo())));
    }

    @Test
    @DisplayName("Al desvincular se anula la credencial y no se devuelve token")
    void desvinculaYAnulaCredencial() {
        DispositivoVinculado existente = new DispositivoVinculado();
        existente.setUsuario(usuario);
        existente.setEstado(EstadoDispositivo.VINCULADO);
        existente.setTokenHash(DispositivoService.hash("credencial-vieja"));
        when(dispositivoRepository.findByUsuarioId(7)).thenReturn(Optional.of(existente));

        DispositivoDTO respuesta = servicio.actualizar(CORREO,
                new DispositivoSolicitud(EstadoDispositivo.DESCONECTADO, false));

        assertThat(respuesta.estado()).isEqualTo(EstadoDispositivo.DESCONECTADO);
        assertThat(respuesta.tokenDispositivo()).isNull();
        assertThat(existente.getTokenHash()).isNull();
    }

    @Test
    @DisplayName("Si nunca ha vinculado, el estado es Desconectado")
    void estadoInicialDesconectado() {
        when(dispositivoRepository.findByUsuarioId(7)).thenReturn(Optional.empty());

        DispositivoDTO respuesta = servicio.obtener(CORREO);

        assertThat(respuesta.estado()).isEqualTo(EstadoDispositivo.DESCONECTADO);
        assertThat(respuesta.tokenDispositivo()).isNull();
    }

    @Test
    @DisplayName("Cada vinculación genera una credencial distinta")
    void credencialesDistintas() {
        assertThat(DispositivoService.generarToken()).isNotEqualTo(DispositivoService.generarToken());
    }
}
