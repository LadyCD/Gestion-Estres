package com.ucentral.gestionestres.service;

import com.ucentral.gestionestres.dto.LoginRespuesta;
import com.ucentral.gestionestres.dto.LoginSolicitud;
import com.ucentral.gestionestres.exception.CredencialesInvalidasException;
import com.ucentral.gestionestres.exception.CuentaBloqueadaException;
import com.ucentral.gestionestres.model.RegistroAcceso;
import com.ucentral.gestionestres.model.ResultadoAcceso;
import com.ucentral.gestionestres.model.Usuario;
import com.ucentral.gestionestres.repository.RegistroAccesoRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import com.ucentral.gestionestres.security.JwtServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthServicio {

    private final UsuarioRepository usuarios;
    private final RegistroAccesoRepository accesos;
    private final PasswordEncoder encoder;
    private final JwtServicio jwt;
    private final int intentosMaximos;
    private final int minutosBloqueo;

    public AuthServicio(UsuarioRepository usuarios,
                        RegistroAccesoRepository accesos,
                        PasswordEncoder encoder,
                        JwtServicio jwt,
                        @Value("${app.seguridad.intentos-maximos:5}") int intentosMaximos,
                        @Value("${app.seguridad.minutos-bloqueo:15}") int minutosBloqueo) {
        this.usuarios = usuarios;
        this.accesos = accesos;
        this.encoder = encoder;
        this.jwt = jwt;
        this.intentosMaximos = intentosMaximos;
        this.minutosBloqueo = minutosBloqueo;
    }

    // noRollbackFor: el contador y el bloqueo deben guardarse aunque lancemos 401/423
    @Transactional(noRollbackFor = {CredencialesInvalidasException.class, CuentaBloqueadaException.class})
    public LoginRespuesta login(LoginSolicitud solicitud) {
        // Correo inexistente: mismo 401 que clave mala, sin registro (usuario_id es obligatorio)
        Usuario usuario = usuarios.findByCorreo(solicitud.correo())
                .orElseThrow(CredencialesInvalidasException::new);

        LocalDateTime ahora = LocalDateTime.now();

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
            registrarRechazo(usuario, "Cuenta bloqueada");
            throw new CuentaBloqueadaException(minutosRestantes(ahora, usuario.getBloqueadoHasta()));
        }

        if (!encoder.matches(solicitud.contrasena(), usuario.getContrasenaHash())) {
            int intentos = usuario.getIntentosFallidos() + 1;
            registrarRechazo(usuario, "Credenciales inválidas");

            if (intentos >= intentosMaximos) {
                usuario.setBloqueadoHasta(ahora.plusMinutes(minutosBloqueo));
                usuario.setIntentosFallidos(0);
                usuarios.save(usuario);
                throw new CuentaBloqueadaException(minutosBloqueo);
            }
            usuario.setIntentosFallidos(intentos);
            usuarios.save(usuario);
            throw new CredencialesInvalidasException();
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarios.save(usuario);

        return new LoginRespuesta(jwt.generar(usuario), usuario.getRol(),
                usuario.getNombreCompleto(), jwt.getMinutosExpiracion());
    }

    private void registrarRechazo(Usuario usuario, String motivo) {
        RegistroAcceso r = new RegistroAcceso();
        r.setUsuario(usuario);
        r.setFuncion("LOGIN");
        r.setResultado(ResultadoAcceso.RECHAZADO);
        r.setFiltros(motivo);
        accesos.save(r);
    }

    private long minutosRestantes(LocalDateTime ahora, LocalDateTime hasta) {
        return Math.max(1, Duration.between(ahora, hasta).toMinutes() + 1);
    }
}