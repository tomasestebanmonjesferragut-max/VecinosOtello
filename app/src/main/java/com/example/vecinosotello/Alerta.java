package com.example.vecinosotello;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Alerta comunitaria. Viaja por MQTT como JSON y se guarda como registro en Realtime Database.
 * El id (UUID) permite descartar copias repetidas: con QoS 1 el broker puede entregar dos veces.
 */
public final class Alerta {

    public static final String ACTIVA = "activa", ATENDIDA = "atendida", CERRADA = "cerrada";

    /** Tipos súper urgentes: vibración fuerte y alarma. Los demás avisan normal. */
    static final String[] URGENTES = {"Necesito ayuda", "Robo o intento de robo", "Incendio o humo", "Emergencia médica"};

    public final String id, comunidad, uid, nombre, vivienda, tipo, estado;
    public final long fecha;

    public Alerta(String id, String comunidad, String uid, String nombre, String vivienda, String tipo, long fecha, String estado) {
        this.id = id;
        this.comunidad = comunidad;
        this.uid = uid;
        this.nombre = nombre;
        this.vivienda = vivienda;
        this.tipo = tipo;
        this.fecha = fecha;
        this.estado = estado;
    }

    /** Crea una alerta nueva con identificador único y estado "activa". */
    public static Alerta nueva(String tipo, String uid, String nombre, String vivienda, long ahora) {
        return new Alerta(UUID.randomUUID().toString(), Firebase.COMUNIDAD, uid, nombre, vivienda, tipo, ahora, ACTIVA);
    }

    public boolean esUrgente() {
        for (String u : URGENTES) if (u.equals(tipo)) return true;
        return false;
    }

    /** "Profe Benja · Vivienda #311A ha solicitado ayuda (Robo o intento de robo)." */
    public String descripcion() {
        return nombre + " · Vivienda #" + vivienda + " ha solicitado ayuda (" + tipo + ").";
    }

    public String aJson() {
        try {
            return new JSONObject()
                    .put("id", id).put("comunidad", comunidad).put("uid", uid).put("nombre", nombre)
                    .put("vivienda", vivienda).put("tipo", tipo).put("fecha", fecha).put("estado", estado)
                    .toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e); // no ocurre: todos los valores son texto o números
        }
    }

    /** Lee una alerta recibida. Devuelve null si el mensaje no tiene el formato esperado. */
    public static Alerta desdeJson(String json) {
        try {
            JSONObject o = new JSONObject(json);
            String id = o.optString("id", ""), uid = o.optString("uid", ""), nombre = o.optString("nombre", "");
            String vivienda = o.optString("vivienda", ""), tipo = o.optString("tipo", "");
            if (id.isEmpty() || uid.isEmpty() || nombre.isEmpty() || vivienda.isEmpty() || tipo.isEmpty()) return null;
            return new Alerta(id, o.optString("comunidad", ""), uid, nombre, vivienda, tipo,
                    o.optLong("fecha", 0), o.optString("estado", ACTIVA));
        } catch (JSONException e) {
            return null; // mensaje ajeno o dañado: se ignora
        }
    }
}
