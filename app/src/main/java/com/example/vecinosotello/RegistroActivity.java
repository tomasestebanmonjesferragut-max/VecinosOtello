package com.example.vecinosotello;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

/**
 * Registro con nombre, vivienda, correo y contraseña. Crea la cuenta en Firebase Authentication
 * y el perfil en /usuarios/{uid} con rol "vecino" (nunca administrador).
 */
public class RegistroActivity extends AppCompatActivity {

    /** La cuenta ya existe pero le falta el perfil: solo se piden nombre y vivienda. */
    public static final String EXTRA_SOLO_PERFIL = "soloPerfil";

    private MaterialButton crear;
    private TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);
        EditText nombre = findViewById(R.id.nombre), vivienda = findViewById(R.id.vivienda);
        EditText correo = findViewById(R.id.correo), clave = findViewById(R.id.clave), repetir = findViewById(R.id.repetir);
        crear = findViewById(R.id.crear);
        error = findViewById(R.id.error);
        boolean soloPerfil = getIntent().getBooleanExtra(EXTRA_SOLO_PERFIL, false);
        Ui.ojoClave(clave, findViewById(R.id.ver_clave));
        Ui.ojoClave(repetir, findViewById(R.id.ver_repetir));

        if (soloPerfil) { // ocultar lo que ya existe
            ocultarCampos();
            crear.setText("Guardar perfil");
        }

        findViewById(R.id.volver).setOnClickListener(v -> {
            if (soloPerfil) FirebaseAuth.getInstance().signOut();
            finish();
        });

        crear.setOnClickListener(v -> {
            String n = nombre.getText().toString(), viv = vivienda.getText().toString();
            String problema = soloPerfil
                    ? primeroNoNulo(Validaciones.validarNombre(n),
                        Validaciones.viviendaValida(viv) ? null : "La vivienda debe ser un número con hasta 2 letras, por ejemplo 311A.")
                    : Validaciones.validarRegistro(n, viv, correo.getText().toString(),
                        clave.getText().toString(), repetir.getText().toString());
            error.setText(problema == null ? "" : problema);
            if (problema != null) return;
            cargando(true);
            if (soloPerfil) {
                guardarPerfil(n, viv);
                return;
            }
            FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(correo.getText().toString().trim(), clave.getText().toString())
                    .addOnSuccessListener(r -> guardarPerfil(n, viv))
                    .addOnFailureListener(e -> {
                        cargando(false);
                        error.setText(e instanceof FirebaseAuthUserCollisionException
                                ? "Ese correo ya tiene una cuenta. Vuelve e inicia sesión."
                                : LoginActivity.mensajeDeError(e));
                    });
        });
    }

    private void guardarPerfil(String nombre, String vivienda) {
        Sesion.crearPerfil(nombre, vivienda, e -> {
            cargando(false);
            if (e != null) {
                error.setText(e);
                return;
            }
            LoginActivity.abrirApp(this, getIntent().getStringExtra(LoginActivity.EXTRA_ANUNCIO));
            finishAffinity();
        });
    }

    /** Oculta correo y contraseñas con sus etiquetas (cada etiqueta está justo antes de su campo). */
    private void ocultarCampos() {
        for (int id : new int[]{R.id.correo, R.id.clave, R.id.repetir}) {
            View campo = findViewById(id);
            // Las contraseñas van dentro de un FrameLayout junto a su ojo: se oculta el conjunto
            View bloque = campo.getParent() instanceof android.widget.FrameLayout ? (View) campo.getParent() : campo;
            android.view.ViewGroup padre = (android.view.ViewGroup) bloque.getParent();
            int i = padre.indexOfChild(bloque);
            if (i > 0) padre.getChildAt(i - 1).setVisibility(View.GONE);
            bloque.setVisibility(View.GONE);
        }
    }

    private static String primeroNoNulo(String a, String b) { return a != null ? a : b; }

    private void cargando(boolean si) {
        crear.setEnabled(!si);
        crear.setText(si ? "Guardando…"
                : getIntent().getBooleanExtra(EXTRA_SOLO_PERFIL, false) ? "Guardar perfil" : "Crear cuenta");
    }
}
