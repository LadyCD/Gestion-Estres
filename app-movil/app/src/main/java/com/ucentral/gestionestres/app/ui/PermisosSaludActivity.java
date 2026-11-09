package com.ucentral.gestionestres.app.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ucentral.gestionestres.app.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Pantalla que Health Connect abre cuando el usuario pregunta cómo se usan sus datos.
 * Muestra la explicación de los permisos y el consentimiento informado completo.
 */
public class PermisosSaludActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permisos_salud);

        String consentimiento;
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(R.raw.consentimiento_v1_0), StandardCharsets.UTF_8))) {
            consentimiento = lector.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            consentimiento = "";
        }
        ((TextView) findViewById(R.id.textoConsentimiento)).setText(consentimiento);
    }
}
