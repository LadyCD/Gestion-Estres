package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.ChequeoDiarioDTO;
import com.ucentral.gestionestres.model.Cuestionario;
import com.ucentral.gestionestres.model.TipoCuestionario;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.CuestionarioRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChequeoDiarioService {

    private final CuestionarioRepository cuestionarioRepository;
    private final UsuarioRepository usuarioRepository;

    public ChequeoDiarioService(CuestionarioRepository cuestionarioRepository, UsuarioRepository usuarioRepository) {
        this.cuestionarioRepository = cuestionarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Cuestionario registrarChequeo(ChequeoDiarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + dto.usuarioId()));

        Cuestionario chequeo = new Cuestionario();
        chequeo.setUsuario(usuario);
        chequeo.setTipo(TipoCuestionario.DIARIO);
        chequeo.setPuntaje(dto.puntaje());
        chequeo.setEnergia(dto.energia());
        chequeo.setTension(dto.tension());

        return cuestionarioRepository.save(chequeo);
    }
}