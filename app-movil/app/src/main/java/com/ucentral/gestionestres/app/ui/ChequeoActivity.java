package com.ucentral.gestionestres.app.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;
import com.ucentral.gestionestres.app.R;
import com.ucentral.gestionestres.app.api.ApiCliente;
import com.ucentral.gestionestres.app.api.Dtos;
import com.ucentral.gestionestres.app.api.Errores;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** HU021: chequeo breve diario de 3 preguntas. Se puede omitir sin bloquear la app. */
public class ChequeoActivity extends BaseActivity {

    private Slider estres;
    private Slider energia;
    private RadioGroup tension;
    private Button enviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chequeo);

        estres = findViewById(R.id.deslizadorEstres);
        energia = findViewById(R.id.deslizadorEnergia);
        tension = findViewById(R.id.grupoTension);
        enviar = findViewById(R.id.botonEnviar);

        enviar.setOnClickListener(v -> enviar());
        findViewById(R.id.botonOmitir).setOnClickListener(v -> finish());
    }

    private void enviar() {
        int seleccion = tension.getCheckedRadioButtonId();
        if (seleccion == -1) {
            aviso(getString(R.string.error_tension));
            return;
        }
        Dtos.ChequeoSolicitud solicitud = new Dtos.ChequeoSolicitud(
                Math.round(estres.getValue()),
                Math.round(energia.getValue()),
                seleccion == R.id.opcionTensionSi);

        enviar.setEnabled(false);
        ApiCliente.servicio(this).enviarChequeo(solicitud).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> r) {
                enviar.setEnabled(true);
                if (sesionRechazada(r.code())) {
                    return;
                }
                if (r.isSuccessful()) {
                    aviso(getString(R.string.chequeo_guardado));
                    finish();
                } else {
                    aviso(Errores.mensaje(r, getString(R.string.error_conexion)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                enviar.setEnabled(true);
                aviso(getString(R.string.error_conexion));
            }
        });
    }
}
