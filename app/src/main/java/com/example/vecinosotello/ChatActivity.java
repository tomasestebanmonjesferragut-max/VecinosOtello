package com.example.vecinosotello;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;



public class ChatActivity extends AppCompatActivity {

    private LinearLayout lista;
    private ScrollView scroll;
    private Chat.Escucha escucha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Datos.miNumero == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_chat);
        lista = findViewById(R.id.mensajes);
        scroll = findViewById(R.id.scroll);
        ((TextView) findViewById(R.id.total)).setText(Datos.casas.size() + " casas · Pje. Otello");
        findViewById(R.id.volver).setOnClickListener(v -> finish());

        EditText texto = findViewById(R.id.texto);
        Runnable mandar = () -> {
            String t = texto.getText().toString().trim();
            if (t.isEmpty()) return;
            Datos.enviar(t, false); // Firebase avisa y la lista se repinta sola
            texto.setText("");
        };
        findViewById(R.id.enviar).setOnClickListener(v -> mandar.run());
        texto.setOnEditorActionListener((v, accion, e) -> {
            if (accion != EditorInfo.IME_ACTION_SEND) return false;
            mandar.run();
            return true;
        });

        // Alertas rápidas
        LinearLayout rapidos = findViewById(R.id.rapidos);
        Object[][] chips = {
                {R.drawable.ic_sospechoso, "Persona sospechosa"}, {R.drawable.ic_auto, "Auto desconocido"},
                {R.drawable.ic_ampolleta, "Corte de luz"}, {R.drawable.ic_fuego, "Humo o incendio"}};
        for (Object[] c : chips) {
            TextView chip = new TextView(this);
            String tipo = (String) c[1];
            chip.setText(tipo);
            chip.setTextColor(ContextCompat.getColor(this, R.color.peligro));
            chip.setTextSize(13);
            chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setBackgroundResource(R.drawable.bg_chip_rapido);
            chip.setPadding(dp(12), 0, dp(14), 0);
            chip.setMinHeight(dp(40));
            Drawable ic = ContextCompat.getDrawable(this, (int) c[0]);
            if (ic != null) {
                ic = ic.mutate();
                ic.setTint(ContextCompat.getColor(this, R.color.peligro));
                ic.setBounds(0, 0, dp(16), dp(16));
                chip.setCompoundDrawablesRelative(ic, null, null, null);
                chip.setCompoundDrawablePadding(dp(6));
            }
            chip.setOnClickListener(v -> Datos.enviar(tipo, true));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
            lp.setMarginEnd(dp(8));
            rapidos.addView(chip, lp);
        }
        pintar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (lista != null) escucha = Chat.escuchar(this::pintar); // mensajes en tiempo real
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (escucha != null) escucha.quitar();
        escucha = null;
    }

    private void pintar() {
        Datos.mensajesLeidos = Datos.mensajes.size();
        lista.removeAllViews();
        for (Datos.Mensaje m : Datos.mensajes) {
            TextView burbuja = new TextView(this);
            SpannableStringBuilder sb = new SpannableStringBuilder();
            int colorTexto = ContextCompat.getColor(this, m.mio ? R.color.blanco : m.alerta ? R.color.peligro : R.color.texto);
            if (!m.mio) {
                sb.append(m.quien).append("\n");
                sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primario)), 0, m.quien.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new StyleSpan(Typeface.BOLD), 0, m.quien.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new RelativeSizeSpan(0.85f), 0, m.quien.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.append(m.texto).append("\n");
            int ini = sb.length();
            sb.append(m.hora);
            sb.setSpan(new RelativeSizeSpan(0.78f), ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(m.mio ? 0xFFBFDBFE : 0xFF94A3B8), ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            burbuja.setText(sb);
            burbuja.setTextColor(colorTexto);
            burbuja.setTextSize(15);
            burbuja.setLineSpacing(0, 1.15f);
            burbuja.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));
            burbuja.setPadding(dp(13), dp(10), dp(13), dp(10));
            burbuja.setBackgroundResource(m.mio ? (m.alerta ? R.drawable.bg_msg_mio_alerta : R.drawable.bg_msg_mio)
                    : (m.alerta ? R.drawable.bg_msg_alerta : R.drawable.bg_msg_otro));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = m.mio ? Gravity.END : Gravity.START;
            lp.bottomMargin = dp(10);
            lista.addView(burbuja, lp);
        }
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
