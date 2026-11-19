package com.example.vecinosotello;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/** Hoja que sube desde abajo con título, filas u opciones y botón Cerrar. */
final class Hoja {

    /** Color del círculo del ícono: fondo suave + color del ícono. */
    enum Tono {
        AZUL(R.color.primario_suave, R.color.primario),
        ROJO(R.color.peligro_suave, R.color.peligro),
        AMBAR(R.color.aviso_suave, R.color.aviso),
        VERDE(R.color.ok_suave, R.color.ok),
        ORO(R.color.oro_suave, R.color.oro_osc);

        final int fondo, icono;

        Tono(int fondo, int icono) {
            this.fondo = fondo;
            this.icono = icono;
        }
    }

    private final Context ctx;
    private final LayoutInflater inflater;
    final BottomSheetDialog dialogo;
    final LinearLayout contenido;

    Hoja(Context ctx, int icono, Tono tono, String titulo, String sub) {
        this.ctx = ctx;
        inflater = LayoutInflater.from(ctx);
        dialogo = new BottomSheetDialog(ctx);
        View v = inflater.inflate(R.layout.hoja, null);
        pintar(v.findViewById(R.id.hojaChip), v.findViewById(R.id.hojaIcono), icono, tono);
        ((TextView) v.findViewById(R.id.hojaTitulo)).setText(titulo);
        ((TextView) v.findViewById(R.id.hojaSub)).setText(sub);
        contenido = v.findViewById(R.id.hojaContenido);
        v.findViewById(R.id.hojaCerrar).setOnClickListener(x -> dialogo.dismiss());
        dialogo.setContentView(v);
        dialogo.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialogo.getBehavior().setSkipCollapsed(true);
    }

    void textoCerrar(String texto) {
        ((TextView) dialogo.findViewById(R.id.hojaCerrar)).setText(texto);
    }

    private void pintar(View chip, ImageView img, int icono, Tono tono) {
        chip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, tono.fondo)));
        img.setImageResource(icono);
        img.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, tono.icono)));
    }

    /** Agrega una fila. Si click es null, la fila no se puede tocar. */
    View fila(int icono, Tono tono, String titulo, String detalle, View.OnClickListener click) {
        View f = inflater.inflate(R.layout.item_fila, contenido, false);
        pintar(f.findViewById(R.id.filaChip), f.findViewById(R.id.filaIcono), icono, tono);
        ((TextView) f.findViewById(R.id.filaTitulo)).setText(titulo);
        TextView det = f.findViewById(R.id.filaDetalle);
        det.setText(detalle);
        det.setVisibility(detalle == null ? View.GONE : View.VISIBLE);
        if (click != null) f.setOnClickListener(click);
        else f.setClickable(false);
        contenido.addView(f);
        return f;
    }

    /** Muestra la flecha ">" a la derecha de una fila. */
    static void flecha(View fila) {
        fila.findViewById(R.id.filaFlecha).setVisibility(View.VISIBLE);
    }

    /** Muestra un texto a la derecha de una fila (un número o un estado). */
    static void derecha(View fila, String texto, int color) {
        TextView d = fila.findViewById(R.id.filaDerecha);
        d.setText(texto);
        d.setTextColor(ContextCompat.getColor(fila.getContext(), color));
        d.setVisibility(View.VISIBLE);
    }

    /** Cuadrícula de 2 columnas para opciones. */
    GridLayout cuadricula() {
        GridLayout g = new GridLayout(ctx);
        g.setColumnCount(2);
        g.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        contenido.addView(g);
        return g;
    }

    void opcion(GridLayout g, int icono, Tono tono, String texto, View.OnClickListener click) {
        View o = inflater.inflate(R.layout.item_opcion, g, false);
        pintar(o.findViewById(R.id.opcionChip), o.findViewById(R.id.opcionIcono), icono, tono);
        ((TextView) o.findViewById(R.id.opcionTexto)).setText(texto);
        o.setOnClickListener(click);
        GridLayout.LayoutParams p = (GridLayout.LayoutParams) o.getLayoutParams();
        p.width = 0;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        g.addView(o, p);
    }

    void mostrar() {
        dialogo.show();
    }

    void cerrar() {
        dialogo.dismiss();
    }
}
