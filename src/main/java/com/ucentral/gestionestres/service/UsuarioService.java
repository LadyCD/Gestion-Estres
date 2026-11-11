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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** HU001: registro de usuarios con consentimiento informado (Ley 1581 de 2012). */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConsentimientoRepository consentimientoRepository;
    private final CargoRepository cargoRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final List<String> dominiosPermitidos;
    private final String versionConsentimiento;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ConsentimientoRepository consentimientoRepository,
                          CargoRepository cargoRepository,
                          AreaRepository areaRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.registro.dominios-permitidos}") String dominios,
                          @Value("${app.registro.version-consentimiento}") String versionConsentimiento) {
        this.usuarioRepository = usuarioRepository;
        this.consentimientoRepository = consentimientoRepository;
        this.cargoRepository = cargoRepository;
        this.areaRepository = areaRepository;
        this.passwordEncoder = passwordEncoder;
        this.dominiosPermitidos = Arrays.stream(dominios.split(","))
                .map(d -> d.trim().toLowerCase(Locale.ROOT))
                .filter(d -> !d.isEmpty())
                .toList();
        this.versionConsentimiento = versionConsentimiento;
    }

    /** Crea el usuario y su consentimiento vigente en una sola transacción. */
    @Transactional
    public UsuarioRespuesta registrar(RegistroSolicitud solicitud) {
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);

        validarDominio(correo);

        if (!versionConsentimiento.equals(solicitud.versionConsentimiento())) {
            throw new SolicitudInvalidaException(
                    "Debe aceptar la versión vigente del consentimiento informado");
        }
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }

        Cargo cargo = cargoRepository.findById(solicitud.cargoId())
                .orElseThrow(() -> new SolicitudInvalidaException("El cargo seleccionado no existe"));
        Area area = areaRepository.findById(solicitud.areaId())
                .orElseThrow(() -> new SolicitudInvalidaException("El área seleccionada no existe"));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(solicitud.nombreCompleto().trim());
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(passwordEncoder.encode(solicitud.contrasena()));
        usuario.setRol(Rol.TRABAJADOR);   // nunca viene del cliente
        usuario.setCargo(cargo);
        usuario.setArea(area);

        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo: gana la restricción UNIQUE
            throw new ReglaNegocioException("El correo ya está registrado");
        }

        Consentimiento consentimiento = new Consentimiento();
        consentimiento.setUsuario(usuario);
        consentimiento.setVersion(versionConsentimiento);
        consentimiento.setVigente(Boolean.TRUE);
        consentimientoRepository.save(consentimiento);

        return new UsuarioRespuesta(usuario.getId(), usuario.getNombreCompleto(),
                usuario.getCorreo(), usuario.getRol());
    }

    private void validarDominio(String correo) {
        String dominio = correo.substring(correo.lastIndexOf('@') + 1);
        if (!dominiosPermitidos.contains(dominio)) {
            throw new SolicitudInvalidaException(
                    "Use su correo institucional (" + String.join(", ", dominiosPermitidos) + ")");
        }
    }
}