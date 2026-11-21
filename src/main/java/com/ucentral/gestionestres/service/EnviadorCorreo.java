package com.ucentral.gestionestres.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * HU001: envía el código de verificación por correo (SMTP).
 * Si el servidor de correo no está configurado o falla, el registro no se cae:
 * el código se escribe en la consola del servidor para que la prueba pueda seguir.
 */
@Component
public class EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EnviadorCorreo.class);

    private final ObjectProvider<JavaMailSender> enviador;
    private final String servidor;
    private final String remitente;
    private final boolean codigoEnConsola;

    public EnviadorCorreo(ObjectProvider<JavaMailSender> enviador,
                          @Value("${spring.mail.host:}") String servidor,
                          @Value("${spring.mail.username:}") String remitente,
                          @Value("${app.verificacion.codigo-en-consola:false}") boolean codigoEnConsola) {
        this.enviador = enviador;
        this.servidor = servidor;
        this.remitente = remitente;
        this.codigoEnConsola = codigoEnConsola;
    }

    /** @return true si el correo salió por SMTP */
    public boolean enviarCodigo(String para, String nombre, String codigo, int minutosVigencia) {
        if (codigoEnConsola) {
            log.info("[HU001] Código de verificación para {}: {}", para, codigo);
        }
        JavaMailSender smtp = enviador.getIfAvailable();
        if (!StringUtils.hasText(servidor) || smtp == null) {
            if (!codigoEnConsola) {
                log.warn("[HU001] Correo no configurado (MAIL_HOST). Código para {}: {}", para, codigo);
            }
            return false;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(para);
            mensaje.setSubject("Gestión del Estrés: código de verificación");
            mensaje.setText("Hola " + nombre + ",\n\n"
                    + "Su código para verificar el correo es: " + codigo + "\n\n"
                    + "Vence en " + minutosVigencia + " minutos. Si usted no creó una cuenta, ignore este mensaje.\n\n"
                    + "Gestión del Estrés - Práctica de Ingeniería III, Universidad Central");
            smtp.send(mensaje);
            log.info("[HU001] Código de verificación enviado a {}", para);
            return true;
        } catch (Exception e) {
            log.warn("[HU001] No se pudo enviar el correo a {} ({}). Código: {}", para, e.getMessage(), codigo);
            return false;
        }
    }
}
