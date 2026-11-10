package com.ucentral.gestionestres.security;

import com.ucentral.gestionestres.model.Rol;
import com.ucentral.gestionestres.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtServicio {

    @Value("${app.seguridad.jwt-secreto:}")
    private String secreto;

    @Value("${app.seguridad.minutos-inactividad-sesion:15}")
    private long minutosExpiracion;

    private SecretKey clave;

    @PostConstruct
    void iniciar() {
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRETO no está definido o tiene menos de 32 caracteres");
        }
        clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public String generar(Usuario usuario) {
        return generar(usuario, minutosExpiracion);
    }

    // Visible para pruebas (token ya vencido con minutos negativos)
    String generar(Usuario usuario, long minutos) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(usuario.getCorreo())
                .claim("rol", usuario.getRol().name())
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + minutos * 60_000))
                .signWith(clave)
                .compact();
    }

    /** Devuelve los claims si el token es válido y no ha vencido; vacío en caso contrario. */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(clave).build()
                    .parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Rol rolDe(Claims claims) {
        return Rol.valueOf(claims.get("rol", String.class));
    }

    public long getMinutosExpiracion() { return minutosExpiracion; }
}