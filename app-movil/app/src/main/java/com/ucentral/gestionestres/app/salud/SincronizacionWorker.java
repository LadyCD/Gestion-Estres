package com.ucentral.gestionestres.app.salud;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ucentral.gestionestres.app.api.ApiCliente;
import com.ucentral.gestionestres.app.api.Dtos;
import com.ucentral.gestionestres.app.api.Fechas;
import com.ucentral.gestionestres.app.sesion.Sesion;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import retrofit2.Response;

/**
 * HU026: cada 15 minutos, aun con la app cerrada, lee de Health Connect las ventanas
 * de 15 minutos que faltan por enviar y manda sus promedios al backend.
 *
 * Si no hay conexión, las ventanas no enviadas se recuperan en la siguiente ejecución
 * (se guarda el fin de la última ventana enviada). El backend rechaza con 409 una
 * ventana repetida, así que reenviar no crea duplicados.
 */
public class SincronizacionWorker extends Worker {

    private static final String TAG = "Sincronizacion";
    private static final String NOMBRE_TRABAJO = "sincronizacion-senales";
    private static final Duration TAMANO_VENTANA = Duration.ofMinutes(15);
    /** El servidor conserva las ventanas 24 horas; no tiene sentido recuperar más atrás. */
    private static final Duration MAXIMO_ATRASO = Duration.ofHours(24);

    public SincronizacionWorker(@NonNull Context contexto, @NonNull WorkerParameters parametros) {
        super(contexto, parametros);
    }

    /** Se llama al vincular el dispositivo (HU002). */
    public static void programar(Context contexto) {
        PeriodicWorkRequest trabajo = new PeriodicWorkRequest.Builder(
                SincronizacionWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        WorkManager.getInstance(contexto)
                .enqueueUniquePeriodicWork(NOMBRE_TRABAJO, ExistingPeriodicWorkPolicy.KEEP, trabajo);
    }

    /** Se llama al desvincular (HU002): se detienen la lectura y el envío. */
    public static void cancelar(Context contexto) {
        WorkManager.getInstance(contexto).cancelUniqueWork(NOMBRE_TRABAJO);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context contexto = getApplicationContext();
        Sesion sesion = new Sesion(contexto);
        String tokenDispositivo = sesion.tokenDispositivo();

        if (tokenDispositivo == null || !SaludConexion.disponible(contexto)) {
            return Result.success();   // no hay dispositivo vinculado: nada que hacer
        }

        Instant limite = alinear(Instant.now());            // solo ventanas completas
        Instant inicio = sesion.finUltimaVentana();
        Instant masAntiguo = alinear(Instant.now().minus(MAXIMO_ATRASO));
        if (inicio == null) {
            inicio = limite.minus(TAMANO_VENTANA);
        } else if (inicio.isBefore(masAntiguo)) {
            inicio = masAntiguo;
        }

        try {
            while (!inicio.plus(TAMANO_VENTANA).isAfter(limite)) {
                Instant fin = inicio.plus(TAMANO_VENTANA);
                SaludConexion.Ventana ventana =
                        SaludConexion.leerVentana(contexto, inicio, fin).get(60, TimeUnit.SECONDS);

                if (ventana != null) {
                    Response<Void> respuesta = ApiCliente.servicio(contexto)
                            .enviarVentana(tokenDispositivo, aSolicitud(ventana))
                            .execute();
                    if (respuesta.code() == 401 || respuesta.code() == 403) {
                        Log.w(TAG, "Credencial del dispositivo rechazada; se detiene la sincronización");
                        return Result.failure();
                    }
                    if (!respuesta.isSuccessful() && respuesta.code() != 409) {
                        return Result.retry();
                    }
                }
                sesion.guardarFinUltimaVentana(fin);
                inicio = fin;
            }
            return Result.success();
        } catch (IOException | TimeoutException e) {
            Log.w(TAG, "Sin conexión; se reintenta más tarde", e);
            return Result.retry();
        } catch (ExecutionException e) {
            // SecurityException: el usuario quitó los permisos desde Health Connect
            Log.w(TAG, "No se pudo leer Health Connect", e.getCause());
            return Result.failure();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }
    }

    /** Redondea hacia abajo a un múltiplo de 15 minutos (8:07 → 8:00), para que las ventanas no se crucen. */
    static Instant alinear(Instant instante) {
        Instant minuto = instante.truncatedTo(ChronoUnit.MINUTES);
        long minutoDelDia = minuto.atZone(Fechas.ZONA).getMinute();
        return minuto.minus(minutoDelDia % 15, ChronoUnit.MINUTES);
    }

    private static Dtos.VentanaSolicitud aSolicitud(SaludConexion.Ventana v) {
        Dtos.VentanaSolicitud s = new Dtos.VentanaSolicitud();
        s.inicio = Fechas.aTexto(v.getInicio());
        s.fin = Fechas.aTexto(v.getFin());
        s.fcPromedio = v.getFcPromedio();
        s.hrvPromedio = v.getHrvPromedio();
        s.numLecturas = v.getNumLecturas();
        return s;
    }
}
