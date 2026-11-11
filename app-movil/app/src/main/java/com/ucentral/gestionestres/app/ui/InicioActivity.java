package com.ucentral.gestionestres.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.ucentral.gestionestres.app.R;
import com.ucentral.gestionestres.app.api.ApiCliente;
import com.ucentral.gestionestres.app.api.Dtos;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Pantalla principal: nivel de la última hora (HU003), línea base (HU025) y accesos. */
public class InicioActivity extends BaseActivity {

    private TextView nivel;
    private TextView parcial;
    private TextView lineaBase;
    private Button vincular;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        nivel = findViewById(R.id.textoNivel);
        parcial = findViewById(R.id.textoParcial);
        lineaBase = findViewById(R.id.textoLineaBase);
        vincular = findViewById(R.id.botonVincular);

        vincular.setOnClickListener(v -> startActivity(new Intent(this, DispositivoActivity.class)));
        findViewById(R.id.botonChequeo).setOnClickListener(
                v -> startActivity(new Intent(this, ChequeoActivity.class)));
        findViewById(R.id.botonMiDispositivo).setOnClickListener(
                v -> startActivity(new Intent(this, DispositivoActivity.class)));
        findViewById(R.id.botonCerrarSesion).setOnClickListener(v -> cerrarSesion());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sesion.haySesion()) {
            return;
        }
        ((TextView) findViewById(R.id.textoSaludo)).setText(getString(R.string.saludo, sesion.nombre()));

        // HU020: el rol SST/Talento Humano no ve datos individuales
        boolean trabajador = sesion.esTrabajador();
        findViewById(R.id.panelTrabajador).setVisibility(trabajador ? View.VISIBLE : View.GONE);
        findViewById(R.id.textoRolSst).setVisibility(trabajador ? View.GONE : View.VISIBLE);
        if (trabajador) {
            consultarDispositivo();
            consultarLineaBase();
            consultarNivel();
        }
    }

    /** HU002: el botón «Vincular dispositivo» aparece arriba solo si no hay dispositivo vinculado. */
    private void consultarDispositivo() {
        ApiCliente.servicio(this).dispositivo().enqueue(new Callback<Dtos.Dispositivo>() {
            @Override
            public void onResponse(@NonNull Call<Dtos.Dispositivo> call, @NonNull Response<Dtos.Dispositivo> r) {
                if (sesionRechazada(r.code())) {
                    return;
                }
                boolean vinculado = r.isSuccessful() && r.body() != null && "VINCULADO".equals(r.body().estado);
                vincular.setVisibility(vinculado ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(@NonNull Call<Dtos.Dispositivo> call, @NonNull Throwable t) {
                vincular.setVisibility(View.VISIBLE);
            }
        });
    }

    private void consultarLineaBase() {
        ApiCliente.servicio(this).lineaBase().enqueue(new Callback<Dtos.LineaBase>() {
            @Override
            public void onResponse(@NonNull Call<Dtos.LineaBase> call, @NonNull Response<Dtos.LineaBase> r) {
                Dtos.LineaBase lb = r.body();
                if (r.isSuccessful() && lb != null && !"LISTA".equals(lb.estado)) {
                    lineaBase.setText(getString(R.string.linea_base_progreso, lb.diasValidos, lb.diasRequeridos));
                    lineaBase.setVisibility(View.VISIBLE);
                } else {
                    lineaBase.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Dtos.LineaBase> call, @NonNull Throwable t) {
                lineaBase.setVisibility(View.GONE);
            }
        });
    }

    /** Pide la clasificación inmediata; si no hay datos de la última hora (204), muestra la última guardada. */
    private void consultarNivel() {
        ApiCliente.servicio(this).evaluarAhora().enqueue(new Callback<Dtos.Deteccion>() {
            @Override
            public void onResponse(@NonNull Call<Dtos.Deteccion> call, @NonNull Response<Dtos.Deteccion> r) {
                if (r.isSuccessful() && r.body() != null && r.body().nivel != null) {
                    mostrarNivel(r.body());
                } else {
                    consultarUltima();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Dtos.Deteccion> call, @NonNull Throwable t) {
                consultarUltima();
            }
        });
    }

    private void consultarUltima() {
        ApiCliente.servicio(this).ultimaDeteccion().enqueue(new Callback<Dtos.Deteccion>() {
            @Override
            public void onResponse(@NonNull Call<Dtos.Deteccion> call, @NonNull Response<Dtos.Deteccion> r) {
                Dtos.Deteccion d = r.body();
                if (!r.isSuccessful() || d == null || d.nivel == null) {   // 204: sin detección
                    nivel.setText(R.string.nivel_sin_datos);
                    nivel.setTextColor(ContextCompat.getColor(InicioActivity.this, android.R.color.darker_gray));
                    parcial.setVisibility(View.GONE);
                    return;
                }
                mostrarNivel(d);
            }

            @Override
            public void onFailure(@NonNull Call<Dtos.Deteccion> call, @NonNull Throwable t) {
                nivel.setText(R.string.nivel_sin_datos);
            }
        });
    }

    private void mostrarNivel(Dtos.Deteccion d) {
        switch (d.nivel) {
            case "ALTO":
                pintar("Alto", R.color.nivel_alto);
                break;
            case "MODERADO":
                pintar("Moderado", R.color.nivel_moderado);
                break;
            default:
                pintar("Bajo", R.color.nivel_bajo);
        }
        parcial.setVisibility(d.parcial ? View.VISIBLE : View.GONE);
    }

    private void pintar(String texto, int color) {
        nivel.setText(texto);
        nivel.setTextColor(ContextCompat.getColor(this, color));
    }

    private void cerrarSesion() {
        ApiCliente.servicio(this).logout().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> r) { }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) { }
        });
        sesion.cerrar();
        irALogin(null);
    }
}
