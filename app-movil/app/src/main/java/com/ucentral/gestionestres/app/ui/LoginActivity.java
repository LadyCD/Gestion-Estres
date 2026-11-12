package com.ucentral.gestionestres.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.ucentral.gestionestres.app.R;
import com.ucentral.gestionestres.app.api.ApiCliente;
import com.ucentral.gestionestres.app.api.Dtos;
import com.ucentral.gestionestres.app.api.Errores;
import com.ucentral.gestionestres.app.sesion.Sesion;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** HU020: inicio de sesión con correo institucional y contraseña. */
public class LoginActivity extends AppCompatActivity {

    /** Correo que llega ya escrito, por ejemplo después de verificarlo (HU001). */
    public static final String EXTRA_CORREO = "correo";

    private TextInputEditText correo;
    private TextInputEditText contrasena;
    private TextView mensaje;
    private Button ingresar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sesion sesion = new Sesion(this);
        if (sesion.haySesion() && !sesion.vencioPorInactividad()) {
            abrirInicio();
            return;
        }
        setContentView(R.layout.activity_login);

        correo = findViewById(R.id.campoCorreo);
        contrasena = findViewById(R.id.campoContrasena);
        mensaje = findViewById(R.id.textoMensaje);
        ingresar = findViewById(R.id.botonIngresar);

        String correoRecibido = getIntent().getStringExtra(EXTRA_CORREO);
        if (correoRecibido != null) {
            correo.setText(correoRecibido);
            contrasena.requestFocus();
        }

        ingresar.setOnClickListener(v -> ingresar());
        findViewById(R.id.botonCrearCuenta).setOnClickListener(
                v -> startActivity(new Intent(this, RegistroActivity.class)));
    }

    private void ingresar() {
        String c = texto(correo);
        String p = texto(contrasena);
        if (c.isEmpty() || p.isEmpty()) {
            mostrar(getString(R.string.login_incorrecto));
            return;
        }
        ingresar.setEnabled(false);
        mensaje.setVisibility(View.GONE);

        ApiCliente.servicio(this).login(new Dtos.LoginSolicitud(c, p))
                .enqueue(new Callback<Dtos.LoginRespuesta>() {
                    @Override
                    public void onResponse(@NonNull Call<Dtos.LoginRespuesta> call,
                                           @NonNull Response<Dtos.LoginRespuesta> respuesta) {
                        ingresar.setEnabled(true);
                        Dtos.LoginRespuesta cuerpo = respuesta.body();
                        if (respuesta.isSuccessful() && cuerpo != null) {
                            new Sesion(LoginActivity.this).iniciar(cuerpo.token, cuerpo.rol, cuerpo.nombreCompleto);
                            abrirInicio();
                        } else if (respuesta.code() == 403) {
                            // HU001: contraseña correcta, pero el correo aún no se verificó
                            Intent verificar = new Intent(LoginActivity.this, VerificarCorreoActivity.class);
                            verificar.putExtra(VerificarCorreoActivity.EXTRA_CORREO, c.toLowerCase());
                            startActivity(verificar);
                        } else if (respuesta.code() == 423) {
                            // Cuenta bloqueada por 5 intentos fallidos: el backend envía el mensaje
                            mostrar(Errores.mensaje(respuesta, getString(R.string.login_incorrecto)));
                        } else {
                            // Mensaje genérico: no dice cuál de los dos datos falló
                            mostrar(getString(R.string.login_incorrecto));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Dtos.LoginRespuesta> call, @NonNull Throwable t) {
                        ingresar.setEnabled(true);
                        mostrar(getString(R.string.error_conexion));
                    }
                });
    }

    private void abrirInicio() {
        startActivity(new Intent(this, InicioActivity.class));
        finish();
    }

    private void mostrar(String texto) {
        mensaje.setText(texto);
        mensaje.setVisibility(View.VISIBLE);
    }

    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
