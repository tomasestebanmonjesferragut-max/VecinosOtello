package com.example.vecinosotello;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Canal de anuncios oficiales (nodo "anuncios"): todos leen, solo el administrador publica.
 * La restricción real está en database.rules.json (la app solo oculta el botón).
 * La notificación FCM la envía el notificador (entorno de confianza), no la app.
 */
public final class Anuncios {

    public static final int LIMITE = 50;

    public static final class Anuncio {
        public final String id, titulo, contenido, autor;
        public final Long fecha;
        public final boolean importante;

        Anuncio(String id, String titulo, String contenido, String autor, Long fecha, boolean importante) {
            this.id = id;
            this.titulo = titulo;
            this.contenido = contenido;
            this.autor = autor;
            this.fecha = fecha;
            this.importante = importante;
        }
    }

    public interface Cambio { void alCambiar(List<Anuncio> anuncios); }

    private Anuncios() { }

    static DatabaseReference ref() { return Firebase.db().getReference("anuncios"); }

    /** Publica un anuncio (si quien lo intenta no es admin, Firebase lo rechaza). */
    public static String publicar(String titulo, String contenido, boolean importante, Ventas.Listo listo) {
        String e = Validaciones.validarAnuncio(titulo, contenido);
        if (e != null) return e;
        Map<String, Object> a = new HashMap<>();
        a.put("titulo", titulo.trim());
        a.put("contenido", contenido.trim());
        a.put("autor", Sesion.identidad());
        a.put("uid", Sesion.uid);
        a.put("importante", importante);
        a.put("fecha", ServerValue.TIMESTAMP);
        ref().push().setValue(a).addOnCompleteListener(t -> listo.listo(t.isSuccessful() ? null
                : "Firebase rechazó el anuncio. Solo el administrador puede publicar."));
        return null;
    }

    /** Importantes primero; dentro de cada grupo, los más nuevos arriba. */
    public static List<Anuncio> ordenar(List<Anuncio> lista) {
        List<Anuncio> r = new ArrayList<>(lista);
        Collections.sort(r, (a, b) -> {
            if (a.importante != b.importante) return a.importante ? -1 : 1;
            long fa = a.fecha == null ? Long.MAX_VALUE : a.fecha, fb = b.fecha == null ? Long.MAX_VALUE : b.fecha;
            return Long.compare(fb, fa);
        });
        return r;
    }

    public static Chat.Escucha escuchar(Cambio cambio, Chat.Error error) {
        Query q = ref().orderByChild("fecha").limitToLast(LIMITE);
        ValueEventListener l = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                List<Anuncio> lista = new ArrayList<>();
                for (DataSnapshot d : snap.getChildren()) {
                    String titulo = d.child("titulo").getValue(String.class);
                    String contenido = d.child("contenido").getValue(String.class);
                    if (titulo == null || contenido == null) continue;
                    lista.add(new Anuncio(d.getKey(), titulo, contenido, d.child("autor").getValue(String.class),
                            d.child("fecha").getValue(Long.class), Boolean.TRUE.equals(d.child("importante").getValue(Boolean.class))));
                }
                cambio.alCambiar(ordenar(lista));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                error.alFallar("No se pudieron leer los anuncios: " + e.getMessage());
            }
        };
        q.addValueEventListener(l);
        return () -> q.removeEventListener(l);
    }
}
