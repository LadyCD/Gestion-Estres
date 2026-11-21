package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.exception.SolicitudInvalidaException;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** HU001: verificación del correo con código de 6 dígitos. */
class VerificacionCorreoServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 13, 8, 0);

    private UsuarioRepository usuarioRepository;
    private EnviadorCorreo enviadorCorreo;
    private VerificacionCorreoService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        enviadorCorreo = mock(EnviadorCorreo.class);
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        service = new VerificacionCorreoService(usuarioRepository, enviadorCorreo, reloj, 15, 5);

        usuario = new Usuario();
        usuario.setNombreCompleto("Ana Pérez");
        usuario.setCorreo("ana@ucentral.edu.co");
        when(usuarioRepository.findByCorreo("ana@ucentral.edu.co")).thenReturn(Optional.of(usuario));
    }

    /** Emite un código y devuelve el que se "envió" por correo. */
    private String emitir() {
        service.emitirCodigo(usuario);
        return ultimoCodigoEnviado();
    }

    private String ultimoCodigoEnviado() {
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(enviadorCorreo, atLeastOnce()).enviarCodigo(eq("ana@ucentral.edu.co"), anyString(), codigo.capture(), eq(15));
        return codigo.getValue();
    }

    @Test
    void emitirGuardaSoloElHashConVencimientoDe15Minutos() {
        String codigo = emitir();

        assertTrue(codigo.matches("\\d{6}"));
        assertNotNull(usuario.getCodigoVerificacionHash());
        assertNotEquals(codigo, usuario.getCodigoVerificacionHash());
        assertEquals(64, usuario.getCodigoVerificacionHash().length());
        assertEquals(AHORA.plusMinutes(15), usuario.getCodigoExpira());
        assertFalse(usuario.getCorreoVerificado());
    }

    @Test
    void codigoCorrectoVerificaLaCuentaYBorraElCodigo() {
        String codigo = emitir();

        service.verificar("Ana@ucentral.edu.co", codigo);

        assertTrue(usuario.getCorreoVerificado());
        assertNull(usuario.getCodigoVerificacionHash());
        assertNull(usuario.getCodigoExpira());
    }

    @Test
    void codigoIncorrectoSumaUnIntentoYNoVerifica() {
        String codigo = emitir();
        String otro = codigo.equals("000000") ? "111111" : "000000";

        assertThrows(SolicitudInvalidaException.class, () -> service.verificar("ana@ucentral.edu.co", otro));

        assertEquals(1, usuario.getIntentosCodigo());
        assertFalse(usuario.getCorreoVerificado());
    }

    @Test
    void despuesDe5IntentosSeRechazaAunqueElCodigoSeaCorrecto() {
        String codigo = emitir();
        usuario.setIntentosCodigo(5);

        assertThrows(SolicitudInvalidaException.class, () -> service.verificar("ana@ucentral.edu.co", codigo));
        assertFalse(usuario.getCorreoVerificado());
    }

    @Test
    void codigoVencidoSeRechaza() {
        String codigo = emitir();
        usuario.setCodigoExpira(AHORA.minusMinutes(1));

        assertThrows(SolicitudInvalidaException.class, () -> service.verificar("ana@ucentral.edu.co", codigo));
        assertFalse(usuario.getCorreoVerificado());
    }

    @Test
    void reenviarGeneraUnCodigoNuevoYElAnteriorDejaDeServir() {
        String primero = emitir();
        String hashPrimero = usuario.getCodigoVerificacionHash();

        service.reenviar("ana@ucentral.edu.co");
        String segundo = ultimoCodigoEnviado();

        assertNotEquals(hashPrimero, usuario.getCodigoVerificacionHash());
        if (!primero.equals(segundo)) {   // 1 en un millón de que coincidan
            assertThrows(SolicitudInvalidaException.class,
                    () -> service.verificar("ana@ucentral.edu.co", primero));
        }
        service.verificar("ana@ucentral.edu.co", segundo);
        assertTrue(usuario.getCorreoVerificado());
    }

    @Test
    void reenviarAUnCorreoQueNoExisteNoEnviaNadaNiFalla() {
        when(usuarioRepository.findByCorreo("nadie@ucentral.edu.co")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.reenviar("nadie@ucentral.edu.co"));
        verify(enviadorCorreo, never()).enviarCodigo(anyString(), anyString(), anyString(), anyInt());
    }
}
