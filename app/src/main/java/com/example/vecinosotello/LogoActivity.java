package com.example.vecinosotello;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Primera pantalla: muestra el logo y pasa al inicio de sesión.
 * Si ya hay una sesión iniciada, no muestra el logo y entra directo.
 */
public class LogoActivity extends AppCompatActivity {

    private static final long DURACION_MS = 1500;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable seguir = this::seguir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            seguir();
            return;
        }
        setContentView(R.layout.activity_logo);
        View logo = findViewById(R.id.logo);
        logo.setAlpha(0f);
        logo.animate().alpha(1f).setDuration(500).start();
        handler.postDelayed(seguir, DURACION_MS);
    }

    private void seguir() {
        startActivity(new Intent(this, LoginActivity.class).putExtras(getIntent()));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(seguir); // si el usuario sale antes, no abrir el login
        super.onDestroy();
    }
}
