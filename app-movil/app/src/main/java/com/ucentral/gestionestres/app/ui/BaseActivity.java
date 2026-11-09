package com.ucentral.gestionestres.app.ui;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ucentral.gestionestres.app.R;
import com.ucentral.gestionestres.app.sesion.Sesion;

/**
 * Base de las pantallas que requieren sesión (HU020).
 * Si pasan más de 15 minutos sin actividad, cierra la sesión y vuelve al inicio de sesión.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected Sesion sesion;

    @Override
    protected void onResume() {
        super.onResume();
        sesion = new Sesion(this);
        if (!sesion.haySesion()) {
            irALogin(null);
        } else if (sesion.vencioPorInactividad()) {
            sesion.cerrar();
            irALogin(getString(R.string.sesion_vencida));
        } else {
            sesion.registrarActividad();
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (sesion != null) {
            sesion.registrarActividad();
        }
    }

    /** Si el backend responde 401, el token venció: se cierra la sesión. */
    protected boolean sesionRechazada(int codigoHttp) {
        if (codigoHttp == 401) {
            sesion.cerrar();
            irALogin(getString(R.string.sesion_vencida));
            return true;
        }
        return false;
    }

    protected void irALogin(String mensaje) {
        if (mensaje != null) {
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected void aviso(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}
