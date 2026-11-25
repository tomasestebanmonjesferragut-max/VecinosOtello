package com.example.vecinosotello;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.UUID;

/**
 * Servicio en primer plano que mantiene abierta la conexión MQTT de las alertas, para recibirlas
 * con la app en segundo plano. Cuando llega una alerta de otro vecino muestra una notificación local:
 * las súper urgentes vibran fuerte con sonido de alarma. Android exige mostrar una notificación fija.
 * Límite: si el sistema o el usuario cierran la app a la fuerza, la conexión se corta (ver informe).
 */
public class VigilanciaService extends Service {

    private static final String CANAL_FIJO = "vigilancia", CANAL_ALERTA = "alertas", CANAL_URGENTE = "urgentes";
    private static final int ID_FIJA = 1;
    private static final long[] PATRON_URGENTE = {0, 1000, 400, 1000, 400, 1000, 400, 1000};
    private static final long[] PATRON_ALERTA = {0, 300, 200, 300};

    private final ClienteMqtt.Oyente oyente = this::avisar;
    private final ClienteMqtt.OyenteEstado oyenteEstado = (c, d) -> actualizarFija(d);

    public static void iniciar(Context ctx) {
        ContextCompat.startForegroundService(ctx, new Intent(ctx, VigilanciaService.class));
    }

    public static void detener(Context ctx) {
        ctx.stopService(new Intent(ctx, VigilanciaService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        crearCanales();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ServiceCompat.startForeground(this, ID_FIJA, notificacionFija("Conectando con el servidor de alertas…"),
                Build.VERSION.SDK_INT >= 34 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE : 0);
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { // sin sesión no hay a quién avisar
            stopSelf();
            return START_NOT_STICKY;
        }
        ClienteMqtt c = ClienteMqtt.get();
        c.agregar(oyente);       // un Set: registrar dos veces no duplica avisos
        c.agregar(oyenteEstado);
        c.conectar("otello-" + u.getUid().substring(0, 8) + "-" + idDispositivo());
        return START_STICKY;
    }

    /** Identificador estable de este teléfono, para que dos teléfonos del mismo vecino no se desconecten entre sí. */
    private String idDispositivo() {
        SharedPreferences p = getSharedPreferences("vigilancia", MODE_PRIVATE);
        String id = p.getString("dispositivo", null);
        if (id == null) {
            id = UUID.randomUUID().toString().substring(0, 8);
            p.edit().putString("dispositivo", id).apply();
        }
        return id;
    }

    private void avisar(Alerta a) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null && u.getUid().equals(a.uid)) return; // mi propia alerta
        boolean urgente = a.esUrgente();
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, urgente ? CANAL_URGENTE : CANAL_ALERTA)
                .setSmallIcon(R.drawable.ic_sirena)
                .setColor(ContextCompat.getColor(this, R.color.peligro))
                .setContentTitle(urgente ? "Alerta comunitaria · EMERGENCIA" : "Alerta comunitaria")
                .setContentText(a.descripcion())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(a.descripcion()))
                .setCategory(urgente ? NotificationCompat.CATEGORY_ALARM : NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(urgente ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(urgente ? PATRON_URGENTE : PATRON_ALERTA)
                .setAutoCancel(true)
                .setContentIntent(abrirApp());
        if (urgente) b.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        try {
            getSystemService(NotificationManager.class).notify(a.id.hashCode(), b.build());
        } catch (SecurityException e) {
            // Permiso de notificaciones negado: la alerta igual se ve en el mapa y se vibra si es urgente.
        }
        if (urgente) vibrarFuerte();
    }

    private void vibrarFuerte() {
        Vibrator v = getSystemService(Vibrator.class);
        if (v == null || !v.hasVibrator()) return;
        v.vibrate(VibrationEffect.createWaveform(PATRON_URGENTE, -1),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
    }

    private PendingIntent abrirApp() {
        Intent i = new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void actualizarFija(String detalle) {
        try {
            getSystemService(NotificationManager.class).notify(ID_FIJA, notificacionFija(detalle));
        } catch (SecurityException e) {
            // Sin permiso de notificaciones no se puede actualizar el texto; el servicio sigue funcionando.
        }
    }

    private Notification notificacionFija(String detalle) {
        return new NotificationCompat.Builder(this, CANAL_FIJO)
                .setSmallIcon(R.drawable.ic_escudo)
                .setContentTitle("Vigilancia del pasaje activa")
                .setContentText(detalle)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(abrirApp())
                .build();
    }

    private void crearCanales() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel fijo = new NotificationChannel(CANAL_FIJO, "Vigilancia activa", NotificationManager.IMPORTANCE_MIN);
        fijo.setDescription("Aviso fijo mientras la app escucha las alertas del pasaje");

        NotificationChannel alerta = new NotificationChannel(CANAL_ALERTA, "Alertas del vecindario", NotificationManager.IMPORTANCE_HIGH);
        alerta.setDescription("Personas o autos sospechosos y otros avisos");
        alerta.enableVibration(true);
        alerta.setVibrationPattern(PATRON_ALERTA);

        NotificationChannel urgente = new NotificationChannel(CANAL_URGENTE, "Emergencias", NotificationManager.IMPORTANCE_HIGH);
        urgente.setDescription("Pedido de ayuda, robo, incendio o emergencia médica");
        urgente.enableVibration(true);
        urgente.setVibrationPattern(PATRON_URGENTE);
        urgente.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        Uri alarma = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        urgente.setSound(alarma, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());

        nm.createNotificationChannel(fijo);
        nm.createNotificationChannel(alerta);
        nm.createNotificationChannel(urgente);
    }

    @Override
    public void onDestroy() {
        ClienteMqtt c = ClienteMqtt.get();
        c.quitar(oyente);
        c.quitar(oyenteEstado);
        c.desconectar(); // libera la conexión al cerrar sesión
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
