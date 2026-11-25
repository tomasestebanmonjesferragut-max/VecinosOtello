package com.example.vecinosotello;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

/** Inicio de sesión con correo y contraseña (Firebase Authentication) y recuperación de contraseña. */
public class LoginActivity extends AppCompatActivity {

    /** Al tocar la notificación de un anuncio, se abre ese anuncio después de entrar. */
    public static final String EXTRA_ANUNCIO = "anuncioId";

    private MaterialButton entrar;
    private TextView error;
    private EditText correo, clave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        correo = findViewById(R.id.correo);
        clave = findViewById(R.id.clave);
        error = findViewById(R.id.error);
        entrar = findViewById(R.id.entrar);

        entrar.setOnClickListener(v -> {
            String c = correo.getText().toString().trim();
            String k = clave.getText().toString();
            String problema = Validaciones.validarLogin(c, k);
            error.setText(problema == null ? "" : problema);
            if (problema != null) return;
            cargando(true);
            FirebaseAuth.getInstance().signInWithEmailAndPassword(c, k)
                    .addOnSuccessListener(r -> cargarPerfil())
                    .addOnFailureListener(e -> {
                        cargando(false);
                        error.setText(mensajeDeError(e));
                    });
        });
        findViewById(R.id.olvide).setOnClickListener(v -> recuperarClave());
        findViewById(R.id.registrarme).setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class).putExtras(getIntent())));

        // Si ya hay sesión (por ejemplo, al tocar una notificación), entra directo
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            cargando(true);
            cargarPerfil();
        }
    }

    private void cargarPerfil() {
        Sesion.cargar(e -> {
            cargando(false);
            if (e == null) {
                abrirApp(this, getIntent().getStringExtra(EXTRA_ANUNCIO));
                finish();
            } else if ("SIN_PERFIL".equals(e)) {
                // Cuenta creada sin terminar el registro: pide nombre y vivienda
                startActivity(new Intent(this, RegistroActivity.class)
                        .putExtra(RegistroActivity.EXTRA_SOLO_PERFIL, true).putExtras(getIntent()));
            } else {
                error.setText(e + " Toca Entrar para reintentar.");
            }
        });
    }

    /** Abre el mapa; si vino de una notificación de anuncio, abre ese anuncio encima. */
    static void abrirApp(android.app.Activity a, String anuncioId) {
        Intent mapa = new Intent(a, MapaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (anuncioId == null) {
            a.startActivity(mapa);
        } else {
            a.startActivities(new Intent[]{mapa,
                    new Intent(a, AnunciosActivity.class).putExtra(EXTRA_ANUNCIO, anuncioId)});
        }
    }

    private void recuperarClave() {
        String c = correo.getText().toString().trim();
        String problema = Validaciones.validarCorreo(c);
        if (problema != null) {
            error.setText(problema + " Escríbelo arriba para enviarte el enlace.");
            return;
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(c)
                .addOnSuccessListener(r -> error.setText("Te enviamos un correo a " + c + " para cambiar tu contraseña."))
                .addOnFailureListener(e -> error.setText(mensajeDeError(e)));
    }

    static String mensajeDeError(Exception e) {
        if (e instanceof FirebaseNetworkException) return "Sin conexión. Revisa tu internet e intenta de nuevo.";
        if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException)
            return "Correo o contraseña incorrectos.";
        return "No se pudo completar. Intenta de nuevo.";
    }

    private void cargando(boolean si) {
        entrar.setEnabled(!si);
        entrar.setText(si ? "Entrando…" : "Entrar");
    }
}
