package com.example.vecinosotello;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Datos del vecindario. El chat (y las alertas que van en él) vienen de Firebase (ver Chat);
 * eventos, rondas y vacaciones todavía viven en memoria mientras la app está abierta.
 */
public final class Datos {

    // Cada casa entra con su propio correo: otello<número>sanjoaquin@gmail.com
    // Las cuentas (correo + clave de letras y números) se crean en Firebase Authentication.
    static final String FORMATO_CORREO = "otello\\d+sanjoaquin@gmail\\.com";

    // Puntos de las calles medidos sobre OpenStreetMap
    static final LatLng GOYA = new LatLng(-33.490942, -70.621657);  // tramo de arriba, esquina Pintor Goya
    static final LatLng CRUCE = new LatLng(-33.490992, -70.621395); // tramo de arriba con el pasaje
    static final LatLng ESTE = new LatLng(-33.490956, -70.621260);  // fin del tramo de arriba
    static final LatLng PINAR = new LatLng(-33.491734, -70.621346); // pasaje con El Pinar
    static final double DLAT = 0.00007, DLON = 0.00009;             // ~8 m desde el centro de la calle

    // Números de casa: solo la 3538 está confirmada; 3530, 3531 y 3598 vienen de OpenStreetMap.
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

        public String nombre() { return num != null ? "N° " + num : "Casa " + codigo + " · N° por confirmar"; }

        public boolean esMia() { return num != null && num.equals(miNumero); }
    }

    public static final class Mensaje {
        public final String quien, texto, hora;
        public final boolean mio, alerta;

        Mensaje(String quien, String texto, String hora, boolean mio, boolean alerta) {
            this.quien = quien;
            this.texto = texto;
            this.hora = hora;
            this.mio = mio;
            this.alerta = alerta;
        }
    }

    public static final class Evento {
        public final String titulo, detalle, tipo; // tipo: alerta, reporte, agua o null

        Evento(String titulo, String detalle, String tipo) {
            this.titulo = titulo;
            this.detalle = detalle;
            this.tipo = tipo;
        }
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

    public static String miNumero;
    public static final List<Casa> casas = new ArrayList<>();
    public static final Set<Integer> alertas = new HashSet<>();
    public static final Set<Integer> vacaciones = new HashSet<>();
    public static final List<Mensaje> mensajes = new ArrayList<>();
    public static final List<Evento> eventos = new ArrayList<>();
    public static final List<Turno> turnos = new ArrayList<>();
    public static int mensajesLeidos = -1; // -1 = aún no llega el chat de Firebase

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


        eventos.add(new Evento("Persona sospechosa en El Pinar", "Hoy 21:02 · reportado por Don Luis", "alerta"));
        eventos.add(new Evento("Corte de agua programado", "Jueves 9:00 a 14:00", "agua"));
        eventos.add(new Evento("Reunión de vecinos", "Sábado 18:00 · sede del pasaje", null));
        eventos.add(new Evento("Operativo de limpieza", "Domingo 10:00 · traer bolsas", null));

        turnos.add(new Turno("Hoy", "21:00 - 22:00", "Don Luis"));
        turnos.add(new Turno("Hoy", "22:00 - 23:00", null));
        turnos.add(new Turno("Mañana", "21:00 - 22:00", "Carla"));
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

    /**
     * Revisa el formato antes de ir a Firebase: correo de una casa del pasaje y clave de 8 dígitos.
     * Devuelve null si está bien o el mensaje de error. La clave correcta la comprueba Firebase.
     */
    public static String validar(String correo, String clave) {
        String c = correo.trim().toLowerCase(Locale.ROOT);
        if (!c.matches(FORMATO_CORREO)) return "Usa el correo de tu casa: otello + número + sanjoaquin@gmail.com";
        if (casaPorNumero(numeroDelCorreo(c)) == null) return "Ese número de casa no está registrado en el pasaje.";
        if (!claveValida(clave)) return MENSAJE_CLAVE;
        return null;
    }

    static final String MENSAJE_CLAVE = "La contraseña debe tener al menos 8 caracteres, con letras y números.";

    /** Clave de al menos 8 caracteres, solo letras y números, con al menos una letra y un número. */
    static boolean claveValida(String clave) {
        return clave.matches("[A-Za-z0-9]{8,}") && clave.matches(".*[A-Za-z].*") && clave.matches(".*\\d.*");
    }

    /** Alertas súper urgentes: vibración fuerte y alarma. Las demás alertas avisan normal. */
    static final String[] URGENTES = {"Necesito ayuda", "Robo o intento de robo", "Incendio o humo",
            "Humo o incendio", "Emergencia médica"};

    static boolean esUrgente(String texto) {
        for (String u : URGENTES) if (texto.startsWith(u)) return true;
        return false;
    }

    public static Casa casaPorNumero(String num) {
        for (Casa c : casas) if (c.num != null && c.num.equals(num)) return c;
        return null;
    }

    /** Toma los números del correo: otello3538... -> "3538". */
    public static String numeroDelCorreo(String correo) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(correo);
        return m.find() ? m.group() : null;
    }

    public static Casa miCasa() {
        for (Casa c : casas) if (c.esMia()) return c;
        return null;
    }

    public static String hora() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    /** Publica un mensaje mío en el chat de Firebase; si es alerta, marca mi casa. */
    public static void enviar(String texto, boolean alerta) {
        Chat.enviar(texto, alerta);
        Casa mia = miCasa();
        if (alerta && mia != null) alertas.add(mia.n);
    }

    /** Marca en rojo la casa que mandó una alerta ("N° 3538" -> casa 3538). */
    public static void marcarAlerta(String quien) {
        Casa c = casaPorNumero(numeroDelCorreo(quien));
        if (c != null) alertas.add(c.n);
    }
}
