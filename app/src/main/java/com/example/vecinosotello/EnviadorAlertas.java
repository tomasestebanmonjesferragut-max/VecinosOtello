package com.example.vecinosotello;

import java.nio.charset.StandardCharsets;

/**
 * Flujo al activar la alarma:
 * 1) evita repeticiones seguidas, 2) crea la alerta con id único, 3) si no hay conexión con el broker
 * informa "no enviada" (sin falso éxito), 4) la publica por MQTT y 5) solo si el broker la acepta
 * la guarda como registro en Realtime Database y la marca "enviada".
 */
public final class EnviadorAlertas {

    public enum Estado { ENVIANDO, ENVIADA, NO_ENVIADA, ESPERAR }

    public interface Aviso { void estado(Estado estado, Alerta alerta, String detalle); }

    /** Dónde queda el registro de la alerta (Realtime Database en la app). */
    public interface Registro { void guardar(Alerta alerta); }

    public static final String TOPICO = "comunidades/" + Firebase.COMUNIDAD + "/alertas";

    private final Transporte transporte;
    private final Registro registro;
    private final Antirrebote antirrebote;

    public EnviadorAlertas(Transporte transporte, Registro registro, Antirrebote antirrebote) {
        this.transporte = transporte;
        this.registro = registro;
        this.antirrebote = antirrebote;
    }

    /** Crea y envía una alerta nueva. Devuelve la alerta creada o null si hay que esperar. */
    public Alerta activar(String tipo, String uid, String nombre, String vivienda, long ahora, Aviso aviso) {
        if (!antirrebote.puede(ahora)) {
            aviso.estado(Estado.ESPERAR, null, "Ya enviaste una alerta. Espera " + antirrebote.segundosRestantes(ahora)
                    + " s antes de enviar otra.");
            return null;
        }
        Alerta a = Alerta.nueva(tipo, uid, nombre, vivienda, ahora);
        enviar(a, aviso);
        return a;
    }

    /** Envía (o reintenta) una alerta ya creada, con el mismo id para que no se duplique. */
    public void enviar(Alerta a, Aviso aviso) {
        if (!transporte.conectado()) {
            aviso.estado(Estado.NO_ENVIADA, a, "Sin conexión con el servidor de alertas. La alerta NO se envió.");
            return;
        }
        aviso.estado(Estado.ENVIANDO, a, "Enviando alerta…");
        transporte.publicar(TOPICO, a.aJson().getBytes(StandardCharsets.UTF_8), new Transporte.Resultado() {
            @Override
            public void ok() {
                antirrebote.marcar(a.fecha);
                registro.guardar(a);
                aviso.estado(Estado.ENVIADA, a, "Alerta enviada a los vecinos conectados.");
            }

            @Override
            public void fallo(String motivo) {
                aviso.estado(Estado.NO_ENVIADA, a, "La alerta NO se envió: " + motivo);
            }
        });
    }
}
