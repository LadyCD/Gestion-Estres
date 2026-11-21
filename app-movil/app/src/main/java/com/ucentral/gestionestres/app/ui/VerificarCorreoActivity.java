package com.ucentral.gestionestres.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.ucentral.gestionestres.app.R;
import com.ucentral.gestionestres.app.api.ApiCliente;
import com.ucentral.gestionestres.app.api.Dtos;
import com.ucentral.gestionestres.app.api.Errores;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** HU001: el usuario escribe el código de 6 dígitos que recibió en su correo. */
public class VerificarCorreoActivity extends AppCompatActivity {

    public static final String EXTRA_CORREO = "correo";

    private String correo;
    private TextInputEditText codigo;
    private TextView mensaje;
    private Button verificar;
    private Button reenviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verificar_correo);

        correo = getIntent().getStringExtra(EXTRA_CORREO);
        if (correo == null) {
            finish();
            return;
        }
        codigo = findViewById(R.id.campoCodigo);
        mensaje = findViewById(R.id.textoMensaje);
        verificar = findViewById(R.id.botonVerificar);
        reenviar = findViewById(R.id.botonReenviar);
        TextView instruccion = findViewById(R.id.textoInstruccion);
        instruccion.setText(getString(R.string.instruccion_verificar, correo));

        verificar.setOnClickListener(v -> verificar());
        reenviar.setOnClickListener(v -> reenviar());
    }

    private void verificar() {
        String c = codigo.getText() == null ? "" : codigo.getText().toString().trim();
        if (!c.matches("\\d{6}")) {
            mostrar(getString(R.string.error_codigo));
            return;
        }
        verificar.setEnabled(false);
        mensaje.setVisibility(View.GONE);

        ApiCliente.servicio(this).verificarCorreo(new Dtos.VerificacionSolicitud(correo, c))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> respuesta) {
                        verificar.setEnabled(true);
                        if (respuesta.isSuccessful()) {
                            Toast.makeText(VerificarCorreoActivity.this, R.string.correo_verificado,
                                    Toast.LENGTH_LONG).show();
                            Intent login = new Intent(VerificarCorreoActivity.this, LoginActivity.class);
                            login.putExtra(LoginActivity.EXTRA_CORREO, correo);
                            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(login);
                            finish();
                        } else {
                            // Código incorrecto, vencido o con los intentos agotados: el backend explica cuál
                            mostrar(Errores.mensaje(respuesta, getString(R.string.error_codigo)));
                            codigo.setText("");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        verificar.setEnabled(true);
                        mostrar(getString(R.string.error_conexion));
                    }
                });
    }

    private void reenviar() {
        reenviar.setEnabled(false);
        ApiCliente.servicio(this).reenviarCodigo(new Dtos.CorreoSolicitud(correo))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> respuesta) {
                        // Se deja pasar un momento antes de permitir otro reenvío
                        codigo.postDelayed(() -> reenviar.setEnabled(true), 30_000);
                        Toast.makeText(VerificarCorreoActivity.this, R.string.codigo_reenviado,
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        reenviar.setEnabled(true);
                        mostrar(getString(R.string.error_conexion));
                    }
                });
    }

    private void mostrar(String texto) {
        mensaje.setText(texto);
        mensaje.setVisibility(View.VISIBLE);
    }
}
