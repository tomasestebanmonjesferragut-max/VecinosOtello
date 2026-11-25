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
import java.util.List;
import java.util.Map;

/**
 * Historial de alertas en Realtime Database (nodo "alertas/{id}"). MQTT avisa en el momento;
 * aquí queda el registro que se consulta después y donde el administrador cambia el estado.
 * Mejora propia: cada vecino puede confirmar que vio la alerta (alertas/{id}/vistos/{uid}).
 */
public final class RegistroAlertas implements EnviadorAlertas.Registro {

    public static final int LIMITE = 30;
    /** Una alerta activa marca la casa en rojo durante este tiempo como máximo. */
    public static final long VIGENCIA_MS = 12 * 60 * 60 * 1000L;

    public static final class Item {
        public final Alerta alerta;
        public final int vistos;
        public final boolean laVi;

        Item(Alerta alerta, int vistos, boolean laVi) {
            this.alerta = alerta;
            this.vistos = vistos;
            this.laVi = laVi;
        }
    }

    public interface Cambio { void alCambiar(List<Item> items); }

    static DatabaseReference ref() { return Firebase.db().getReference("alertas"); }

    @Override
    public void guardar(Alerta a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.id);
        m.put("comunidad", a.comunidad);
        m.put("uid", a.uid);
        m.put("nombre", a.nombre);
        m.put("vivienda", a.vivienda);
        m.put("tipo", a.tipo);
        m.put("fecha", a.fecha);
        m.put("estado", Alerta.ACTIVA);
        ref().child(a.id).setValue(m);
    }

    /** Solo el administrador (lo exigen las reglas). */
    public static void cambiarEstado(String id, String estado, Ventas.Listo listo) {
        Map<String, Object> m = new HashMap<>();
        m.put("estado", estado);
        m.put("atendidaPor", Sesion.identidad());
        ref().child(id).updateChildren(m).addOnCompleteListener(t ->
                listo.listo(t.isSuccessful() ? null : "Firebase rechazó el cambio. Solo el administrador puede hacerlo."));
    }

    public static void marcarVista(String id) {
        ref().child(id).child("vistos").child(Sesion.uid).setValue(ServerValue.TIMESTAMP);
    }

    public static Chat.Escucha escuchar(Cambio cambio, Chat.Error error) {
        Query q = ref().orderByChild("fecha").limitToLast(LIMITE);
        ValueEventListener l = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                List<Item> lista = new ArrayList<>();
                for (DataSnapshot d : snap.getChildren()) {
                    String id = d.child("id").getValue(String.class), uid = d.child("uid").getValue(String.class);
                    String nombre = d.child("nombre").getValue(String.class), vivienda = d.child("vivienda").getValue(String.class);
                    String tipo = d.child("tipo").getValue(String.class), estado = d.child("estado").getValue(String.class);
                    Long fecha = d.child("fecha").getValue(Long.class);
                    if (id == null || nombre == null || vivienda == null || tipo == null || fecha == null) continue;
                    Alerta a = new Alerta(id, Firebase.COMUNIDAD, uid, nombre, vivienda, tipo, fecha, estado == null ? Alerta.ACTIVA : estado);
                    DataSnapshot vistos = d.child("vistos");
                    lista.add(0, new Item(a, (int) vistos.getChildrenCount(), Sesion.uid != null && vistos.hasChild(Sesion.uid)));
                }
                cambio.alCambiar(lista);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                if (error != null) error.alFallar("No se pudo leer el historial de alertas: " + e.getMessage());
            }
        };
        q.addValueEventListener(l);
        return () -> q.removeEventListener(l);
    }

    /** Marca en el mapa las casas con alertas activas y recientes. */
    public static void marcarCasas(List<Item> items, long ahora) {
        Datos.alertas.clear();
        for (Item i : items) {
            if (!Alerta.ACTIVA.equals(i.alerta.estado) || ahora - i.alerta.fecha > VIGENCIA_MS) continue;
            Datos.Casa c = Datos.casaPorNumero(i.alerta.vivienda);
            if (c != null) Datos.alertas.add(c.n);
        }
    }
}
