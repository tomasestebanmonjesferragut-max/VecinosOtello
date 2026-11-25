package com.example.vecinosotello;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/** Piezas de interfaz reutilizadas (chips de filtro, botones pequeños y campos de formulario). */
final class Ui {

    private Ui() { }

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Chip de filtro: azul si está elegido. */
    static TextView chip(Context c, String texto, boolean elegido, View.OnClickListener click) {
        TextView t = new TextView(c);
        t.setText(texto);
        t.setTextSize(13);
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(c, 40));
        t.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        t.setBackgroundResource(R.drawable.bg_chip);
        t.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, elegido ? R.color.primario : R.color.primario_suave)));
        t.setTextColor(ContextCompat.getColor(c, elegido ? R.color.blanco : R.color.primario));
        t.setContentDescription(texto + (elegido ? ", elegido" : ""));
        t.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(c, 40));
        lp.setMarginEnd(dp(c, 8));
        t.setLayoutParams(lp);
        return t;
    }

    /** Botón pequeño dentro de una tarjeta. */
    static TextView boton(Context c, String texto, int color, View.OnClickListener click) {
        TextView b = new TextView(c);
        b.setText(texto);
        b.setTextColor(ContextCompat.getColor(c, color));
        b.setTextSize(13);
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(c, 44));
        b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        b.setBackgroundResource(R.drawable.bg_fila);
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(c, 44));
        lp.setMarginEnd(dp(c, 8));
        b.setLayoutParams(lp);
        return b;
    }

    /** Etiqueta + campo de texto dentro de una hoja. */
    static EditText campo(Context c, LinearLayout dentro, String etiqueta, String pista, int tipo, int lineas) {
        TextView e = new TextView(c);
        e.setText(etiqueta);
        e.setTextColor(ContextCompat.getColor(c, R.color.texto));
        e.setTextSize(13);
        e.setTypeface(e.getTypeface(), Typeface.BOLD);
        e.setPadding(0, dp(c, 10), 0, dp(c, 6));
        dentro.addView(e);
        EditText t = new EditText(c);
        t.setHint(pista);
        t.setInputType(tipo | (lineas > 1 ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        t.setMinLines(lineas);
        t.setGravity(lineas > 1 ? Gravity.TOP | Gravity.START : Gravity.CENTER_VERTICAL);
        t.setBackgroundResource(R.drawable.bg_campo);
        t.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        t.setTextSize(16);
        t.setTextColor(ContextCompat.getColor(c, R.color.texto));
        t.setHintTextColor(ContextCompat.getColor(c, R.color.texto_3));
        t.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        t.setMinHeight(dp(c, 52));
        e.setLabelFor(View.generateViewId());
        t.setId(e.getLabelFor());
        dentro.addView(t, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return t;
    }

    static CheckBox casilla(Context c, LinearLayout dentro, String texto) {
        CheckBox cb = new CheckBox(c);
        cb.setText(texto);
        cb.setTextSize(15);
        cb.setMinHeight(dp(c, 48));
        dentro.addView(cb);
        return cb;
    }

    /** Texto de error bajo un formulario. */
    static TextView error(Context c, LinearLayout dentro) {
        TextView t = new TextView(c);
        t.setTextColor(ContextCompat.getColor(c, R.color.peligro));
        t.setTextSize(13);
        t.setPadding(0, dp(c, 8), 0, 0);
        t.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        dentro.addView(t);
        return t;
    }
}
