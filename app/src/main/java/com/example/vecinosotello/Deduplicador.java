package com.example.vecinosotello;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recuerda los últimos identificadores de alerta procesados para no notificar dos veces la misma
 * (QoS 1 garantiza "al menos una vez", así que pueden llegar copias). Guarda como máximo {@code capacidad}.
 */
public final class Deduplicador {

    private final Map<String, Boolean> vistos;

    public Deduplicador(int capacidad) {
        vistos = new LinkedHashMap<String, Boolean>(capacidad, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> e) {
                return size() > capacidad;
            }
        };
    }

    /** true la primera vez que llega ese id; false si ya se procesó. */
    public synchronized boolean esNueva(String id) {
        if (id == null || vistos.containsKey(id)) return false;
        vistos.put(id, Boolean.TRUE);
        return true;
    }
}
