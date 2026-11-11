package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.ChequeoDiarioDTO;
import com.ucentral.gestionestres.dto.ChequeoDiarioRespuesta;
import com.ucentral.gestionestres.exception.RecursoNoEncontradoException;
import com.ucentral.gestionestres.model.Cuestionario;
import com.ucentral.gestionestres.model.TipoCuestionario;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.CuestionarioRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU021: guarda el chequeo diario (estrés 0-10, energía 1-5, tensión Sí/No). */
@Service
public class ChequeoDiarioService {

    private final CuestionarioRepository cuestionarioRepository;
    private final UsuarioRepository usuarioRepository;

    public ChequeoDiarioService(CuestionarioRepository cuestionarioRepository, UsuarioRepository usuarioRepository) {
        this.cuestionarioRepository = cuestionarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** @param correo usuario de la sesión (Authentication.getName()) */
    @Transactional
    public ChequeoDiarioRespuesta registrarChequeo(String correo, ChequeoDiarioDTO dto) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Cuestionario chequeo = new Cuestionario();
        chequeo.setUsuario(usuario);
        chequeo.setTipo(TipoCuestionario.DIARIO);
        chequeo.setPuntaje(dto.estres());     // en DIARIO el puntaje es la pregunta de estrés
        chequeo.setEnergia(dto.energia());
        chequeo.setTension(dto.tension());

        Cuestionario guardado = cuestionarioRepository.save(chequeo);
        return new ChequeoDiarioRespuesta(guardado.getId(), guardado.getPuntaje(),
                guardado.getEnergia(), guardado.getTension());
    }
}
