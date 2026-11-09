package com.ucentral.gestionestres.app.sesion;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Instant;

/**
 * Guarda en el teléfono la sesión del usuario (HU020) y la credencial del
 * dispositivo vinculado (HU002, HU026).
 */
public class Sesion {

    /** HU020: la sesión vence tras 15 minutos de inactividad. */
    public static final long INACTIVIDAD_MAXIMA_MS = 15 * 60 * 1000L;

    private static final String ARCHIVO = "gestion_estres";
    private static final String TOKEN = "token";
    private static final String ROL = "rol";
    private static final String NOMBRE = "nombre";
    private static final String ULTIMA_ACTIVIDAD = "ultima_actividad";
    private static final String TOKEN_DISPOSITIVO = "token_dispositivo";
    private static final String FIN_ULTIMA_VENTANA = "fin_ultima_ventana";

    private final SharedPreferences preferencias;

    public Sesion(Context contexto) {
        preferencias = contexto.getApplicationContext()
                .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    // ---------------- Sesión del usuario (HU020) ----------------

    public void iniciar(String token, String rol, String nombre) {
        preferencias.edit()
                .putString(TOKEN, token)
                .putString(ROL, rol)
                .putString(NOMBRE, nombre)
                .putLong(ULTIMA_ACTIVIDAD, System.currentTimeMillis())
                .apply();
    }

    public boolean haySesion() {
        return token() != null;
    }

    public String token() {
        return preferencias.getString(TOKEN, null);
    }

    public String rol() {
        return preferencias.getString(ROL, null);
    }

    public String nombre() {
        return preferencias.getString(NOMBRE, "");
    }

    public boolean esTrabajador() {
        return "TRABAJADOR".equals(rol());
    }

    /** Se llama cada vez que el usuario interactúa con la app. */
    public void registrarActividad() {
        preferencias.edit().putLong(ULTIMA_ACTIVIDAD, System.currentTimeMillis()).apply();
    }

    public boolean vencioPorInactividad() {
        long ultima = preferencias.getLong(ULTIMA_ACTIVIDAD, 0);
        return System.currentTimeMillis() - ultima > INACTIVIDAD_MAXIMA_MS;
    }

    /** Cierra la sesión del usuario. El dispositivo sigue vinculado hasta que se desvincule (HU002). */
    public void cerrar() {
        preferencias.edit().remove(TOKEN).remove(ROL).remove(NOMBRE).remove(ULTIMA_ACTIVIDAD).apply();
    }

    // ---------------- Dispositivo vinculado (HU002, HU026) ----------------

    public void guardarTokenDispositivo(String token) {
        preferencias.edit().putString(TOKEN_DISPOSITIVO, token).apply();
    }

    public String tokenDispositivo() {
        return preferencias.getString(TOKEN_DISPOSITIVO, null);
    }

    public Instant finUltimaVentana() {
        long ms = preferencias.getLong(FIN_ULTIMA_VENTANA, 0);
        return ms == 0 ? null : Instant.ofEpochMilli(ms);
    }

    public void guardarFinUltimaVentana(Instant fin) {
        preferencias.edit().putLong(FIN_ULTIMA_VENTANA, fin.toEpochMilli()).apply();
    }

    public void olvidarDispositivo() {
        preferencias.edit().remove(TOKEN_DISPOSITIVO).remove(FIN_ULTIMA_VENTANA).apply();
    }
}
