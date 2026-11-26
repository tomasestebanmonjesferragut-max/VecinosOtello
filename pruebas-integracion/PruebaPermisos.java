// Prueba de integración del broker MQTT (EMQX): conexión TLS, publicación/recepción QoS 1 y permisos.
// Uso (desde esta carpeta, con el jar de Paho 1.2.5 de la caché de Gradle):
//   javac -cp paho.jar -d clases PruebaPermisos.java
//   java -cp "paho.jar;clases" PruebaPermisos ../local.properties
// Lee host, usuario y clave de local.properties (no está en Git) y nunca muestra la clave.
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Conexión TLS, publicación/recepción y permisos (ACL) del usuario de la app. Lee la clave de local.properties sin mostrarla. */
public class PruebaPermisos {
    static final String PERMITIDO = "comunidades/otello/alertas", PROHIBIDO = "pruebas/otro";

    static int qos(IMqttToken t) throws MqttException { t.waitForCompletion(10_000); return t.getGrantedQos()[0]; }

    public static void main(String[] a) throws Exception {
        Properties p = new Properties();
        p.load(new InputStreamReader(new FileInputStream(a[0]), StandardCharsets.UTF_8));
        String host = p.getProperty("MQTT_HOST"), usuario = p.getProperty("MQTT_USUARIO"), clave = p.getProperty("MQTT_CLAVE");
        String id = "prueba-" + UUID.randomUUID();
        CountDownLatch llego = new CountDownLatch(1);
        long[] t = {0};

        MqttAsyncClient c = new MqttAsyncClient("ssl://" + host + ":8883", "otello-prueba-pc-" + id.substring(7, 15), new MemoryPersistence());
        c.setCallback(new MqttCallback() {
            public void connectionLost(Throwable e) { System.out.println("  (el broker cerró la conexión: " + e.getMessage() + ")"); }
            public void messageArrived(String top, MqttMessage m) {
                if (new String(m.getPayload(), StandardCharsets.UTF_8).contains(id)) {
                    System.out.println("P3 recibe su propia alerta en " + top + " en " + (System.currentTimeMillis() - t[0])
                            + " ms (qos " + m.getQos() + ", retain " + m.isRetained() + "): OK");
                    llego.countDown();
                }
            }
            public void deliveryComplete(IMqttDeliveryToken tk) { }
        });
        MqttConnectOptions o = new MqttConnectOptions();
        o.setUserName(usuario);
        o.setPassword(clave.toCharArray());
        o.setCleanSession(true);
        o.setConnectionTimeout(15);
        long t0 = System.currentTimeMillis();
        c.connect(o).waitForCompletion(20_000);
        System.out.println("P1 conexión TLS con usuario " + usuario + " a " + host + ":8883 en " + (System.currentTimeMillis() - t0) + " ms: OK");

        int qosPermitido = qos(c.subscribe(PERMITIDO, 1));
        System.out.println("P2 suscripción a " + PERMITIDO + ": qos concedido " + qosPermitido + (qosPermitido == 1 ? " -> OK" : " -> FALLA"));

        String json = "{\"id\":\"" + id + "\",\"comunidad\":\"otello\",\"uid\":\"prueba\",\"nombre\":\"Prueba PC\",\"vivienda\":\"0\","
                + "\"tipo\":\"Prueba de conexión\",\"fecha\":" + System.currentTimeMillis() + ",\"estado\":\"activa\"}";
        MqttMessage m = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        m.setQos(1);
        m.setRetained(false);
        t[0] = System.currentTimeMillis();
        c.publish(PERMITIDO, m).waitForCompletion(10_000);
        if (!llego.await(10, TimeUnit.SECONDS)) System.out.println("P3 recibe su propia alerta: FALLA (no volvió)");

        int qosProhibido;
        try {
            qosProhibido = qos(c.subscribe(PROHIBIDO, 1));
        } catch (MqttException e) {
            qosProhibido = 128;
        }
        System.out.println("P4 suscripción a " + PROHIBIDO + " (fuera de la comunidad): "
                + (qosProhibido == 128 ? "rechazada por el broker (0x80) -> OK" : "ACEPTADA con qos " + qosProhibido + " -> FALLA"));
        if (c.isConnected()) {
            c.disconnect().waitForCompletion(5_000);
        }
        c.close();
    }
}
