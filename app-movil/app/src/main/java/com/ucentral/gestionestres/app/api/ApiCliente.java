package com.ucentral.gestionestres.app.api;

import android.content.Context;

import com.ucentral.gestionestres.app.BuildConfig;
import com.ucentral.gestionestres.app.sesion.Sesion;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Crea una sola instancia de Retrofit y agrega el token de la sesión a cada petición. */
public final class ApiCliente {

    private static volatile ApiServicio servicio;

    private ApiCliente() { }

    public static ApiServicio servicio(Context contexto) {
        if (servicio == null) {
            synchronized (ApiCliente.class) {
                if (servicio == null) {
                    servicio = crear(contexto.getApplicationContext());
                }
            }
        }
        return servicio;
    }

    private static ApiServicio crear(Context contexto) {
        Sesion sesion = new Sesion(contexto);

        HttpLoggingInterceptor registro = new HttpLoggingInterceptor();
        registro.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(cadena -> {
                    Request original = cadena.request();
                    String token = sesion.token();
                    if (token == null || original.header("Authorization") != null) {
                        return cadena.proceed(original);
                    }
                    return cadena.proceed(original.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build());
                })
                .addInterceptor(registro)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiServicio.class);
    }
}
