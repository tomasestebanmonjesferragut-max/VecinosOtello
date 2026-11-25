package com.example.vecinosotello;

/**
 * Mejora propia: evita activar la alarma varias veces seguidas (por nervios o toques repetidos).
 * Después de una alerta enviada hay que esperar {@code intervaloMs} para enviar otra.
 */
public final class Antirrebote {

    private final long intervaloMs;
    private long ultimaEnviada = Long.MIN_VALUE / 2;

    public Antirrebote(long intervaloMs) { this.intervaloMs = intervaloMs; }

    public synchronized boolean puede(long ahora) { return ahora - ultimaEnviada >= intervaloMs; }

    public synchronized void marcar(long ahora) { ultimaEnviada = ahora; }

    public synchronized long segundosRestantes(long ahora) {
        return Math.max(0, (intervaloMs - (ahora - ultimaEnviada) + 999) / 1000);
    }
}
