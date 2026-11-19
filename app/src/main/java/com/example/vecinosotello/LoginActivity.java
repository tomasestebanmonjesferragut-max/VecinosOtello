package com.example.vecinosotello;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private MaterialButton entrar;
    private TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText correo = findViewById(R.id.correo);
        EditText clave = findViewById(R.id.clave);
        error = findViewById(R.id.error);
        entrar = findViewById(R.id.entrar);

        entrar.setOnClickListener(v -> {
            String c = correo.getText().toString().trim().toLowerCase();
            String k = clave.getText().toString();
            String problema = Datos.validar(c, k);
            error.setText(problema == null ? "" : problema);
            if (problema != null) return;
            cargando(true);
            iniciarSesion(c, k);
        });

        // Si ya hay sesión (por ejemplo, al tocar una notificación), entra directo
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (savedInstanceState == null && u != null && u.getEmail() != null
                && Datos.casaPorNumero(Datos.numeroDelCorreo(u.getEmail())) != null) {
            abrirMapa(u.getEmail());
        }
    }

    /** Entra con la cuenta de la casa en Firebase (las cuentas se crean en Firebase, no desde la app). */
    private void iniciarSesion(String correo, String clave) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, clave)
                .addOnSuccessListener(r -> abrirMapa(correo))
                .addOnFailureListener(e -> {
                    cargando(false);
                    boolean datosMalos = e instanceof FirebaseAuthInvalidUserException
                            || e instanceof FirebaseAuthInvalidCredentialsException;
                    error.setText(datosMalos ? "Correo o contraseña incorrectos."
                            : "No se pudo conectar. Revisa tu internet e intenta de nuevo.");
                });
    }

    private void abrirMapa(String correo) {
        cargando(false);
        Datos.miNumero = Datos.numeroDelCorreo(correo); // la casa sale de los números del correo
        ((EditText) findViewById(R.id.clave)).setText("");
        startActivity(new Intent(this, MapaActivity.class));
    }

    private void cargando(boolean si) {
        entrar.setEnabled(!si);
        entrar.setText(si ? "Entrando…" : "Entrar");
    }
}
