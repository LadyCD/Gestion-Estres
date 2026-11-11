package com.ucentral.gestionestres.exception;

import com.ucentral.gestionestres.model.RegistroAcceso;
import com.ucentral.gestionestres.model.ResultadoAcceso;
import com.ucentral.gestionestres.repository.RegistroAccesoRepository;
import com.ucentral.gestionestres.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Convierte las excepciones en respuestas JSON con un formato común. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private final UsuarioRepository usuarios;
    private final RegistroAccesoRepository accesos;

    public ManejadorErrores(UsuarioRepository usuarios, RegistroAccesoRepository accesos) {
        this.usuarios = usuarios;
        this.accesos = accesos;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> reglaNegocio(ReglaNegocioException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    /** Errores de @Valid en los DTO: devuelve qué campo falló y por qué. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud", campos);
    }

    /** JSON mal formado o con tipos incorrectos (por ejemplo, texto en un campo numérico). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no tiene un formato válido", null);
    }

    /** Solicitud con datos que incumplen una regla de entrada. */
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> solicitudInvalida(SolicitudInvalidaException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    /** HU026: credencial del dispositivo inválida o anulada. */
    @ExceptionHandler(TokenDispositivoInvalidoException.class)
    public ResponseEntity<ErrorRespuesta> tokenDispositivoInvalido(TokenDispositivoInvalidoException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    /** HU020: correo o contraseña incorrectos (mensaje genérico). */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorRespuesta> credencialesInvalidas(CredencialesInvalidasException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    /** HU020: cuenta bloqueada por intentos fallidos. */
    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<ErrorRespuesta> cuentaBloqueada(CuentaBloqueadaException ex) {
        return responder(HttpStatus.LOCKED, ex.getMessage(), null);
    }

    /** HU020: el rol del usuario no permite usar esta función. Se deja registro del rechazo. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> accesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        registrarRechazoPorRol(request);
        return responder(HttpStatus.FORBIDDEN, "No tiene permiso para usar esta función", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> general(Exception ex) {
        log.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intente de nuevo.", null);
    }

    private void registrarRechazoPorRol(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                return;
            }
            usuarios.findByCorreo(auth.getName()).ifPresent(usuario -> {
                RegistroAcceso r = new RegistroAcceso();
                r.setUsuario(usuario);
                r.setFuncion(request.getMethod() + " " + request.getRequestURI());
                r.setResultado(ResultadoAcceso.RECHAZADO);
                r.setFiltros("Rol " + usuario.getRol() + " sin permiso");
                accesos.save(r);
            });
        } catch (Exception e) {
            // Que un fallo al registrar no oculte el 403 real
            log.warn("No se pudo registrar el acceso rechazado", e);
        }
    }

    private ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje, Map<String, String> campos) {
        return ResponseEntity.status(estado)
                .body(new ErrorRespuesta(LocalDateTime.now(), estado.value(), mensaje, campos));
    }
}