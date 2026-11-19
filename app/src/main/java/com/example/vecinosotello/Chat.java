package com.example.vecinosotello;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Chat del vecindario en Firebase Realtime Database (nodo "mensajes").
 * Los mensajes llegan al instante y quedan guardados en la nube y en el teléfono (sin conexión).
 */
final class Chat {

    interface Cambio { void alCambiar(); }

    /** Para dejar de escuchar al salir de una pantalla. */
    interface Escucha { void quitar(); }

    private static final int MAXIMO = 200; // últimos mensajes que se muestran
    private static final String URL = "https://vecinosotello-default-rtdb.firebaseio.com"; // us-central1
    private static DatabaseReference ref;

    private Chat() { }

    static DatabaseReference mensajes() {
        if (ref == null) {
            FirebaseDatabase db = FirebaseDatabase.getInstance(URL);
            db.setPersistenceEnabled(true); // guarda una copia en el teléfono y envía al volver la conexión
            ref = db.getReference("mensajes");
            ref.keepSynced(true);
        }
        return ref;
    }

    static String miUid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    /** Publica un mensaje para todo el vecindario. */
    static void enviar(String texto, boolean alerta) {
        String uid = miUid();
        if (uid == null) return;
        Map<String, Object> m = new HashMap<>();
        m.put("quien", "N° " + Datos.miNumero);
        m.put("texto", texto);
        m.put("alerta", alerta);
        m.put("uid", uid);
        m.put("fecha", ServerValue.TIMESTAMP);
        mensajes().push().setValue(m);
    }

    /**
     * Escucha los mensajes en tiempo real, los copia a Datos.mensajes y marca en el mapa
     * las casas que mandaron alertas.
     */
    static Escucha escuchar(Cambio cambio) {
        Query q = mensajes().orderByChild("fecha").limitToLast(MAXIMO);
        ValueEventListener l = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                String uid = miUid();
                Datos.mensajes.clear();
                for (DataSnapshot d : snap.getChildren()) {
                    String quien = d.child("quien").getValue(String.class);
                    String texto = d.child("texto").getValue(String.class);
                    if (quien == null || texto == null) continue;
                    boolean alerta = Boolean.TRUE.equals(d.child("alerta").getValue(Boolean.class));
                    Long fecha = d.child("fecha").getValue(Long.class);
                    Datos.mensajes.add(new Datos.Mensaje(quien, texto, hora(fecha),
                            uid != null && uid.equals(d.child("uid").getValue(String.class)), alerta));
                    if (alerta) Datos.marcarAlerta(quien);
                }
                cambio.alCambiar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Se corta si se cierra la sesión o las reglas lo rechazan; la pantalla queda con lo último recibido.
            }
        };
        q.addValueEventListener(l);
        return () -> q.removeEventListener(l);
    }

    private static String hora(Long fecha) {
        if (fecha == null) return Datos.hora();
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(fecha));
    }
}
