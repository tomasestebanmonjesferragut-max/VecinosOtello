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
 * Tienda del pasaje (nodo "ventas"): productos y servicios que ofrecen los vecinos.
 * Solo el dueño puede marcarlos como "no disponible"; los demás vecinos los califican
 * con 1 a 5 estrellas en /calificaciones/{venta}/{uid} (una calificación por vecino).
 */
public final class Ventas {

    public static final int LIMITE = 100;

    public static final class Venta {
        public final String id, uid, nombre, vivienda, titulo, descripcion, categoria, tipo;
        public final long precio;
        public final boolean vendido;
        public final Long fecha;

        Venta(String id, String uid, String nombre, String vivienda, String titulo, String descripcion,
              String categoria, long precio, boolean vendido, Long fecha) {
            this(id, uid, nombre, vivienda, titulo, descripcion, categoria, precio, vendido, fecha, "Producto");
        }

        /** tipo: "Producto" o "Servicio" (las publicaciones antiguas no lo traen y cuentan como producto). */
        Venta(String id, String uid, String nombre, String vivienda, String titulo, String descripcion,
              String categoria, long precio, boolean vendido, Long fecha, String tipo) {
            this.tipo = Validaciones.esTipo(tipo) ? tipo : "Producto";
            this.id = id;
            this.uid = uid;
            this.nombre = nombre;
            this.vivienda = vivienda;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.categoria = categoria;
            this.precio = precio;
            this.vendido = vendido;
            this.fecha = fecha;
        }

        public boolean esMia() { return uid != null && uid.equals(Sesion.uid); }
    }

    public interface Cambio { void alCambiar(List<Venta> ventas); }

    /** Calificaciones por venta: id de venta → (uid → estrellas). */
    public interface CambioEstrellas { void alCambiar(Map<String, Map<String, Long>> calificaciones); }

    public interface Listo { void listo(String error); }

    private Ventas() { }

    static DatabaseReference ref() { return Firebase.db().getReference("ventas"); }

    /** Publica un producto o servicio. Devuelve el error de validación o null. */
    public static String publicar(String tipo, String titulo, String precio, String categoria, String descripcion, Listo listo) {
        String e = Validaciones.validarTipo(tipo);
        if (e == null) e = Validaciones.validarVenta(titulo, precio, categoria, descripcion);
        if (e != null) return e;
        Map<String, Object> v = new HashMap<>();
        v.put("uid", Sesion.uid);
        v.put("nombre", Sesion.nombre);
        v.put("vivienda", Sesion.vivienda);
        v.put("titulo", titulo.trim());
        v.put("descripcion", descripcion == null ? "" : descripcion.trim());
        v.put("categoria", categoria);
        v.put("precio", Validaciones.precioDesdeTexto(precio));
        v.put("tipo", tipo);
        v.put("vendido", false);
        v.put("fecha", ServerValue.TIMESTAMP);
        ref().push().setValue(v).addOnCompleteListener(t ->
                listo.listo(t.isSuccessful() ? null : "No se pudo publicar. Revisa tu conexión e intenta de nuevo."));
        return null;
    }

    public static void marcarVendido(Venta v, boolean vendido, Listo listo) {
        ref().child(v.id).child("vendido").setValue(vendido).addOnCompleteListener(t ->
                listo.listo(t.isSuccessful() ? null : "No se pudo cambiar el estado."));
    }

    static DatabaseReference refEstrellas() { return Firebase.db().getReference("calificaciones"); }

    /** Guarda (o cambia) la calificación del usuario actual. No se puede calificar lo propio. */
    public static void calificar(Venta v, int estrellas, Listo listo) {
        if (v.esMia()) { listo.listo("No puedes calificar lo que tú ofreces."); return; }
        if (!Validaciones.estrellasValidas(estrellas)) { listo.listo("Elige de 1 a 5 estrellas."); return; }
        refEstrellas().child(v.id).child(Sesion.uid).setValue(estrellas).addOnCompleteListener(t ->
                listo.listo(t.isSuccessful() ? null : "No se pudo guardar tu calificación."));
    }

    public static double promedio(java.util.Collection<Long> estrellas) {
        if (estrellas.isEmpty()) return 0;
        double suma = 0;
        for (Long e : estrellas) suma += e;
        return suma / estrellas.size();
    }

    /** "★ 4,5 (2)" o "Sin calificaciones". */
    public static String textoEstrellas(java.util.Collection<Long> estrellas) {
        if (estrellas.isEmpty()) return "Sin calificaciones";
        return String.format(new java.util.Locale("es", "CL"), "★ %.1f (%d)", promedio(estrellas), estrellas.size());
    }

    public static Chat.Escucha escucharEstrellas(CambioEstrellas cambio) {
        ValueEventListener l = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                Map<String, Map<String, Long>> r = new HashMap<>();
                for (DataSnapshot venta : snap.getChildren()) {
                    Map<String, Long> porUsuario = new HashMap<>();
                    for (DataSnapshot u : venta.getChildren()) {
                        Long e = u.getValue(Long.class);
                        if (e != null) porUsuario.put(u.getKey(), e);
                    }
                    r.put(venta.getKey(), porUsuario);
                }
                cambio.alCambiar(r);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) { } // sin estrellas la tienda sigue funcionando
        };
        refEstrellas().addValueEventListener(l);
        return () -> refEstrellas().removeEventListener(l);
    }

    /** Devuelve solo las de la categoría (o todas si es null). Más nuevas primero. */
    public static List<Venta> filtrar(List<Venta> ventas, String categoria, boolean soloDisponibles) {
        List<Venta> r = new ArrayList<>();
        for (Venta v : ventas)
            if ((categoria == null || categoria.equals(v.categoria)) && (!soloDisponibles || !v.vendido)) r.add(v);
        return r;
    }

    public static Chat.Escucha escuchar(Cambio cambio, Chat.Error error) {
        Query q = ref().orderByChild("fecha").limitToLast(LIMITE);
        ValueEventListener l = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                List<Venta> lista = new ArrayList<>();
                for (DataSnapshot d : snap.getChildren()) {
                    String titulo = d.child("titulo").getValue(String.class);
                    Long precio = d.child("precio").getValue(Long.class);
                    if (titulo == null || precio == null) continue;
                    lista.add(0, new Venta(d.getKey(), d.child("uid").getValue(String.class),
                            d.child("nombre").getValue(String.class), d.child("vivienda").getValue(String.class), titulo,
                            d.child("descripcion").getValue(String.class), d.child("categoria").getValue(String.class),
                            precio, Boolean.TRUE.equals(d.child("vendido").getValue(Boolean.class)),
                            d.child("fecha").getValue(Long.class), d.child("tipo").getValue(String.class)));
                }
                cambio.alCambiar(lista);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                error.alFallar("No se pudieron leer las ventas: " + e.getMessage());
            }
        };
        q.addValueEventListener(l);
        return () -> q.removeEventListener(l);
    }
}
