package com.example.vecinosotello;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tienda del pasaje: ofrecer productos o servicios, calificarlos con estrellas y marcarlos como no disponibles. */
public class VentasActivity extends AppCompatActivity {

    private LinearLayout contenido, filtros;
    private TextView error;
    private View cargando;
    private Chat.Escucha escucha, escuchaEstrellas;
    private Map<String, Map<String, Long>> estrellas = new HashMap<>();
    private List<Ventas.Venta> todas = new ArrayList<>();
    private String categoria; // null = todas
    private boolean soloDisponibles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sesion.activa()) { finish(); return; }
        setContentView(R.layout.activity_lista);
        ((TextView) findViewById(R.id.titulo)).setText("Tienda del pasaje");
        ((TextView) findViewById(R.id.subtitulo)).setText("Productos y servicios entre vecinos");
        contenido = findViewById(R.id.contenido);
        filtros = findViewById(R.id.filtros);
        error = findViewById(R.id.error);
        cargando = findViewById(R.id.cargando);
        findViewById(R.id.filtrosScroll).setVisibility(View.VISIBLE);
        findViewById(R.id.volver).setOnClickListener(v -> finish());
        TextView publicar = findViewById(R.id.publicar);
        publicar.setText("Ofrecer algo");
        publicar.setVisibility(View.VISIBLE);
        publicar.setOnClickListener(v -> hojaPublicar());
        pintarFiltros();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (contenido != null) escuchar();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (escucha != null) escucha.quitar();
        if (escuchaEstrellas != null) escuchaEstrellas.quitar();
        escucha = escuchaEstrellas = null;
    }

    private void escuchar() {
        if (escucha != null) escucha.quitar();
        cargando.setVisibility(View.VISIBLE);
        if (escuchaEstrellas == null) escuchaEstrellas = Ventas.escucharEstrellas(e -> { estrellas = e; pintar(); });
        escucha = Ventas.escuchar(ventas -> {
            todas = ventas;
            cargando.setVisibility(View.GONE);
            error.setVisibility(View.GONE);
            pintar();
        }, e -> {
            cargando.setVisibility(View.GONE);
            error.setText(e + " Toca aquí para reintentar.");
            error.setVisibility(View.VISIBLE);
            error.setOnClickListener(v -> escuchar());
        });
    }

    private void pintarFiltros() {
        filtros.removeAllViews();
        filtros.addView(Ui.chip(this, "Todas", categoria == null, v -> { categoria = null; pintarFiltros(); pintar(); }));
        for (String c : Validaciones.CATEGORIAS)
            filtros.addView(Ui.chip(this, c, c.equals(categoria), v -> { categoria = c; pintarFiltros(); pintar(); }));
        filtros.addView(Ui.chip(this, "Solo disponibles", soloDisponibles,
                v -> { soloDisponibles = !soloDisponibles; pintarFiltros(); pintar(); }));
    }

    private void pintar() {
        contenido.removeAllViews();
        List<Ventas.Venta> lista = Ventas.filtrar(todas, categoria, soloDisponibles);
        if (lista.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText(todas.isEmpty() ? "Aún no hay productos ni servicios. ¡Ofrece el primero!" : "No hay nada con ese filtro.");
            vacio.setTextColor(ContextCompat.getColor(this, R.color.texto_2));
            vacio.setPadding(0, Ui.dp(this, 24), 0, 0);
            contenido.addView(vacio);
            return;
        }
        for (Ventas.Venta v : lista) contenido.addView(tarjeta(v));
    }

    private View tarjeta(Ventas.Venta v) {
        View t = LayoutInflater.from(this).inflate(R.layout.item_tarjeta, contenido, false);
        TextView etiqueta = t.findViewById(R.id.etiqueta);
        etiqueta.setText(v.vendido ? "No disponible" : v.tipo + " · " + v.categoria);
        etiqueta.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                v.vendido ? R.color.borde : R.color.ok_suave)));
        etiqueta.setTextColor(ContextCompat.getColor(this, v.vendido ? R.color.texto_2 : R.color.ok));
        TextView precio = t.findViewById(R.id.derecha);
        precio.setText(Validaciones.precioBonito(v.precio));
        ((TextView) t.findViewById(R.id.titulo)).setText(v.titulo);
        TextView texto = t.findViewById(R.id.texto);
        texto.setText(v.descripcion);
        texto.setVisibility(v.descripcion == null || v.descripcion.isEmpty() ? View.GONE : View.VISIBLE);
        ((TextView) t.findViewById(R.id.pie)).setText(Validaciones.identidad(v.nombre, v.vivienda) + " · " + Fechas.corta(v.fecha));
        if (v.vendido) t.setAlpha(0.65f);
        LinearLayout acciones = t.findViewById(R.id.acciones);
        acciones.setVisibility(View.VISIBLE);
        acciones.setOrientation(LinearLayout.VERTICAL);
        Map<String, Long> deEsta = estrellas.containsKey(v.id) ? estrellas.get(v.id) : new HashMap<>();
        acciones.addView(filaEstrellas(v, deEsta));
        if (v.esMia()) {
            acciones.addView(Ui.boton(this, v.vendido ? "Volver a disponible" : "Marcar como no disponible", R.color.primario,
                    b -> Ventas.marcarVendido(v, !v.vendido, e -> { if (e != null) aviso(e); })));
        }
        t.setContentDescription(v.titulo + ", " + Validaciones.precioBonito(v.precio) + ", " + (v.vendido ? "no disponible" : v.tipo)
                + ", " + Ventas.textoEstrellas(deEsta.values())
                + ", vende " + Validaciones.identidad(v.nombre, v.vivienda));
        return t;
    }

    /** Promedio de estrellas y, si no es tuyo, 5 estrellas para calificar (se marca la que diste). */
    private View filaEstrellas(Ventas.Venta v, Map<String, Long> deEsta) {
        LinearLayout fila = new LinearLayout(this);
        fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView resumen = new TextView(this);
        resumen.setText(Ventas.textoEstrellas(deEsta.values()));
        resumen.setTextColor(ContextCompat.getColor(this, deEsta.isEmpty() ? R.color.texto_2 : R.color.oro_osc));
        resumen.setTypeface(resumen.getTypeface(), android.graphics.Typeface.BOLD);
        resumen.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        fila.addView(resumen);
        if (v.esMia()) return fila;
        Long mia = deEsta.get(Sesion.uid);
        for (int i = 1; i <= 5; i++) {
            int n = i;
            boolean llena = mia != null && n <= mia;
            TextView estrella = new TextView(this);
            estrella.setText(llena ? "★" : "☆");
            estrella.setTextSize(24);
            estrella.setTextColor(ContextCompat.getColor(this, llena ? R.color.oro : R.color.texto_3));
            estrella.setGravity(android.view.Gravity.CENTER);
            estrella.setMinWidth(Ui.dp(this, 40));
            estrella.setMinHeight(Ui.dp(this, 44));
            estrella.setContentDescription("Calificar con " + n + (n == 1 ? " estrella" : " estrellas"));
            estrella.setOnClickListener(b -> Ventas.calificar(v, n, e -> aviso(e == null ? "Calificaste con " + n + " ★" : e)));
            fila.addView(estrella);
        }
        return fila;
    }

    private void hojaPublicar() {
        Hoja h = new Hoja(this, R.drawable.ic_mas, Hoja.Tono.VERDE, "Ofrecer algo", "Tus vecinos verán tu nombre y vivienda.");
        h.textoCerrar("Cancelar");
        String[] tipo = {null};
        LinearLayout tipos = new LinearLayout(this);
        h.contenido.addView(tipos);
        Runnable[] pintarTipos = new Runnable[1];
        pintarTipos[0] = () -> {
            tipos.removeAllViews();
            for (String x : Validaciones.TIPOS)
                tipos.addView(Ui.chip(this, x, x.equals(tipo[0]), c -> { tipo[0] = x; pintarTipos[0].run(); }));
        };
        pintarTipos[0].run();
        EditText titulo = Ui.campo(this, h.contenido, "¿Qué ofreces?", "Ej. Empanadas de pino o Corte de pelo",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 1);
        EditText precio = Ui.campo(this, h.contenido, "Precio en pesos", "Ej. 2500", InputType.TYPE_CLASS_NUMBER, 1);
        TextView etCat = new TextView(this);
        etCat.setText("Categoría");
        etCat.setTextColor(ContextCompat.getColor(this, R.color.texto));
        etCat.setTypeface(etCat.getTypeface(), android.graphics.Typeface.BOLD);
        etCat.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 6));
        h.contenido.addView(etCat);
        android.widget.HorizontalScrollView sc = new android.widget.HorizontalScrollView(this);
        sc.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        sc.addView(chips);
        h.contenido.addView(sc);
        String[] elegida = {null};
        Runnable[] pintarChips = new Runnable[1];
        pintarChips[0] = () -> {
            chips.removeAllViews();
            for (String c : Validaciones.CATEGORIAS)
                chips.addView(Ui.chip(this, c, c.equals(elegida[0]), x -> { elegida[0] = c; pintarChips[0].run(); }));
        };
        pintarChips[0].run();
        EditText desc = Ui.campo(this, h.contenido, "Descripción (opcional)", "Ej. Los viernes, por encargo",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 3);
        TextView err = Ui.error(this, h.contenido);
        TextView enviar = Ui.boton(this, "Publicar", R.color.blanco, null);
        enviar.setBackgroundResource(R.drawable.bg_chat_card);
        enviar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        h.contenido.addView(enviar);
        enviar.setOnClickListener(x -> {
            enviar.setEnabled(false);
            enviar.setText("Publicando…");
            String e = Ventas.publicar(tipo[0], titulo.getText().toString(), precio.getText().toString(), elegida[0],
                    desc.getText().toString(), fallo -> {
                        if (fallo == null) { h.cerrar(); aviso("Publicado en la tienda"); }
                        else { err.setText(fallo); enviar.setEnabled(true); enviar.setText("Reintentar"); }
                    });
            if (e != null) { err.setText(e); enviar.setEnabled(true); enviar.setText("Publicar"); }
        });
        h.mostrar();
    }

    private void aviso(String t) { Toast.makeText(this, t, Toast.LENGTH_SHORT).show(); }
}
