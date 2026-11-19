package com.example.vecinosotello;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.auth.FirebaseAuth;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mapa;
    private final List<Marker> marcadores = new ArrayList<>();
    private Circle brillo;
    private ValueAnimator animBrillo;
    private LatLngBounds zona;
    private TextView aviso, nuevos;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Chat.Escucha escucha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Datos.miNumero == null) { // la app se cerró y perdió la sesión
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_mapa);

        ((TextView) findViewById(R.id.avatar)).setText(Datos.miNumero);
        ((TextView) findViewById(R.id.total)).setText(Datos.casas.size() + " casas del vecindario");
        aviso = findViewById(R.id.aviso);
        nuevos = findViewById(R.id.nuevos);
        puntoLeyenda(R.id.leyendaVecina, R.color.primario);
        puntoLeyenda(R.id.leyendaMia, R.color.oro);
        puntoLeyenda(R.id.leyendaAlerta, R.color.peligro);

        findViewById(R.id.salir).setOnClickListener(v -> {
            VigilanciaService.detener(this);
            FirebaseAuth.getInstance().signOut();
            Datos.miNumero = null;
            finish();
        });
        findViewById(R.id.irMiCasa).setOnClickListener(v -> irA(Datos.miCasa()));
        findViewById(R.id.abrirChat).setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.bAlerta).setOnClickListener(v -> hojaAlerta());
        findViewById(R.id.bIrCasa).setOnClickListener(v -> hojaIrACasa());
        findViewById(R.id.bEventos).setOnClickListener(v -> hojaEventos());
        findViewById(R.id.bEmergencias).setOnClickListener(v -> hojaEmergencias());
        findViewById(R.id.bMas).setOnClickListener(v -> hojaMas());

        SupportMapFragment f = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        f.getMapAsync(this);

        activarAvisos();
    }

    // ---------------- Avisos de emergencia ----------------

    private final ActivityResultLauncher<String> pedirNotificaciones =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> iniciarVigilancia());

    /** Pide permiso de notificaciones (Android 13+) y deja escuchando las alertas con la app cerrada. */
    private void activarAvisos() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            iniciarVigilancia();
        }
    }

    private void iniciarVigilancia() {
        VigilanciaService.iniciar(this);
        // Sin esto, el ahorro de batería puede cortar la conexión y no llegarían las alertas.
        PowerManager pm = getSystemService(PowerManager.class);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName())));
            } catch (ActivityNotFoundException e) {
                avisoCorto("Desactiva el ahorro de batería para Vecinos Otello en Ajustes");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (nuevos == null) return;
        actualizarChat();
        // Chat en tiempo real: contador de mensajes nuevos y alertas de otros vecinos en el mapa
        escucha = Chat.escuchar(() -> {
            if (Datos.mensajesLeidos < 0) Datos.mensajesLeidos = Datos.mensajes.size(); // lo anterior ya se vio
            actualizarChat();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (escucha != null) escucha.quitar();
        escucha = null;
    }

    private void actualizarChat() {
        int sinLeer = 0; // solo cuentan los mensajes nuevos de otros vecinos
        for (int i = Math.max(0, Datos.mensajesLeidos); i < Datos.mensajes.size(); i++)
            if (!Datos.mensajes.get(i).mio) sinLeer++;
        nuevos.setText(String.valueOf(sinLeer));
        nuevos.setVisibility(sinLeer > 0 ? View.VISIBLE : View.GONE);
        if (mapa != null) dibujarCasas();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animBrillo != null) animBrillo.cancel();
        handler.removeCallbacksAndMessages(null);
    }

    private void puntoLeyenda(int id, int color) {
        GradientDrawable punto = new GradientDrawable();
        punto.setShape(GradientDrawable.OVAL);
        punto.setColor(ContextCompat.getColor(this, color));
        int t = dp(9);
        punto.setSize(t, t);
        ((TextView) findViewById(id)).setCompoundDrawablesRelativeWithIntrinsicBounds(punto, null, null, null);
    }

    // ---------------- Mapa ----------------

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
        mapa.setMapStyle(new MapStyleOptions("[{\"featureType\":\"poi.business\",\"stylers\":[{\"visibility\":\"off\"}]}]"));
        mapa.getUiSettings().setZoomControlsEnabled(true);
        mapa.getUiSettings().setMapToolbarEnabled(false);
        mapa.getUiSettings().setRotateGesturesEnabled(false);
        mapa.getUiSettings().setTiltGesturesEnabled(false);

        // Zona del vecindario (línea punteada)
        List<LatLng> borde = Arrays.asList(
                Datos.mover(Datos.GOYA, 2 * Datos.DLAT, -0.00004), Datos.mover(Datos.ESTE, 2 * Datos.DLAT, 0.00004),
                Datos.mover(Datos.ESTE, -2 * Datos.DLAT, 0.00004), Datos.mover(Datos.CRUCE, -2 * Datos.DLAT, 2 * Datos.DLON),
                Datos.mover(Datos.PINAR, -0.00003, 2 * Datos.DLON), Datos.mover(Datos.PINAR, -0.00003, -2 * Datos.DLON),
                Datos.mover(Datos.CRUCE, -2 * Datos.DLAT, -2 * Datos.DLON), Datos.mover(Datos.GOYA, -2 * Datos.DLAT, -0.00004));
        LatLngBounds.Builder b = LatLngBounds.builder();
        for (LatLng p : borde) b.include(p);
        zona = b.build();
        mapa.addPolygon(new PolygonOptions().addAll(borde)
                .strokeColor(ContextCompat.getColor(this, R.color.primario)).strokeWidth(dp(2))
                .strokePattern(Arrays.asList(new Dash(dp(6)), new Gap(dp(6))))
                .fillColor(Color.argb(13, 37, 99, 235)));

        // Se puede mover y hacer zoom, pero sin salir mucho del vecindario
        mapa.setLatLngBoundsForCameraTarget(agrandar(zona, 0.6));
        mapa.setMinZoomPreference(17f);
        mapa.setMaxZoomPreference(20f);

        // Deja libre la parte de abajo donde está la tarjeta del chat
        View chat = findViewById(R.id.abrirChat);
        chat.post(() -> mapa.setPadding(0, 0, 0, chat.getHeight() + dp(16)));

        mapa.setOnMapLoadedCallback(() -> mapa.moveCamera(CameraUpdateFactory.newLatLngBounds(zona, dp(24))));
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(Datos.entre(Datos.CRUCE, Datos.PINAR, 0.4), 18.5f));
        dibujarCasas();
    }

    private static LatLngBounds agrandar(LatLngBounds z, double f) {
        double dLat = (z.northeast.latitude - z.southwest.latitude) * f;
        double dLon = (z.northeast.longitude - z.southwest.longitude) * f;
        return new LatLngBounds(new LatLng(z.southwest.latitude - dLat, z.southwest.longitude - dLon),
                new LatLng(z.northeast.latitude + dLat, z.northeast.longitude + dLon));
    }

    private void dibujarCasas() {
        for (Marker m : marcadores) m.remove();
        marcadores.clear();
        for (Datos.Casa c : Datos.casas) {
            boolean mia = c.esMia();
            boolean alerta = Datos.alertas.contains(c.n);
            boolean arriba = c.codigo.startsWith("N") && !mia; // fila de arriba: etiqueta encima
            Marcador m = marcador(c, mia, alerta, arriba);
            Marker mk = mapa.addMarker(new MarkerOptions().position(c.pos)
                    .icon(BitmapDescriptorFactory.fromBitmap(m.bitmap)).anchor(m.u, m.v)
                    .zIndex(mia ? 10 : 1)
                    .title((mia ? "Mi casa · " : "") + c.nombre())
                    .snippet(estado(c)));
            if (mk != null) {
                mk.setTag(c.n);
                marcadores.add(mk);
            }
        }
        brilloDorado(Datos.miCasa());
    }

    private String estado(Datos.Casa c) {
        if (Datos.alertas.contains(c.n)) return "Con alerta";
        if (Datos.vacaciones.contains(c.n)) return "De vacaciones";
        return "Tranquila";
    }

    /** Halo dorado que late alrededor de mi casa. */
    private void brilloDorado(Datos.Casa mia) {
        if (animBrillo != null) animBrillo.cancel();
        if (brillo != null) brillo.remove();
        if (mia == null) return;
        brillo = mapa.addCircle(new CircleOptions().center(mia.pos).radius(4).strokeWidth(0)
                .fillColor(Color.argb(90, 255, 215, 0)).zIndex(0));
        animBrillo = ValueAnimator.ofFloat(0f, 1f);
        animBrillo.setDuration(1600);
        animBrillo.setRepeatCount(ValueAnimator.INFINITE);
        animBrillo.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            brillo.setRadius(3 + 7 * t);
            brillo.setFillColor(Color.argb((int) (120 * (1 - t)), 255, 215, 0));
        });
        animBrillo.start();
    }

    private static final class Marcador {
        Bitmap bitmap;
        float u, v;
    }

    /** Dibuja el punto de la casa con su etiqueta (número o código). */
    private Marcador marcador(Datos.Casa c, boolean mia, boolean alerta, boolean arriba) {
        float d = getResources().getDisplayMetrics().density;
        float diam = (mia ? 32 : 18) * d, borde = (mia && alerta ? 4 : 2.5f) * d, sombra = 3 * d;
        String texto = mia ? "Mi casa " + c.num : c.etiqueta();

        Paint pTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTexto.setTextSize((mia ? 12 : 10) * getResources().getDisplayMetrics().scaledDensity);
        pTexto.setFakeBoldText(true);
        pTexto.setColor(ContextCompat.getColor(this, mia ? R.color.oro_osc : R.color.texto));
        float pad = 5 * d, etW = pTexto.measureText(texto) + 2 * pad, etH = pTexto.getTextSize() + 2 * pad * 0.7f;
        float sep = 4 * d, punto = diam + 2 * sombra;

        float w = arriba ? Math.max(punto, etW) : punto + sep + etW;
        float h = arriba ? etH + sep + punto : Math.max(punto, etH);
        Bitmap bmp = Bitmap.createBitmap((int) Math.ceil(w), (int) Math.ceil(h), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);

        float cx = arriba ? w / 2 : punto / 2, cy = arriba ? etH + sep + punto / 2 : h / 2;
        // Punto
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setShadowLayer(sombra, 0, d, Color.argb(70, 15, 23, 42));
        cv.drawCircle(cx, cy, diam / 2, p);
        p.clearShadowLayer();
        if (mia && alerta) {
            p.setColor(ContextCompat.getColor(this, R.color.peligro));
            cv.drawCircle(cx, cy, diam / 2, p);
        }
        if (mia) {
            p.setShader(new RadialGradient(cx - diam * 0.15f, cy - diam * 0.15f, diam / 2,
                    new int[]{0xFFFFF6C7, 0xFFF5B800, 0xFFB8860B}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        } else {
            p.setColor(ContextCompat.getColor(this, alerta ? R.color.peligro : R.color.primario));
        }
        cv.drawCircle(cx, cy, diam / 2 - borde, p);
        p.setShader(null);
        if (mia) {
            Drawable casa = ContextCompat.getDrawable(this, R.drawable.ic_casa);
            if (casa != null) {
                casa = casa.mutate();
                casa.setTint(0xFF5C4100);
                int r = (int) (diam * 0.26f);
                casa.setBounds((int) cx - r, (int) cy - r, (int) cx + r, (int) cy + r);
                casa.draw(cv);
            }
        }

        // Etiqueta
        float ex = arriba ? (w - etW) / 2 : punto + sep, ey = arriba ? 0 : (h - etH) / 2;
        RectF r = new RectF(ex, ey, ex + etW, ey + etH);
        Paint pf = new Paint(Paint.ANTI_ALIAS_FLAG);
        pf.setColor(ContextCompat.getColor(this, mia ? R.color.oro_suave : R.color.blanco));
        cv.drawRoundRect(r, 7 * d, 7 * d, pf);
        pf.setStyle(Paint.Style.STROKE);
        pf.setStrokeWidth(d);
        pf.setColor(ContextCompat.getColor(this, mia ? R.color.oro : R.color.borde));
        cv.drawRoundRect(r, 7 * d, 7 * d, pf);
        cv.drawText(texto, ex + pad, ey + etH / 2 - (pTexto.descent() + pTexto.ascent()) / 2, pTexto);

        Marcador m = new Marcador();
        m.bitmap = bmp;
        m.u = cx / w;
        m.v = cy / h;
        return m;
    }

    private void irA(Datos.Casa c) {
        if (c == null || mapa == null) return;
        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(c.pos, 20f));
        for (Marker m : marcadores)
            if (Integer.valueOf(c.n).equals(m.getTag())) m.showInfoWindow();
    }

    // ---------------- Botones ----------------

    private void hojaAlerta() {
        Hoja h = new Hoja(this, R.drawable.ic_sirena, Hoja.Tono.ROJO, "Activar alerta", "Avisa a todo el vecindario al instante.");
        h.textoCerrar("Cancelar");
        GridLayout g = h.cuadricula();
        Object[][] tipos = {
                {R.drawable.ic_salvavidas, "Necesito ayuda"}, {R.drawable.ic_sospechoso, "Persona sospechosa"},
                {R.drawable.ic_auto, "Auto sospechoso"}, {R.drawable.ic_candado, "Robo o intento de robo"},
                {R.drawable.ic_fuego, "Incendio o humo"}, {R.drawable.ic_pulso, "Emergencia médica"}};
        for (Object[] t : tipos) {
            String tipo = (String) t[1];
            h.opcion(g, (int) t[0], Hoja.Tono.ROJO, tipo, v -> {
                Datos.enviar(tipo + " en el N° " + Datos.miNumero, true);
                Datos.eventos.add(0, new Datos.Evento(tipo, "N° " + Datos.miNumero + " · " + Datos.hora(), "alerta"));
                dibujarCasas();
                mostrarAviso(tipo + " · alerta enviada al vecindario");
                h.cerrar();
            });
        }
        h.mostrar();
    }

    private void mostrarAviso(String texto) {
        aviso.setText(texto);
        aviso.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> aviso.setVisibility(View.GONE), 8000);
    }

    private void hojaIrACasa() {
        Datos.Casa mia = Datos.miCasa();
        LatLng destino = mia != null ? mia.pos : Datos.CRUCE;
        String ll = destino.latitude + "," + destino.longitude;
        Hoja h = new Hoja(this, R.drawable.ic_casa, Hoja.Tono.AZUL, "Llegar a mi casa",
                "Destino: Pje. Otello " + Datos.miNumero + ", San Joaquín");
        Hoja.flecha(h.fila(R.drawable.ic_mapa, Hoja.Tono.VERDE, "Google Maps", "Ruta en auto hasta tu casa",
                v -> abrir("https://www.google.com/maps/dir/?api=1&destination=" + ll + "&travelmode=driving")));
        Hoja.flecha(h.fila(R.drawable.ic_navegar, Hoja.Tono.AZUL, "Waze", "Navegación con tráfico en vivo",
                v -> abrir("https://waze.com/ul?ll=" + ll + "&navigate=yes")));
        h.fila(R.drawable.ic_ojo, Hoja.Tono.AMBAR, "Avisar que voy llegando", "Tus vecinos estarán atentos", v -> {
            Datos.enviar("Voy llegando a casa, ¿me echan un ojo?", false);
            avisoCorto("Aviso enviado al chat");
            h.cerrar();
        });
        h.fila(R.drawable.ic_check, Hoja.Tono.VERDE, "Avisar que llegué bien", "Tranquiliza a tus vecinos", v -> {
            Datos.enviar("Llegué bien a casa, gracias vecinos", false);
            avisoCorto("Aviso enviado al chat");
            h.cerrar();
        });
        h.mostrar();
    }

    private void abrir(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            avisoCorto("No hay una app para abrir el enlace");
        }
    }

    private void avisoCorto(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }

    private void hojaEventos() {
        Hoja h = new Hoja(this, R.drawable.ic_calendario, Hoja.Tono.AZUL, "Eventos del barrio", "Lo que pasa en el pasaje y alrededores.");
        for (Datos.Evento e : Datos.eventos) {
            int icono = R.drawable.ic_calendario;
            Hoja.Tono tono = Hoja.Tono.AZUL;
            if ("alerta".equals(e.tipo)) { icono = R.drawable.ic_sirena; tono = Hoja.Tono.ROJO; }
            else if ("reporte".equals(e.tipo)) { icono = R.drawable.ic_llave; tono = Hoja.Tono.AMBAR; }
            else if ("agua".equals(e.tipo)) { icono = R.drawable.ic_gota; }
            h.fila(icono, tono, e.titulo, e.detalle, null);
        }
        // Agregar un evento
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        EditText texto = new EditText(this);
        texto.setHint("Agregar evento o aviso…");
        texto.setBackgroundResource(R.drawable.bg_input_chat);
        texto.setPadding(dp(16), 0, dp(16), 0);
        texto.setTextSize(15);
        texto.setSingleLine(true);
        texto.setImeOptions(EditorInfo.IME_ACTION_DONE);
        fila.addView(texto, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView agregar = new TextView(this);
        agregar.setText("Agregar");
        agregar.setTextColor(Color.WHITE);
        agregar.setTypeface(agregar.getTypeface(), android.graphics.Typeface.BOLD);
        agregar.setGravity(android.view.Gravity.CENTER);
        agregar.setPadding(dp(16), 0, dp(16), 0);
        agregar.setBackgroundResource(R.drawable.bg_chat_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(48));
        lp.setMarginStart(dp(8));
        fila.addView(agregar, lp);
        h.contenido.addView(fila);
        agregar.setOnClickListener(v -> {
            String t = texto.getText().toString().trim();
            if (t.isEmpty()) return;
            Datos.eventos.add(0, new Datos.Evento(t, "Hoy " + Datos.hora() + " · N° " + Datos.miNumero, null));
            h.cerrar();
            hojaEventos();
        });
        h.mostrar();
    }

    private void hojaEmergencias() {
        Hoja h = new Hoja(this, R.drawable.ic_telefono, Hoja.Tono.ROJO, "Números de emergencia", "Toca para llamar.");
        Object[][] nums = {
                {R.drawable.ic_escudo, Hoja.Tono.AZUL, "Carabineros", "Delitos y seguridad", "133"},
                {R.drawable.ic_pulso, Hoja.Tono.ROJO, "Ambulancia SAMU", "Urgencias médicas", "131"},
                {R.drawable.ic_fuego, Hoja.Tono.AMBAR, "Bomberos", "Incendios y rescates", "132"},
                {R.drawable.ic_lupa, Hoja.Tono.AZUL, "PDI", "Policía de Investigaciones", "134"}};
        for (Object[] n : nums) {
            String numero = (String) n[4];
            View f = h.fila((int) n[0], (Hoja.Tono) n[1], (String) n[2], (String) n[3],
                    v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + numero))));
            Hoja.derecha(f, numero, R.color.peligro);
        }
        h.mostrar();
    }

    private void hojaMas() {
        Hoja h = new Hoja(this, R.drawable.ic_cuadros, Hoja.Tono.AZUL, "Más funciones", "Herramientas para cuidar el pasaje entre todos.");
        Hoja.flecha(h.fila(R.drawable.ic_vecinos, Hoja.Tono.AZUL, "Directorio de vecinos", "Todas las casas y su estado",
                v -> { h.cerrar(); hojaDirectorio(); }));
        Hoja.flecha(h.fila(R.drawable.ic_llave, Hoja.Tono.AMBAR, "Reportar un problema", "Luminaria, basura, mascota perdida…",
                v -> { h.cerrar(); hojaReporte(); }));
        Datos.Casa mia = Datos.miCasa();
        boolean deVacaciones = mia != null && Datos.vacaciones.contains(mia.n);
        h.fila(R.drawable.ic_sol, Hoja.Tono.ORO, deVacaciones ? "Modo vacaciones: activo" : "Modo vacaciones",
                deVacaciones ? "Toca para avisar que volviste" : "Pide a tus vecinos que vigilen tu casa", v -> {
                    if (mia == null) return;
                    if (deVacaciones) {
                        Datos.vacaciones.remove(mia.n);
                        Datos.enviar("Ya volví, gracias por cuidar mi casa", false);
                    } else {
                        Datos.vacaciones.add(mia.n);
                        Datos.enviar("Estaré fuera unos días, ¿me ayudan a vigilar mi casa?", false);
                    }
                    avisoCorto("Aviso enviado al chat");
                    dibujarCasas();
                    h.cerrar();
                });
        Hoja.flecha(h.fila(R.drawable.ic_linterna, Hoja.Tono.VERDE, "Ronda vecinal", "Turnos para vigilar el pasaje",
                v -> { h.cerrar(); hojaRonda(); }));
        h.mostrar();
    }

    private void hojaDirectorio() {
        Hoja h = new Hoja(this, R.drawable.ic_vecinos, Hoja.Tono.AZUL, "Directorio de vecinos", "Toca una casa para verla en el mapa.");
        for (Datos.Casa c : Datos.casas) {
            String est = estado(c);
            int color = Datos.alertas.contains(c.n) ? R.color.peligro
                    : Datos.vacaciones.contains(c.n) ? R.color.primario : R.color.ok;
            View f = h.fila(c.esMia() ? R.drawable.ic_casa : R.drawable.ic_casa, c.esMia() ? Hoja.Tono.ORO : Hoja.Tono.AZUL,
                    c.etiqueta() + (c.esMia() ? " · tú" : ""), c.num == null ? "Número por confirmar" : null,
                    v -> { h.cerrar(); irA(c); });
            Hoja.derecha(f, est, color);
            ((TextView) f.findViewById(R.id.filaDerecha)).setTextSize(12);
        }
        h.mostrar();
    }

    private void hojaReporte() {
        Hoja h = new Hoja(this, R.drawable.ic_llave, Hoja.Tono.AMBAR, "Reportar un problema", "Queda en Eventos y se avisa en el chat.");
        h.textoCerrar("Cancelar");
        GridLayout g = h.cuadricula();
        Object[][] tipos = {
                {R.drawable.ic_ampolleta, "Luminaria apagada"}, {R.drawable.ic_basura, "Basura acumulada"},
                {R.drawable.ic_hoyo, "Hoyo en la calle"}, {R.drawable.ic_peligro, "Perro suelto"},
                {R.drawable.ic_huella, "Mascota perdida"}, {R.drawable.ic_volumen, "Ruidos molestos"}};
        for (Object[] t : tipos) {
            String tipo = (String) t[1];
            h.opcion(g, (int) t[0], Hoja.Tono.AMBAR, tipo, v -> {
                Datos.eventos.add(0, new Datos.Evento(tipo, "Hoy " + Datos.hora() + " · reportado por N° " + Datos.miNumero, "reporte"));
                Datos.enviar("Reporte: " + tipo, false);
                avisoCorto("Reporte enviado");
                h.cerrar();
            });
        }
        h.mostrar();
    }

    private void hojaRonda() {
        Hoja h = new Hoja(this, R.drawable.ic_linterna, Hoja.Tono.VERDE, "Ronda vecinal", "Anótate en un turno para recorrer el pasaje.");
        for (Datos.Turno t : Datos.turnos) {
            boolean libre = t.quien == null;
            View f = h.fila(R.drawable.ic_linterna, libre ? Hoja.Tono.VERDE : Hoja.Tono.AZUL, t.dia + " · " + t.hora,
                    libre ? "Libre" : t.quien, libre ? v -> {
                        t.quien = "N° " + Datos.miNumero;
                        Datos.enviar("Me anoté en la ronda de " + t.dia.toLowerCase() + " " + t.hora, false);
                        h.cerrar();
                        hojaRonda();
                    } : null);
            if (libre) Hoja.derecha(f, "Me anoto", R.color.primario);
            if (libre) ((TextView) f.findViewById(R.id.filaDerecha)).setTextSize(13);
        }
        h.mostrar();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
