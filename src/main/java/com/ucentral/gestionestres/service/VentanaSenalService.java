package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.VentanaSenalDTO;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.model.VentanaSenal;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import com.ucentral.gestionestres.repository.VentanaSenalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VentanaSenalService {

    private final VentanaSenalRepository ventanaSenalRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.sincronizacion.duracion-minutos:15}")
    private int duracionVentanaMinutos;

    public VentanaSenalService(VentanaSenalRepository ventanaSenalRepository, UsuarioRepository usuarioRepository) {
        this.ventanaSenalRepository = ventanaSenalRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public VentanaSenal registrarVentana(VentanaSenalDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + dto.usuarioId()));

        VentanaSenal ventana = new VentanaSenal();
        ventana.setUsuario(usuario);
        ventana.setInicio(dto.inicio());
        ventana.setFin(dto.fin());
        ventana.setFcPromedio(dto.fcPromedio());
        ventana.setHrvPromedio(dto.hrvPromedio());
        ventana.setNumLecturas(dto.numLecturas());

        return ventanaSenalRepository.save(ventana);
    }
}
