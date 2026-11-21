package com.ucentral.gestionestres.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.ucentral.gestionestres.app.R;
import com.ucentral.gestionestres.app.api.ApiCliente;
import com.ucentral.gestionestres.app.api.Dtos;
import com.ucentral.gestionestres.app.api.Errores;
import com.ucentral.gestionestres.app.api.Fechas;
import com.ucentral.gestionestres.app.salud.SaludConexion;
import com.ucentral.gestionestres.app.salud.SincronizacionWorker;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HU002: vincular y desvincular el wearable a través de Health Connect.
 * Al vincular se programa la sincronización cada 15 minutos (HU026).
 */
public class DispositivoActivity extends BaseActivity {

    private TextView estado;
    private TextView ultimaSincronizacion;
    private TextView mensaje;
    private Button vincular;
    private Button desvincular;
    private Button instalarHealthConnect;

    private ActivityResultLauncher<Set<String>> solicitarPermisos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivo);

        estado = findViewById(R.id.textoEstado);
        ultimaSincronizacion = findViewById(R.id.textoUltimaSincronizacion);
        mensaje = findViewById(R.id.textoMensaje);
        vincular = findViewById(R.id.botonVincular);
        desvincular = findViewById(R.id.botonDesvincular);
        instalarHealthConnect = findViewById(R.id.botonInstalarHc);

        // Se registra en onCreate, como exige la API de resultados de Android
        solicitarPermisos = registerForActivityResult(
                SaludConexion.contratoPermisos(), concedidos -> verificarPermisos());

        vincular.setOnClickListener(v -> iniciarVinculacion());
        desvincular.setOnClickListener(v -> desvincular());
        instalarHealthConnect.setOnClickListener(v -> abrirPlayStore());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sesion.haySesion()) {
            consultarEstado();
        }
    }

    private void consultarEstado() {
        ApiCliente.servicio(this).dispositivo().enqueue(new Callback<Dtos.Dispositivo>() {
            @Override
            public void onResponse(@NonNull Call<Dtos.Dispositivo> call, @NonNull Response<Dtos.Dispositivo> r) {
                if (sesionRechazada(r.code())) {
                    return;
                }
                mostrarEstado(r.isSuccessful() ? r.body() : null);
            }

            @Override
            public void onFailure(@NonNull Call<Dtos.Dispositivo> call, @NonNull Throwable t) {
                mostrarMensaje(getString(R.string.error_conexion));
            }
        });
    }

    private void mostrarEstado(Dtos.Dispositivo d) {
        boolean vinculado = d != null && "VINCULADO".equals(d.estado);
        estado.setText(vinculado ? R.string.estado_vinculado : R.string.estado_desconectado);
        String ultima = d == null ? null : Fechas.legible(d.ultimaSincronizacion);
        ultimaSincronizacion.setText(ultima == null
                ? getString(R.string.sin_sincronizacion)
                : getString(R.string.ultima_sincronizacion, ultima));
        vincular.setVisibility(vinculado ? View.GONE : View.VISIBLE);
        desvincular.setVisibility(vinculado ? View.VISIBLE : View.GONE);
        if (vinculado && Boolean.FALSE.equals(d.hrvDisponible)) {
            mostrarMensaje(getString(R.string.sin_hrv));
        }
    }

    // ---------------- Vincular ----------------

    private void iniciarVinculacion() {
        instalarHealthConnect.setVisibility(View.GONE);
        if (!SaludConexion.disponible(this)) {
            mostrarError(getString(R.string.error_sin_hc));
            instalarHealthConnect.setVisibility(View.VISIBLE);
            return;
        }
        solicitarPermisos.launch(SaludConexion.permisosSolicitados());
    }

    /** Se consulta a Health Connect qué permisos quedaron concedidos. */
    private void verificarPermisos() {
        SaludConexion.permisosConcedidos(this).whenComplete((concedidos, error) -> runOnUiThread(() -> {
            if (error != null || concedidos == null || !concedidos.contains(SaludConexion.PERMISO_FC)) {
                mostrarError(getString(R.string.error_permiso_fc));
                return;
            }
            comprobarDatos(concedidos.contains(SaludConexion.PERMISO_HRV));
        }));
    }

    /** Debe haber datos de frecuencia cardíaca de algún wearable en las últimas 24 horas. */
    private void comprobarDatos(boolean permisoHrv) {
        Instant ahora = Instant.now();
        SaludConexion.leerVentana(this, ahora.minus(Duration.ofHours(24)), ahora)
                .whenComplete((ventana, error) -> runOnUiThread(() -> {
                    if (error != null || ventana == null) {
                        mostrarError(getString(R.string.error_sin_datos));
                        return;
                    }
                    boolean hayHrv = permisoHrv && ventana.getHrvPromedio() != null;
                    registrarVinculacion(hayHrv);
                }));
    }

    private void registrarVinculacion(boolean hayHrv) {
        ApiCliente.servicio(this)
                .actualizarDispositivo(new Dtos.DispositivoSolicitud("VINCULADO", hayHrv))
                .enqueue(new Callback<Dtos.Dispositivo>() {
                    @Override
                    public void onResponse(@NonNull Call<Dtos.Dispositivo> call,
                                           @NonNull Response<Dtos.Dispositivo> r) {
                        if (sesionRechazada(r.code())) {
                            return;
                        }
                        Dtos.Dispositivo d = r.body();
                        if (r.isSuccessful() && d != null) {
                            sesion.guardarTokenDispositivo(d.tokenDispositivo);
                            SincronizacionWorker.programar(DispositivoActivity.this);
                            mostrarEstado(d);
                            mostrarMensaje(hayHrv
                                    ? getString(R.string.dispositivo_vinculado)
                                    : getString(R.string.dispositivo_vinculado) + "\n" + getString(R.string.sin_hrv));
                        } else if (r.code() == 409 || r.code() == 422) {
                            mostrarError(Errores.mensaje(r, getString(R.string.error_sin_consentimiento)));
                        } else {
                            mostrarError(Errores.mensaje(r, getString(R.string.error_conexion)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Dtos.Dispositivo> call, @NonNull Throwable t) {
                        mostrarError(getString(R.string.error_conexion));
                    }
                });
    }

    // ---------------- Desvincular ----------------

    private void desvincular() {
        ApiCliente.servicio(this)
                .actualizarDispositivo(new Dtos.DispositivoSolicitud("DESCONECTADO", false))
                .enqueue(new Callback<Dtos.Dispositivo>() {
                    @Override
                    public void onResponse(@NonNull Call<Dtos.Dispositivo> call,
                                           @NonNull Response<Dtos.Dispositivo> r) {
                        if (sesionRechazada(r.code())) {
                            return;
                        }
                        if (r.isSuccessful()) {
                            SincronizacionWorker.cancelar(DispositivoActivity.this);
                            SaludConexion.revocarPermisos(DispositivoActivity.this);
                            sesion.olvidarDispositivo();
                            mostrarEstado(r.body());
                            mostrarMensaje(getString(R.string.dispositivo_desvinculado));
                        } else {
                            mostrarError(Errores.mensaje(r, getString(R.string.error_conexion)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Dtos.Dispositivo> call, @NonNull Throwable t) {
                        mostrarError(getString(R.string.error_conexion));
                    }
                });
    }

    // ---------------- Apoyo ----------------

    private void abrirPlayStore() {
        Uri uri = Uri.parse("market://details?id=" + SaludConexion.PAQUETE_HEALTH_CONNECT
                + "&url=healthconnect%3A%2F%2Fonboarding");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending");
        try {
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://play.google.com/store/apps/details?id=" + SaludConexion.PAQUETE_HEALTH_CONNECT)));
        }
    }

    /** Error con causa específica y botón «Reintentar» (HU002). */
    private void mostrarError(String texto) {
        mostrarMensaje(texto);
        vincular.setText(R.string.boton_reintentar);
        vincular.setVisibility(View.VISIBLE);
        desvincular.setVisibility(View.GONE);
    }

    private void mostrarMensaje(String texto) {
        mensaje.setText(texto);
        mensaje.setVisibility(View.VISIBLE);
    }
}
