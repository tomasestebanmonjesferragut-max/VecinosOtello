package com.example.vecinosotello;

import android.os.Handler;
import android.os.Looper;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Conexión MQTT de las alertas (una sola por proceso, la abre VigilanciaService).
 * - Broker EMQX con TLS (ssl://...:8883), usuario y contraseña desde local.properties (no van a Git).
 * - Tópico por comunidad: comunidades/otello/alertas, QoS 1, sin retain (una alarma vieja no reaparece
 *   al suscribirse; el historial se consulta en Realtime Database).
 * - Reconexión automática; al reconectar se vuelve a suscribir sin crear otro callback (sin duplicados).
 * - Las copias repetidas de un mismo id se descartan con el Deduplicador.
 */
public final class ClienteMqtt implements Transporte {

    public interface Oyente { void alerta(Alerta alerta); }

    public interface OyenteEstado { void conexion(boolean conectado, String detalle); }

    private static final long REINTENTO_MAX_MS = 60_000;
    private static ClienteMqtt instancia;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Oyente> oyentes = new CopyOnWriteArraySet<>();
    private final Set<OyenteEstado> oyentesEstado = new CopyOnWriteArraySet<>();
    private final Deduplicador deduplicador = new Deduplicador(500);
    private MqttAsyncClient cliente;
    private MqttConnectOptions opciones;
    private long esperaReintento = 2_000;
    private volatile boolean conectado;
    private String detalle = "Conectando con el servidor de alertas…";

    private ClienteMqtt() { }

    public static synchronized ClienteMqtt get() {
        if (instancia == null) instancia = new ClienteMqtt();
        return instancia;
    }

    public static boolean configurado() {
        return !BuildConfig.MQTT_HOST.startsWith("sin-configurar");
    }

    /** Abre la conexión (si ya existe, no hace nada: un solo cliente y un solo callback). */
    public synchronized void conectar(String clientId) {
        if (cliente != null) return;
        if (!configurado()) {
            avisarEstado(false, "Alertas MQTT sin configurar (faltan los datos del broker en local.properties).");
            return;
        }
        try {
            cliente = new MqttAsyncClient("ssl://" + BuildConfig.MQTT_HOST + ":8883", clientId, new MemoryPersistence());
        } catch (MqttException e) {
            avisarEstado(false, "No se pudo crear la conexión MQTT.");
            return;
        }
        opciones = new MqttConnectOptions();
        opciones.setUserName(BuildConfig.MQTT_USUARIO);
        opciones.setPassword(BuildConfig.MQTT_CLAVE.toCharArray());
        opciones.setAutomaticReconnect(true); // reconecta solo al volver el Wi-Fi o los datos
        opciones.setMaxReconnectDelay((int) REINTENTO_MAX_MS);
        opciones.setCleanSession(true);
        opciones.setKeepAliveInterval(30);
        opciones.setConnectionTimeout(15);

        cliente.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconexion, String servidor) {
                esperaReintento = 2_000;
                suscribir();
                avisarEstado(true, reconexion ? "Alertas reconectadas" : "Alertas conectadas");
            }

            @Override
            public void connectionLost(Throwable causa) {
                avisarEstado(false, "Sin conexión con el servidor de alertas. Reintentando…");
            }

            @Override
            public void messageArrived(String topico, MqttMessage mensaje) {
                Alerta a = Alerta.desdeJson(new String(mensaje.getPayload(), StandardCharsets.UTF_8));
                if (a == null || !Firebase.COMUNIDAD.equals(a.comunidad)) return;
                if (!deduplicador.esNueva(a.id)) return; // copia repetida (QoS 1): se ignora
                main.post(() -> {
                    for (Oyente o : oyentes) o.alerta(a);
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) { }
        });
        intentarConexion();
    }

    /** La reconexión automática de Paho solo actúa después de la primera conexión; antes reintentamos nosotros. */
    private synchronized void intentarConexion() {
        if (cliente == null || cliente.isConnected()) return;
        try {
            cliente.connect(opciones, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken t) { /* connectComplete hace el resto */ }

                @Override
                public void onFailure(IMqttToken t, Throwable e) {
                    avisarEstado(false, "No se pudo conectar al servidor de alertas. Reintentando en "
                            + (esperaReintento / 1000) + " s…");
                    main.postDelayed(ClienteMqtt.this::intentarConexion, esperaReintento);
                    esperaReintento = Math.min(esperaReintento * 2, REINTENTO_MAX_MS);
                }
            });
        } catch (MqttException e) {
            main.postDelayed(this::intentarConexion, esperaReintento);
        }
    }

    private void suscribir() {
        try {
            cliente.subscribe(EnviadorAlertas.TOPICO, 1);
        } catch (MqttException e) {
            avisarEstado(false, "No se pudo suscribir a las alertas.");
        }
    }

    public synchronized void desconectar() {
        main.removeCallbacksAndMessages(null);
        if (cliente == null) return;
        MqttAsyncClient viejo = cliente;
        cliente = null;
        new Thread(() -> { // no esperar al broker en el hilo principal
            try {
                if (viejo.isConnected()) viejo.disconnect().waitForCompletion(3_000);
                viejo.close();
            } catch (MqttException e) {
                // Se está cerrando la sesión: si el broker no responde, igual soltamos el cliente.
            }
        }).start();
        avisarEstado(false, "Alertas desconectadas");
    }

    @Override
    public boolean conectado() { return cliente != null && cliente.isConnected(); }

    /** QoS 1 y sin retain. El resultado llega en el hilo principal. */
    @Override
    public void publicar(String topico, byte[] datos, Resultado r) {
        MqttMessage m = new MqttMessage(datos);
        m.setQos(1);
        m.setRetained(false);
        try {
            cliente.publish(topico, m, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken t) { main.post(r::ok); }

                @Override
                public void onFailure(IMqttToken t, Throwable e) { main.post(() -> r.fallo("el servidor no la recibió.")); }
            });
        } catch (MqttException | NullPointerException e) {
            main.post(() -> r.fallo("sin conexión con el servidor de alertas."));
        }
    }

    public void agregar(Oyente o) { oyentes.add(o); }

    public void quitar(Oyente o) { oyentes.remove(o); }

    public void agregar(OyenteEstado o) {
        oyentesEstado.add(o);
        o.conexion(conectado, detalle);
    }

    public void quitar(OyenteEstado o) { oyentesEstado.remove(o); }

    private void avisarEstado(boolean c, String d) {
        conectado = c;
        detalle = d;
        main.post(() -> {
            for (OyenteEstado o : oyentesEstado) o.conexion(c, d);
        });
    }
}
