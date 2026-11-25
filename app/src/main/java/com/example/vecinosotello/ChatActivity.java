package com.example.vecinosotello;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/** Chat general: mensajes en tiempo real con nombre, vivienda, fecha y hora. */
public class ChatActivity extends AppCompatActivity {

    private LinearLayout lista;
    private ScrollView scroll;
    private TextView total, errorChat;
    private Chat.Escucha escucha, escuchaConexion;
    private int limite = Chat.PAGINA;
    private List<Chat.Mensaje> ultimos = new ArrayList<>();
    private boolean hayMas, irAlFinal = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sesion.activa()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_chat);
        lista = findViewById(R.id.mensajes);
        scroll = findViewById(R.id.scroll);
        total = findViewById(R.id.total);
        errorChat = findViewById(R.id.errorChat);
        findViewById(R.id.volver).setOnClickListener(v -> finish());

        EditText texto = findViewById(R.id.texto);
        Runnable mandar = () -> {
            String e = Chat.enviar(texto.getText().toString(), this::pintar, this::mostrarError);
            if (e != null) {
                texto.setError(e);
                return;
            }
            texto.setText("");
            irAlFinal = true;
        };
        findViewById(R.id.enviar).setOnClickListener(v -> mandar.run());
        texto.setOnEditorActionListener((v, accion, e) -> {
            if (accion != EditorInfo.IME_ACTION_SEND) return false;
            mandar.run();
            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (lista == null) return;
        escuchar();
        escuchaConexion = Conexion.escuchar(this::mostrarConexion);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Todo lo que se vio en el chat deja de contar como "nuevo" en el mapa
        getSharedPreferences("chat", MODE_PRIVATE).edit().putLong("visto", System.currentTimeMillis()).apply();
        if (escucha != null) escucha.quitar();
        if (escuchaConexion != null) escuchaConexion.quitar();
        escucha = escuchaConexion = null;
    }

    private void escuchar() {
        if (escucha != null) escucha.quitar(); // nunca dos listeners a la vez
        escucha = Chat.escuchar(limite, (mensajes, mas) -> {
            ultimos = mensajes;
            hayMas = mas;
            errorChat.setVisibility(View.GONE);
            pintar();
        }, this::mostrarError);
    }

    private void mostrarError(String e) {
        errorChat.setText(e + " Toca aquí para reintentar.");
        errorChat.setVisibility(View.VISIBLE);
        errorChat.setOnClickListener(v -> escuchar());
    }

    private void mostrarConexion(boolean conectado) {
        GradientDrawable punto = new GradientDrawable();
        punto.setShape(GradientDrawable.OVAL);
        punto.setColor(ContextCompat.getColor(this, conectado ? R.color.ok : R.color.aviso));
        punto.setSize(dp(8), dp(8));
        total.setCompoundDrawablesRelativeWithIntrinsicBounds(punto, null, null, null);
        total.setText(conectado ? "Conectado · Pje. Otello" : "Sin conexión · los mensajes se enviarán al volver");
    }

    private void pintar() {
        lista.removeAllViews();
        if (hayMas) lista.addView(botonCargarMas());
        if (ultimos.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Aún no hay mensajes. ¡Escribe el primero!");
            vacio.setTextColor(ContextCompat.getColor(this, R.color.texto_2));
            vacio.setGravity(Gravity.CENTER);
            vacio.setPadding(0, dp(32), 0, 0);
            lista.addView(vacio);
        }
        for (Chat.Mensaje m : ultimos) lista.addView(burbuja(m));
        if (irAlFinal) scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
        irAlFinal = true;
    }

    private View botonCargarMas() {
        TextView b = new TextView(this);
        b.setText("Cargar mensajes anteriores");
        b.setTextColor(ContextCompat.getColor(this, R.color.primario));
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(48));
        b.setBackgroundResource(R.drawable.bg_fila);
        b.setOnClickListener(v -> {
            limite += Chat.PAGINA; // pide una página más hacia atrás
            irAlFinal = false;
            escuchar();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        b.setLayoutParams(lp);
        return b;
    }

    private TextView burbuja(Chat.Mensaje m) {
        TextView burbuja = new TextView(this);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String autor = m.mio ? "Tú · #" + m.vivienda : m.autor();
        sb.append(autor).append("\n");
        sb.setSpan(new ForegroundColorSpan(m.mio ? 0xFFDBEAFE : ContextCompat.getColor(this, R.color.primario)),
                0, autor.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, autor.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new RelativeSizeSpan(0.85f), 0, autor.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(m.texto).append("\n");
        int ini = sb.length();
        sb.append(m.pendiente() ? "Enviando…" : Fechas.corta(m.fecha));
        sb.setSpan(new RelativeSizeSpan(0.78f), ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(m.mio ? 0xFFBFDBFE : 0xFF94A3B8), ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        burbuja.setText(sb);
        burbuja.setTextColor(ContextCompat.getColor(this, m.mio ? R.color.blanco : R.color.texto));
        burbuja.setTextSize(15);
        burbuja.setLineSpacing(0, 1.15f);
        burbuja.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));
        burbuja.setPadding(dp(13), dp(10), dp(13), dp(10));
        burbuja.setBackgroundResource(m.mio ? R.drawable.bg_msg_mio : R.drawable.bg_msg_otro);
        if (m.pendiente()) burbuja.setAlpha(0.7f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = m.mio ? Gravity.END : Gravity.START;
        lp.bottomMargin = dp(10);
        burbuja.setLayoutParams(lp);
        return burbuja;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
