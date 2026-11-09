package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.DispositivoDTO;
import com.ucentral.gestionestres.dto.DispositivoSolicitud;
import com.ucentral.gestionestres.exception.RecursoNoEncontradoException;
import com.ucentral.gestionestres.exception.ReglaNegocioException;
import com.ucentral.gestionestres.model.DispositivoVinculado;
import com.ucentral.gestionestres.model.EstadoDispositivo;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.ConsentimientoRepository;
import com.ucentral.gestionestres.repository.DispositivoVinculadoRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * HU002: vincular y desvincular el wearable (Health Connect).
 * Al vincular se emite la credencial del dispositivo que usa la sincronización (HU026).
 */
@Service
@Transactional
public class DispositivoService {

    static final String MENSAJE_SIN_CONSENTIMIENTO =
            "Debe aceptar el consentimiento informado para vincular un dispositivo";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final DispositivoVinculadoRepository dispositivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConsentimientoRepository consentimientoRepository;

    public DispositivoService(DispositivoVinculadoRepository dispositivoRepository,
                              UsuarioRepository usuarioRepository,
                              ConsentimientoRepository consentimientoRepository) {
        this.dispositivoRepository = dispositivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.consentimientoRepository = consentimientoRepository;
    }

    /** GET /api/dispositivo. Si el usuario nunca ha vinculado, responde Desconectado. */
    @Transactional(readOnly = true)
    public DispositivoDTO obtener(String correo) {
        Usuario usuario = buscarUsuario(correo);
        return dispositivoRepository.findByUsuarioId(usuario.getId())
                .map(d -> aDTO(d, null))
                .orElse(new DispositivoDTO(EstadoDispositivo.DESCONECTADO, false, null, null));
    }

    /** PUT /api/dispositivo. */
    public DispositivoDTO actualizar(String correo, DispositivoSolicitud solicitud) {
        Usuario usuario = buscarUsuario(correo);
        return solicitud.estado() == EstadoDispositivo.VINCULADO
                ? vincular(usuario, solicitud.hrvDisponible())
                : desvincular(usuario);
    }

    private DispositivoDTO vincular(Usuario usuario, boolean hrvDisponible) {
        // La vinculación solo se habilita con un consentimiento vigente (HU001)
        if (!consentimientoRepository.existsByUsuarioIdAndVigenteTrue(usuario.getId())) {
            throw new ReglaNegocioException(MENSAJE_SIN_CONSENTIMIENTO);
        }
        DispositivoVinculado dispositivo = dispositivoRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> {
                    DispositivoVinculado nuevo = new DispositivoVinculado();
                    nuevo.setUsuario(usuario);
                    return nuevo;
                });

        // Cada vinculación emite una credencial nueva; la anterior deja de servir
        String token = generarToken();
        dispositivo.setEstado(EstadoDispositivo.VINCULADO);
        dispositivo.setHrvDisponible(hrvDisponible);
        dispositivo.setTokenHash(hash(token));
        dispositivoRepository.save(dispositivo);
        return aDTO(dispositivo, token);
    }

    /** Desvincular: se anula la credencial; los indicadores ya calculados se conservan. */
    private DispositivoDTO desvincular(Usuario usuario) {
        Optional<DispositivoVinculado> existente = dispositivoRepository.findByUsuarioId(usuario.getId());
        if (existente.isEmpty()) {
            return new DispositivoDTO(EstadoDispositivo.DESCONECTADO, false, null, null);
        }
        DispositivoVinculado dispositivo = existente.get();
        dispositivo.setEstado(EstadoDispositivo.DESCONECTADO);
        dispositivo.setTokenHash(null);
        dispositivoRepository.save(dispositivo);
        return aDTO(dispositivo, null);
    }

    /**
     * HU026: devuelve el dispositivo vinculado al que pertenece la credencial enviada por la app
     * en el encabezado X-Token-Dispositivo. Vacío si la credencial no existe o fue anulada.
     */
    @Transactional(readOnly = true)
    public Optional<DispositivoVinculado> buscarPorToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return dispositivoRepository.findByTokenHashAndEstado(hash(token), EstadoDispositivo.VINCULADO);
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private static DispositivoDTO aDTO(DispositivoVinculado d, String token) {
        return new DispositivoDTO(d.getEstado(), d.getHrvDisponible(), d.getUltimaSincronizacion(), token);
    }

    /** 32 bytes aleatorios en Base64 URL (43 caracteres). */
    static String generarToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal (64 caracteres), que es lo único que se guarda. */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
