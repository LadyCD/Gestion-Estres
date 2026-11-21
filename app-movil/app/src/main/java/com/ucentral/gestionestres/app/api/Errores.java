package com.ucentral.gestionestres.app.api;

import com.google.gson.Gson;

import retrofit2.Response;

/** Extrae el mensaje que envía el backend en ErrorRespuesta. */
public final class Errores {

    private Errores() { }

    public static String mensaje(Response<?> respuesta, String porDefecto) {
        try (okhttp3.ResponseBody cuerpo = respuesta.errorBody()) {
            if (cuerpo == null) {
                return porDefecto;
            }
            Dtos.ErrorRespuesta error = new Gson().fromJson(cuerpo.charStream(), Dtos.ErrorRespuesta.class);
            if (error == null) {
                return porDefecto;
            }
            if (error.campos != null && !error.campos.isEmpty()) {
                return String.join("\n", error.campos.values());
            }
            return error.mensaje != null ? error.mensaje : porDefecto;
        } catch (Exception e) {
            return porDefecto;
        }
    }
}
