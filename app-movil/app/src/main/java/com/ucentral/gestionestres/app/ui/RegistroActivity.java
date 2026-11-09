package com.ucentral.gestionestres.app.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.ucentral.gestionestres.app.R;
import com.ucentral.gestionestres.app.api.ApiCliente;
import com.ucentral.gestionestres.app.api.Dtos;
import com.ucentral.gestionestres.app.api.Errores;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** HU001: registro de usuario con consentimiento informado. */
public class RegistroActivity extends AppCompatActivity {

    /** Debe coincidir con la versión publicada en la wiki (Consentimiento-Informado). */
    private static final String VERSION_CONSENTIMIENTO = "1.0";

    private TextInputEditText nombre;
    private TextInputEditText correo;
    private TextInputEditText contrasena;
    private Spinner cargo;
    private Spinner area;
    private Button registrarme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        nombre = findViewById(R.id.campoNombre);
        correo = findViewById(R.id.campoCorreo);
        contrasena = findViewById(R.id.campoContrasena);
        cargo = findViewById(R.id.listaCargo);
        area = findViewById(R.id.listaArea);
        registrarme = findViewById(R.id.botonRegistrarme);
        CheckBox acepto = findViewById(R.id.casillaAcepto);
        TextView consentimiento = findViewById(R.id.textoConsentimiento);

        consentimiento.setText(leerConsentimiento());

        // El botón solo se habilita al marcar la casilla (la casilla inicia desmarcada)
        registrarme.setEnabled(false);
        acepto.setOnCheckedChangeListener((boton, marcado) -> registrarme.setEnabled(marcado));
        registrarme.setOnClickListener(v -> registrar());

        cargarCatalogo(ApiCliente.servicio(this).cargos(), cargo);
        cargarCatalogo(ApiCliente.servicio(this).areas(), area);
    }

    private void cargarCatalogo(Call<List<Dtos.Catalogo>> llamada, Spinner lista) {
        llamada.enqueue(new Callback<List<Dtos.Catalogo>>() {
            @Override
            public void onResponse(@NonNull Call<List<Dtos.Catalogo>> call,
                                   @NonNull Response<List<Dtos.Catalogo>> respuesta) {
                if (respuesta.isSuccessful() && respuesta.body() != null) {
                    ArrayAdapter<Dtos.Catalogo> adaptador = new ArrayAdapter<>(RegistroActivity.this,
                            android.R.layout.simple_spinner_item, respuesta.body());
                    adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    lista.setAdapter(adaptador);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Dtos.Catalogo>> call, @NonNull Throwable t) {
                aviso(getString(R.string.error_conexion));
            }
        });
    }

    private void registrar() {
        if (!validar()) {
            return;
        }
        Dtos.RegistroSolicitud solicitud = new Dtos.RegistroSolicitud();
        solicitud.nombreCompleto = texto(nombre);
        solicitud.correo = texto(correo).toLowerCase();
        solicitud.contrasena = texto(contrasena);
        solicitud.cargoId = ((Dtos.Catalogo) cargo.getSelectedItem()).id;
        solicitud.areaId = ((Dtos.Catalogo) area.getSelectedItem()).id;
        solicitud.versionConsentimiento = VERSION_CONSENTIMIENTO;

        registrarme.setEnabled(false);
        ApiCliente.servicio(this).registrar(solicitud).enqueue(new Callback<Dtos.Usuario>() {
            @Override
            public void onResponse(@NonNull Call<Dtos.Usuario> call, @NonNull Response<Dtos.Usuario> respuesta) {
                registrarme.setEnabled(true);
                if (respuesta.isSuccessful()) {
                    Toast.makeText(RegistroActivity.this, R.string.registro_exitoso, Toast.LENGTH_LONG).show();
                    finish();   // vuelve al inicio de sesión
                } else if (respuesta.code() == 409) {
                    correo.setError(getString(R.string.correo_duplicado));
                    limpiarContrasena();
                } else if (respuesta.code() == 400) {
                    // Validaciones del servidor, por ejemplo el dominio del correo
                    aviso(Errores.mensaje(respuesta, getString(R.string.registro_fallido)));
                    limpiarContrasena();
                } else {
                    aviso(getString(R.string.registro_fallido));
                    limpiarContrasena();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Dtos.Usuario> call, @NonNull Throwable t) {
                registrarme.setEnabled(true);
                aviso(getString(R.string.registro_fallido));
                limpiarContrasena();
            }
        });
    }

    /** Validaciones locales de HU001; el dominio del correo lo valida el backend (es configurable). */
    private boolean validar() {
        boolean valido = true;
        String n = texto(nombre);
        if (n.length() < 3 || n.length() > 150) {
            nombre.setError(getString(R.string.error_nombre));
            valido = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(texto(correo)).matches()) {
            correo.setError(getString(R.string.error_correo));
            valido = false;
        }
        String p = texto(contrasena);
        if (p.length() < 8 || !p.matches(".*[A-Za-zÁÉÍÓÚáéíóúÑñ].*") || !p.matches(".*\\d.*")) {
            contrasena.setError(getString(R.string.error_contrasena));
            valido = false;
        }
        if (cargo.getSelectedItem() == null || area.getSelectedItem() == null) {
            aviso(getString(R.string.error_listas));
            valido = false;
        }
        return valido;
    }

    private String leerConsentimiento() {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(R.raw.consentimiento_v1_0), StandardCharsets.UTF_8))) {
            return lector.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "";
        }
    }

    private void limpiarContrasena() {
        contrasena.setText("");
    }

    private void aviso(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
