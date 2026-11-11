package com.ucentral.gestionestres.app.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/** Endpoints del backend que usa la app. Ver el contrato en app-movil/README.md. */
public interface ApiServicio {

    // HU001
    @GET("api/catalogos/areas")
    Call<List<Dtos.Catalogo>> areas();

    @GET("api/catalogos/cargos")
    Call<List<Dtos.Catalogo>> cargos();

    @POST("api/usuarios")
    Call<Dtos.Usuario> registrar(@Body Dtos.RegistroSolicitud solicitud);

    // HU020
    @POST("api/auth/login")
    Call<Dtos.LoginRespuesta> login(@Body Dtos.LoginSolicitud solicitud);

    @POST("api/auth/logout")
    Call<Void> logout();

    // HU021
    @POST("api/cuestionarios/diario")
    Call<Void> enviarChequeo(@Body Dtos.ChequeoSolicitud solicitud);

    // HU002
    @GET("api/dispositivo")
    Call<Dtos.Dispositivo> dispositivo();

    @PUT("api/dispositivo")
    Call<Dtos.Dispositivo> actualizarDispositivo(@Body Dtos.DispositivoSolicitud solicitud);

    // HU026: usa la credencial del dispositivo, no la sesión del usuario
    @POST("api/ventanas")
    Call<Void> enviarVentana(@Header("X-Token-Dispositivo") String tokenDispositivo,
                             @Body Dtos.VentanaSolicitud ventana);

    // HU025 y HU003
    @GET("api/linea-base")
    Call<Dtos.LineaBase> lineaBase();

    @GET("api/detecciones/ultima")
    Call<Dtos.Deteccion> ultimaDeteccion();

    // HU003: clasifica ya con los datos de la última hora (para no esperar la evaluación horaria)
    @POST("api/detecciones/evaluar")
    Call<Dtos.Deteccion> evaluarAhora();
}
