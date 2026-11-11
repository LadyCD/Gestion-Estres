package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.ChequeoDiarioDTO;
import com.ucentral.gestionestres.dto.ChequeoDiarioRespuesta;
import com.ucentral.gestionestres.exception.RecursoNoEncontradoException;
import com.ucentral.gestionestres.model.Cuestionario;
import com.ucentral.gestionestres.model.TipoCuestionario;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.CuestionarioRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU021 en la capa de servicio. */
@ExtendWith(MockitoExtension.class)
class ChequeoDiarioServiceTest {

    @Mock private CuestionarioRepository cuestionarioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private ChequeoDiarioService servicio;

    @Test
    @DisplayName("Guarda el chequeo como DIARIO para el usuario de la sesión")
    void guardaChequeoDelUsuarioDeLaSesion() {
        Usuario usuario = new Usuario();
        when(usuarioRepository.findByCorreo("ana@ucentral.edu.co")).thenReturn(Optional.of(usuario));
        when(cuestionarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ChequeoDiarioRespuesta r = servicio.registrarChequeo("ana@ucentral.edu.co",
                new ChequeoDiarioDTO(7, 2, true));

        assertThat(r.estres()).isEqualTo(7);
        assertThat(r.energia()).isEqualTo(2);
        assertThat(r.tension()).isTrue();
        verify(cuestionarioRepository).save(org.mockito.ArgumentMatchers.<Cuestionario>argThat(c ->
                c.getUsuario() == usuario && c.getTipo() == TipoCuestionario.DIARIO && c.getPuntaje() == 7));
    }

    @Test
    @DisplayName("Si el usuario de la sesión no existe, responde 404 y no guarda")
    void usuarioInexistente() {
        when(usuarioRepository.findByCorreo("nadie@ucentral.edu.co")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrarChequeo("nadie@ucentral.edu.co",
                new ChequeoDiarioDTO(3, 3, false)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(cuestionarioRepository, never()).save(any());
    }
}
