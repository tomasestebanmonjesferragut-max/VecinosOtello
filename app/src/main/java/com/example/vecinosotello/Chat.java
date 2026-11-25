package com.example.vecinosotello;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chat general en Realtime Database (nodo "mensajes").
 * Cada mensaje guarda quién lo envió (uid, nombre y vivienda), el texto y la fecha del servidor.
 * El historial se pide por páginas (los últimos N) para no descargar todo cada vez.
 */
public final class Chat {

    public static final int PAGINA = 30;

    public static final class Mensaje {
        public final String id, uid, nombre, vivienda, texto;
        public final Long fecha; // null mientras el servidor no confirma
        public final boolean mio;

        Mensaje(String id, String uid, String nombre, String vivienda, String texto, Long fecha, boolean mio) {
            this.id = id;
            this.uid = uid;
            this.nombre = nombre;
            this.vivienda = vivienda;
            this.texto = texto;
            this.fecha = fecha;
            this.mio = mio;
        }

        public String autor() { return Validaciones.identidad(nombre, vivienda); }

        /** true mientras el servidor no confirma que recibió el mensaje (sin conexión, por ejemplo). */
        public boolean pendiente() { return pendientes.contains(id); }
    }

    public interface Cambio { void alCambiar(List<Mensaje> mensajes, boolean hayMas); }

    public interface Error { void alFallar(String mensaje); }

    /** Para dejar de escuchar al salir de una pantalla (evita listeners duplicados). */
    public interface Escucha { void quitar(); }

    /** Mensajes enviados desde este teléfono que el servidor aún no confirma. */
    private static final Set<String> pendientes = new HashSet<>();

    private Chat() { }

    static DatabaseReference ref() {
        DatabaseReference r = Firebase.db().getReference("mensajes");
        r.keepSynced(true);
        return r;
    }

    /**
     * Publica un mensaje con la identidad del usuario. Devuelve el error de validación o null.
     * alTerminar se llama cuando el servidor confirma (o rechaza) para quitar el "Enviando…".
     */
    public static String enviar(String texto, Runnable alTerminar, Error alFallar) {
        String e = Validaciones.validarMensaje(texto);
        if (e != null) return e;
        DatabaseReference nuevo = ref().push();
        Map<String, Object> m = new HashMap<>();
        m.put("uid", Sesion.uid);
        m.put("nombre", Sesion.nombre);
        m.put("vivienda", Sesion.vivienda);
        m.put("texto", texto.trim());
        m.put("fecha", ServerValue.TIMESTAMP);
        pendientes.add(nuevo.getKey());
        nuevo.setValue(m).addOnCompleteListener(t -> {
            pendientes.remove(nuevo.getKey());
            if (!t.isSuccessful() && alFallar != null) alFallar.alFallar("No se pudo enviar el mensaje.");
            if (alTerminar != null) alTerminar.run();
        });
        return null;
    }

    /** Escucha los últimos {@code limite} mensajes en tiempo real. */
    public static Escucha escuchar(int limite, Cambio cambio, Error error) {
        Query q = ref().orderByChild("fecha").limitToLast(limite);
        ValueEventListener l = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                List<Mensaje> lista = new ArrayList<>();
                for (DataSnapshot d : snap.getChildren()) {
                    String uid = d.child("uid").getValue(String.class);
                    String nombre = d.child("nombre").getValue(String.class);
                    String vivienda = d.child("vivienda").getValue(String.class);
                    String texto = d.child("texto").getValue(String.class);
                    if (nombre == null || vivienda == null || texto == null) continue;
                    lista.add(new Mensaje(d.getKey(), uid, nombre, vivienda, texto, d.child("fecha").getValue(Long.class),
                            uid != null && uid.equals(Sesion.uid)));
                }
                cambio.alCambiar(lista, snap.getChildrenCount() >= limite);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                if (error != null) error.alFallar("No se pudo leer el chat: " + e.getMessage());
            }
        };
        q.addValueEventListener(l);
        return () -> q.removeEventListener(l);
    }
}
