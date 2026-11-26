package com.example.vecinosotello;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Perfil del usuario que inició sesión, guardado en /usuarios/{uid}.
 * La identidad única es el uid de Firebase Authentication; una vivienda puede tener varios residentes.
 * El rol siempre se crea como "vecino": solo el administrador del proyecto puede cambiarlo a "admin"
 * (las reglas impiden que un usuario se ponga admin o cambie su propio rol).
 */
public final class Sesion {

    public static final String VECINO = "vecino", ADMIN = "admin";

    public interface Resultado {
        void listo(String error); // error == null si salió bien
    }

    public static String uid, nombre, vivienda, correo, rol;

    private Sesion() { }

    public static boolean activa() { return uid != null && nombre != null && vivienda != null; }

    public static boolean esAdmin() { return ADMIN.equals(rol); }

    public static String identidad() { return Validaciones.identidad(nombre, vivienda); }

    static DatabaseReference ref(String uid) {
        return Firebase.db().getReference("usuarios").child(uid);
    }

    /** Crea el perfil recién registrado (siempre como vecino). */
    public static void crearPerfil(String nombre, String vivienda, Resultado r) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { r.listo("No hay sesión."); return; }
        Map<String, Object> p = new HashMap<>();
        p.put("nombre", nombre.trim());
        p.put("vivienda", Validaciones.normalizarVivienda(vivienda));
        p.put("rol", VECINO);
        ref(u.getUid()).setValue(p).addOnCompleteListener(t -> {
            if (t.isSuccessful()) cargar(r);
            else r.listo("No se pudo guardar tu perfil. Revisa tu conexión e intenta de nuevo.");
        });
    }

    /** Lee el perfil del usuario actual. Si no existe, devuelve un error. */
    public static void cargar(Resultado r) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { r.listo("No hay sesión."); return; }
        ref(u.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot d) {
                String n = d.child("nombre").getValue(String.class);
                String v = d.child("vivienda").getValue(String.class);
                if (n == null || v == null) { r.listo("SIN_PERFIL"); return; }
                uid = u.getUid();
                correo = u.getEmail();
                nombre = n;
                vivienda = v;
                rol = d.child("rol").getValue(String.class);
                // Correo en /correos/{uid}: solo el administrador puede leerlo (ver reglas)
                if (correo != null) Firebase.db().getReference("correos").child(uid).setValue(correo);
                Firebase.suscribirAnuncios();
                r.listo(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                r.listo("No se pudo leer tu perfil. Revisa tu conexión e intenta de nuevo.");
            }
        });
    }

    /** Cambia el nombre mostrado (el rol y la vivienda no se tocan). */
    public static void cambiarNombre(String nuevo, Resultado r) {
        String e = Validaciones.validarNombre(nuevo);
        if (e != null) { r.listo(e); return; }
        ref(uid).child("nombre").setValue(nuevo.trim()).addOnCompleteListener(t -> {
            if (t.isSuccessful()) nombre = nuevo.trim();
            r.listo(t.isSuccessful() ? null : "No se pudo guardar. Revisa tu conexión e intenta de nuevo.");
        });
    }

    public static void cerrar() {
        Firebase.desuscribirAnuncios();
        FirebaseAuth.getInstance().signOut();
        uid = nombre = vivienda = correo = rol = null;
    }
}
