package com.example.vecinosotello;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * Indicador de conexión con Firebase (mejora propia): usa el nodo especial ".info/connected",
 * que vale true mientras el teléfono está conectado al servidor de Realtime Database.
 */
public final class Conexion {

    public interface Cambio { void alCambiar(boolean conectado); }

    private Conexion() { }

    public static Chat.Escucha escuchar(Cambio cambio) {
        DatabaseReference r = Firebase.db().getReference(".info/connected");
        ValueEventListener l = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot d) {
                cambio.alCambiar(Boolean.TRUE.equals(d.getValue(Boolean.class)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                cambio.alCambiar(false);
            }
        };
        r.addValueEventListener(l);
        return () -> r.removeEventListener(l);
    }
}
