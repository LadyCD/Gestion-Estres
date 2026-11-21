package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.LoginSolicitud;
import com.ucentral.gestionestres.exception.CorreoNoVerificadoException;
import com.ucentral.gestionestres.exception.CredencialesInvalidasException;
import com.ucentral.gestionestres.model.RegistroAcceso;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.RegistroAccesoRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import com.ucentral.gestionestres.security.JwtServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU001 + HU020: no se inicia sesión con el correo sin verificar. */
class AuthServicioTest {

    private UsuarioRepository usuarios;
    private RegistroAccesoRepository accesos;
    private JwtServicio jwt;
    private AuthServicio servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        accesos = mock(RegistroAccesoRepository.class);
        jwt = mock(JwtServicio.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        servicio = new AuthServicio(usuarios, accesos, encoder, jwt, 5, 15);

        usuario = new Usuario();
        usuario.setNombreCompleto("Ana Pérez");
        usuario.setCorreo("ana@ucentral.edu.co");
        usuario.setContrasenaHash(encoder.encode("ClaveSegura1"));
        when(usuarios.findByCorreo("ana@ucentral.edu.co")).thenReturn(Optional.of(usuario));
    }

    @Test
    void correoSinVerificarNoRecibeTokenYQuedaEnElRegistroDeAccesos() {
        usuario.setCorreoVerificado(false);

        assertThrows(CorreoNoVerificadoException.class,
                () -> servicio.login(new LoginSolicitud("ana@ucentral.edu.co", "ClaveSegura1")));

        verify(jwt, never()).generar(any());
        ArgumentCaptor<RegistroAcceso> r = ArgumentCaptor.forClass(RegistroAcceso.class);
        verify(accesos).save(r.capture());
        assertEquals("Correo no verificado", r.getValue().getFiltros());
    }

    @Test
    void conContrasenaIncorrectaNoRevelaQueFaltaVerificar() {
        usuario.setCorreoVerificado(false);

        assertThrows(CredencialesInvalidasException.class,
                () -> servicio.login(new LoginSolicitud("ana@ucentral.edu.co", "OtraClave9")));
    }

    @Test
    void correoVerificadoIniciaSesion() {
        usuario.setCorreoVerificado(true);
        when(jwt.generar(usuario)).thenReturn("token");

        assertEquals("token", servicio.login(new LoginSolicitud("ana@ucentral.edu.co", "ClaveSegura1")).token());
    }
}
