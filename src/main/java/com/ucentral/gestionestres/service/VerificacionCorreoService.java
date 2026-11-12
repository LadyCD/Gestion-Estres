package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.exception.SolicitudInvalidaException;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * HU001: comprueba que el correo exista de verdad. Al registrarse se envía un código
 * de 6 dígitos; la cuenta solo puede iniciar sesión después de escribirlo en la app.
 * Se guarda únicamente el hash del código, con vencimiento y número máximo de intentos.
 */
@Service
public class VerificacionCorreoService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String CODIGO_INVALIDO = "El código no es válido. Revíselo o solicite uno nuevo.";

    private final UsuarioRepository usuarioRepository;
    private final EnviadorCorreo enviadorCorreo;
    private final Clock reloj;
    private final int minutosVigencia;
    private final int intentosMaximos;

    public VerificacionCorreoService(UsuarioRepository usuarioRepository,
                                     EnviadorCorreo enviadorCorreo,
                                     Clock reloj,
                                     @Value("${app.verificacion.minutos-vigencia:15}") int minutosVigencia,
                                     @Value("${app.verificacion.intentos-maximos:5}") int intentosMaximos) {
        this.usuarioRepository = usuarioRepository;
        this.enviadorCorreo = enviadorCorreo;
        this.reloj = reloj;
        this.minutosVigencia = minutosVigencia;
        this.intentosMaximos = intentosMaximos;
    }

    /** Genera un código nuevo (reemplaza el anterior), lo guarda como hash y lo envía. */
    @Transactional
    public void emitirCodigo(Usuario usuario) {
        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        usuario.setCodigoVerificacionHash(hash(usuario.getCorreo(), codigo));
        usuario.setCodigoExpira(LocalDateTime.now(reloj).plusMinutes(minutosVigencia));
        usuario.setIntentosCodigo(0);
        usuarioRepository.save(usuario);
        enviadorCorreo.enviarCodigo(usuario.getCorreo(), usuario.getNombreCompleto(), codigo, minutosVigencia);
    }

    /** Valida el código. Si es correcto, la cuenta queda verificada. */
    @Transactional(noRollbackFor = SolicitudInvalidaException.class)
    public void verificar(String correo, String codigo) {
        Usuario usuario = buscar(correo)
                .orElseThrow(() -> new SolicitudInvalidaException(CODIGO_INVALIDO));

        if (Boolean.TRUE.equals(usuario.getCorreoVerificado())) {
            return;   // ya verificado: no es un error repetir la verificación
        }
        if (usuario.getCodigoVerificacionHash() == null) {
            throw new SolicitudInvalidaException(CODIGO_INVALIDO);
        }
        if (usuario.getIntentosCodigo() >= intentosMaximos) {
            throw new SolicitudInvalidaException(
                    "Superó los " + intentosMaximos + " intentos. Solicite un código nuevo.");
        }
        if (usuario.getCodigoExpira() == null || LocalDateTime.now(reloj).isAfter(usuario.getCodigoExpira())) {
            throw new SolicitudInvalidaException("El código venció. Solicite uno nuevo.");
        }

        boolean coincide = MessageDigest.isEqual(
                usuario.getCodigoVerificacionHash().getBytes(StandardCharsets.US_ASCII),
                hash(usuario.getCorreo(), codigo.trim()).getBytes(StandardCharsets.US_ASCII));
        if (!coincide) {
            usuario.setIntentosCodigo(usuario.getIntentosCodigo() + 1);
            usuarioRepository.save(usuario);
            throw new SolicitudInvalidaException(CODIGO_INVALIDO);
        }

        usuario.setCorreoVerificado(Boolean.TRUE);
        usuario.setCodigoVerificacionHash(null);
        usuario.setCodigoExpira(null);
        usuario.setIntentosCodigo(0);
        usuarioRepository.save(usuario);
    }

    /**
     * Envía un código nuevo. Responde igual exista o no la cuenta, para no revelar
     * qué correos están registrados.
     */
    @Transactional
    public void reenviar(String correo) {
        buscar(correo)
                .filter(u -> !Boolean.TRUE.equals(u.getCorreoVerificado()))
                .ifPresent(this::emitirCodigo);
    }

    private Optional<Usuario> buscar(String correo) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase(Locale.ROOT));
    }

    /** SHA-256 del correo y el código: el mismo código de dos usuarios da hashes distintos. */
    static String hash(String correo, String codigo) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((correo + ":" + codigo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
