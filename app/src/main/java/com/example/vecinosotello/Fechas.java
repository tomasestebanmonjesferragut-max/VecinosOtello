package com.example.vecinosotello;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Formato de fecha y hora para mensajes, ventas, anuncios y alertas. */
public final class Fechas {

    private Fechas() { }

    /** "Hoy 21:04", "Ayer 08:15" o "12/11 21:04". */
    public static String corta(Long ms, long ahora) {
        if (ms == null) return "Enviando…";
        Calendar c = Calendar.getInstance(), hoy = Calendar.getInstance();
        c.setTimeInMillis(ms);
        hoy.setTimeInMillis(ahora);
        String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ms));
        if (mismoDia(c, hoy)) return "Hoy " + hora;
        hoy.add(Calendar.DAY_OF_YEAR, -1);
        if (mismoDia(c, hoy)) return "Ayer " + hora;
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(ms));
    }

    public static String corta(Long ms) { return corta(ms, System.currentTimeMillis()); }

    private static boolean mismoDia(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
