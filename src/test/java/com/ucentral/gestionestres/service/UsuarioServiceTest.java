package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.RegistroSolicitud;
import com.ucentral.gestionestres.dto.UsuarioRespuesta;
import com.ucentral.gestionestres.exception.ReglaNegocioException;
import com.ucentral.gestionestres.exception.SolicitudInvalidaException;
import com.ucentral.gestionestres.model.Area;
import com.ucentral.gestionestres.model.Cargo;
import com.ucentral.gestionestres.model.Consentimiento;
import com.ucentral.gestionestres.model.Rol;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.AreaRepository;
import com.ucentral.gestionestres.repository.CargoRepository;
import com.ucentral.gestionestres.repository.ConsentimientoRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private ConsentimientoRepository consentimientoRepository;
    private CargoRepository cargoRepository;
    private AreaRepository areaRepository;
    private PasswordEncoder passwordEncoder;
    private VerificacionCorreoService verificacionCorreo;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        consentimientoRepository = mock(ConsentimientoRepository.class);
        cargoRepository = mock(CargoRepository.class);
        areaRepository = mock(AreaRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        verificacionCorreo = mock(VerificacionCorreoService.class);
        service = new UsuarioService(usuarioRepository, consentimientoRepository,
                cargoRepository, areaRepository, passwordEncoder, verificacionCorreo, "ucentral.edu.co", "1.0");

        when(cargoRepository.findById(1)).thenReturn(Optional.of(mock(Cargo.class)));
        when(areaRepository.findById(1)).thenReturn(Optional.of(mock(Area.class)));
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private RegistroSolicitud solicitud(String correo, String version) {
        return new RegistroSolicitud("Ana Pérez", correo, "ClaveSegura1", 1, 1, version);
    }

    @Test
    void registroCorrectoGuardaHashRolTrabajadorYConsentimientoVigente() {
        UsuarioRespuesta r = service.registrar(solicitud("Ana@ucentral.edu.co", "1.0"));

        assertEquals("ana@ucentral.edu.co", r.correo());
        assertEquals(Rol.TRABAJADOR, r.rol());

        ArgumentCaptor<Usuario> u = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(u.capture());
        assertNotEquals("ClaveSegura1", u.getValue().getContrasenaHash());
        assertTrue(passwordEncoder.matches("ClaveSegura1", u.getValue().getContrasenaHash()));

        ArgumentCaptor<Consentimiento> c = ArgumentCaptor.forClass(Consentimiento.class);
        verify(consentimientoRepository).save(c.capture());
        assertTrue(c.getValue().getVigente());
        assertEquals("1.0", c.getValue().getVersion());

        // La cuenta nace sin verificar y se envía el código al correo
        assertFalse(u.getValue().getCorreoVerificado());
        verify(verificacionCorreo).emitirCodigo(u.getValue());
    }

    @Test
    void correoDuplicadoLanzaReglaNegocio() {
        when(usuarioRepository.existsByCorreo("ana@ucentral.edu.co")).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> service.registrar(solicitud("ana@ucentral.edu.co", "1.0")));
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(verificacionCorreo, never()).emitirCodigo(any());
    }

    @Test
    void dominioNoPermitidoLanzaSolicitudInvalida() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.registrar(solicitud("ana@gmail.com", "1.0")));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void versionDeConsentimientoDistintaALaVigenteSeRechaza() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.registrar(solicitud("ana@ucentral.edu.co", "0.9")));
        verify(consentimientoRepository, never()).save(any());
    }

    @Test
    void cargoInexistenteSeRechaza() {
        when(cargoRepository.findById(99)).thenReturn(Optional.empty());
        RegistroSolicitud s = new RegistroSolicitud("Ana", "ana@ucentral.edu.co",
                "ClaveSegura1", 99, 1, "1.0");

        assertThrows(SolicitudInvalidaException.class, () -> service.registrar(s));
    }
}