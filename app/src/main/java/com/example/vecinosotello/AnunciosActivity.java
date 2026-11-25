package com.example.vecinosotello;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

/** Anuncios oficiales del administrador. Se abre también al tocar la notificación de un anuncio. */
public class AnunciosActivity extends AppCompatActivity {

    private LinearLayout contenido;
    private ScrollView scroll;
    private TextView error;
    private View cargando;
    private Chat.Escucha escucha;
    private String abrirId; // anuncio que viene de la notificación

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sesion.activa()) { // la app estaba cerrada: pasar por el login y volver a este anuncio
            startActivity(new Intent(this, LoginActivity.class).putExtras(getIntent()));
            finish();
            return;
        }
        setContentView(R.layout.activity_lista);
        ((TextView) findViewById(R.id.titulo)).setText("Anuncios oficiales");
        ((TextView) findViewById(R.id.subtitulo)).setText("Información de la administración del pasaje");
        contenido = findViewById(R.id.contenido);
        scroll = findViewById(R.id.scroll);
        error = findViewById(R.id.error);
        cargando = findViewById(R.id.cargando);
        findViewById(R.id.volver).setOnClickListener(v -> finish());
        if (Sesion.esAdmin()) { // la app solo oculta el botón; quien lo impide de verdad son las reglas
            TextView publicar = findViewById(R.id.publicar);
            publicar.setText("Publicar anuncio");
            publicar.setVisibility(View.VISIBLE);
            publicar.setOnClickListener(v -> hojaPublicar());
        }
        abrirId = getIntent().getStringExtra(LoginActivity.EXTRA_ANUNCIO);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        abrirId = intent.getStringExtra(LoginActivity.EXTRA_ANUNCIO);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (contenido == null) return;
        if (escucha != null) escucha.quitar();
        cargando.setVisibility(View.VISIBLE);
        escucha = Anuncios.escuchar(this::pintar, e -> {
            cargando.setVisibility(View.GONE);
            error.setText(e);
            error.setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (escucha != null) escucha.quitar();
        escucha = null;
    }

    private void pintar(List<Anuncios.Anuncio> lista) {
        cargando.setVisibility(View.GONE);
        error.setVisibility(View.GONE);
        contenido.removeAllViews();
        if (lista.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Todavía no hay anuncios oficiales.");
            vacio.setTextColor(ContextCompat.getColor(this, R.color.texto_2));
            vacio.setPadding(0, Ui.dp(this, 24), 0, 0);
            contenido.addView(vacio);
        }
        for (Anuncios.Anuncio a : lista) {
            View t = tarjeta(a);
            contenido.addView(t);
            if (a.id.equals(abrirId)) { // viene de la notificación: mostrarlo
                abrirId = null;
                scroll.post(() -> scroll.smoothScrollTo(0, t.getTop()));
                detalle(a);
            }
        }
    }

    private View tarjeta(Anuncios.Anuncio a) {
        View t = LayoutInflater.from(this).inflate(R.layout.item_tarjeta, contenido, false);
        TextView etiqueta = t.findViewById(R.id.etiqueta);
        etiqueta.setText(a.importante ? "Importante · fijado" : "Anuncio");
        etiqueta.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                a.importante ? R.color.peligro_suave : R.color.primario_suave)));
        etiqueta.setTextColor(ContextCompat.getColor(this, a.importante ? R.color.peligro : R.color.primario));
        ((TextView) t.findViewById(R.id.titulo)).setText(a.titulo);
        TextView texto = t.findViewById(R.id.texto);
        texto.setText(a.contenido);
        texto.setMaxLines(4);
        ((TextView) t.findViewById(R.id.pie)).setText((a.autor == null ? "Administración" : a.autor) + " · " + Fechas.corta(a.fecha));
        t.setClickable(true);
        t.setOnClickListener(v -> detalle(a));
        return t;
    }

    private void detalle(Anuncios.Anuncio a) {
        new AlertDialog.Builder(this)
                .setTitle(a.titulo)
                .setMessage(a.contenido + "\n\n" + (a.autor == null ? "Administración" : a.autor) + " · " + Fechas.corta(a.fecha))
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void hojaPublicar() {
        Hoja h = new Hoja(this, R.drawable.ic_calendario, Hoja.Tono.AZUL, "Publicar anuncio",
                "Todos los vecinos recibirán una notificación.");
        h.textoCerrar("Cancelar");
        EditText titulo = Ui.campo(this, h.contenido, "Título", "Ej. Corte de agua el jueves",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 1);
        EditText texto = Ui.campo(this, h.contenido, "Contenido", "Detalles del anuncio",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 4);
        CheckBox importante = Ui.casilla(this, h.contenido, "Importante (queda fijado arriba)");
        TextView err = Ui.error(this, h.contenido);
        TextView enviar = Ui.boton(this, "Publicar", R.color.blanco, null);
        enviar.setBackgroundResource(R.drawable.bg_chat_card);
        enviar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        h.contenido.addView(enviar);
        enviar.setOnClickListener(x -> {
            enviar.setEnabled(false);
            enviar.setText("Publicando…");
            String e = Anuncios.publicar(titulo.getText().toString(), texto.getText().toString(), importante.isChecked(), fallo -> {
                if (fallo == null) {
                    h.cerrar();
                    Toast.makeText(this, "Anuncio publicado", Toast.LENGTH_SHORT).show();
                } else {
                    err.setText(fallo);
                    enviar.setEnabled(true);
                    enviar.setText("Reintentar");
                }
            });
            if (e != null) { err.setText(e); enviar.setEnabled(true); enviar.setText("Publicar"); }
        });
        h.mostrar();
    }
}
