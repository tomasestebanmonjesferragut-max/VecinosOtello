package com.example.vecinosotello;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapa del pasaje: posición de cada casa y su estado (alerta activa o vacaciones).
 * Las rondas y el modo vacaciones son locales (mejora propia, no se guardan en Firebase).
 */
public final class Datos {

    // Puntos de las calles medidos sobre OpenStreetMap
    static final LatLng GOYA = new LatLng(-33.490942, -70.621657);  // tramo de arriba, esquina Pintor Goya
    static final LatLng CRUCE = new LatLng(-33.490992, -70.621395); // tramo de arriba con el pasaje
    static final LatLng ESTE = new LatLng(-33.490956, -70.621260);  // fin del tramo de arriba
    static final LatLng PINAR = new LatLng(-33.491734, -70.621346); // pasaje con El Pinar
    static final double DLAT = 0.00007, DLON = 0.00009;             // ~8 m desde el centro de la calle

    // Números de casa conocidos: 3538 (confirmado) y 3530, 3531, 3598 (OpenStreetMap).
    // null = por confirmar (se muestra el código: N1, S2, O5, E7...)
    private static final String[] TRAMO_NORTE = {null, null, null, null};
    private static final String[] TRAMO_SUR = {null, null, null};
    private static final String[] PASAJE_OESTE = {"3531", null, null, null, null, null, null, null, null, null};
    private static final String[] PASAJE_ESTE = {"3530", "3538", null, null, null, null, null, null, null, "3598"};

    public static final class Casa {
        public final int n;
        public final String codigo;
        public final String num;
        public final LatLng pos;

        Casa(int n, String codigo, String num, LatLng pos) {
            this.n = n;
            this.codigo = codigo;
            this.num = num;
            this.pos = pos;
        }

        public String etiqueta() { return num != null ? num : codigo; }

        public String nombre() { return num != null ? "Vivienda #" + num : "Casa " + codigo + " · N° por confirmar"; }

        public boolean esMia() { return num != null && num.equalsIgnoreCase(Sesion.vivienda); }
    }

    public static final class Turno {
        public final String dia, hora;
        public String quien;

        Turno(String dia, String hora, String quien) {
            this.dia = dia;
            this.hora = hora;
            this.quien = quien;
        }
    }

    public static final List<Casa> casas = new ArrayList<>();
    /** Casas con una alerta activa (según el registro de alertas en Realtime Database). */
    public static final Set<Integer> alertas = new HashSet<>();
    public static final Set<Integer> vacaciones = new HashSet<>();
    public static final List<Turno> turnos = new ArrayList<>();

    static {
        for (int i = 0; i < TRAMO_NORTE.length; i++)
            agregar("N", i, TRAMO_NORTE[i], mover(entre(GOYA, ESTE, (i + 0.5) / 4), DLAT, 0));
        for (int i = 0; i < TRAMO_SUR.length; i++)
            agregar("S", i, TRAMO_SUR[i],
                    mover(i < 2 ? entre(GOYA, CRUCE, (i + 0.5) / 2.4) : entre(CRUCE, ESTE, 0.75), -DLAT, 0));
        for (int i = 0; i < PASAJE_OESTE.length; i++)
            agregar("O", i, PASAJE_OESTE[i], mover(entre(CRUCE, PINAR, 0.11 + i * 0.086), 0, -DLON));
        for (int i = 0; i < PASAJE_ESTE.length; i++)
            agregar("E", i, PASAJE_ESTE[i], mover(entre(CRUCE, PINAR, 0.11 + i * 0.086), 0, DLON));

        turnos.add(new Turno("Hoy", "21:00 - 22:00", null));
        turnos.add(new Turno("Hoy", "22:00 - 23:00", null));
        turnos.add(new Turno("Mañana", "21:00 - 22:00", null));
        turnos.add(new Turno("Mañana", "22:00 - 23:00", null));
    }

    private Datos() { }

    private static void agregar(String letra, int i, String num, LatLng pos) {
        casas.add(new Casa(casas.size() + 1, letra + (i + 1), num, pos));
    }

    static LatLng entre(LatLng a, LatLng b, double t) {
        return new LatLng(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t);
    }

    static LatLng mover(LatLng p, double dlat, double dlon) {
        return new LatLng(p.latitude + dlat, p.longitude + dlon);
    }

    public static Casa casaPorNumero(String num) {
        for (Casa c : casas) if (c.num != null && c.num.equalsIgnoreCase(num)) return c;
        return null;
    }

    /** Casa de la vivienda del usuario, si está dibujada en el mapa. */
    public static Casa miCasa() {
        for (Casa c : casas) if (c.esMia()) return c;
        return null;
    }
}
