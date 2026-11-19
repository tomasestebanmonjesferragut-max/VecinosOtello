package com.example.vecinosotello;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;

/**
 * Sigue escuchando el chat en Realtime Database aunque la app esté cerrada y avisa con una
 * notificación cuando otro vecino manda una alerta. Las súper urgentes vibran fuerte con alarma.
 * Es gratis (no usa Cloud Functions); Android exige mostrar una notificación fija mientras vigila.
 */
public class VigilanciaService extends Service {

    private static final String CANAL_FIJO = "vigilancia", CANAL_ALERTA = "alertas", CANAL_URGENTE = "urgentes";
    private static final int ID_FIJA = 1;
    private static final long[] PATRON_URGENTE = {0, 1000, 400, 1000, 400, 1000, 400, 1000};
    private static final long[] PATRON_ALERTA = {0, 300, 200, 300};

    private Query consulta;
    private ChildEventListener escucha;
    private int siguienteId = 100;

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
        ServiceCompat.startForeground(this, ID_FIJA, notificacionFija(),
                Build.VERSION.SDK_INT >= 34 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE : 0);
        if (Chat.miUid() == null) { // sin sesión no se puede leer el chat
            stopSelf();
            return START_NOT_STICKY;
        }
        if (escucha == null) escuchar();
        return START_STICKY;
    }

    /** Solo los mensajes que lleguen desde ahora (no avisa de alertas viejas). */
    private void escuchar() {
        consulta = Chat.mensajes().orderByChild("fecha").startAt(System.currentTimeMillis());
        escucha = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot d, String anterior) {
                Boolean alerta = d.child("alerta").getValue(Boolean.class);
                String texto = d.child("texto").getValue(String.class);
                String quien = d.child("quien").getValue(String.class);
                String uid = d.child("uid").getValue(String.class);
                if (!Boolean.TRUE.equals(alerta) || texto == null || quien == null) return;
                if (uid != null && uid.equals(Chat.miUid())) return; // mi propia alerta
                avisar(quien, texto, Datos.esUrgente(texto));
            }

            @Override public void onChildChanged(@NonNull DataSnapshot d, String anterior) { }
            @Override public void onChildRemoved(@NonNull DataSnapshot d) { }
            @Override public void onChildMoved(@NonNull DataSnapshot d, String anterior) { }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                stopSelf(); // se cerró la sesión o se perdió el permiso
            }
        };
        consulta.addChildEventListener(escucha);
    }

    private void avisar(String quien, String texto, boolean urgente) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, urgente ? CANAL_URGENTE : CANAL_ALERTA)
                .setSmallIcon(R.drawable.ic_sirena)
                .setColor(ContextCompat.getColor(this, R.color.peligro))
                .setContentTitle((urgente ? "EMERGENCIA · " : "Alerta · ") + quien)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setCategory(urgente ? NotificationCompat.CATEGORY_ALARM : NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(urgente ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(urgente ? PATRON_URGENTE : PATRON_ALERTA)
                .setAutoCancel(true)
                .setContentIntent(abrirApp());
        if (urgente) b.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        NotificationManager nm = getSystemService(NotificationManager.class);
        try {
            nm.notify(siguienteId++, b.build());
        } catch (SecurityException e) {
            // Sin permiso de notificaciones: igual se vibra abajo si es urgente.
        }
        if (urgente) vibrarFuerte(); // por si el usuario bajó la vibración del canal
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

    private Notification notificacionFija() {
        return new NotificationCompat.Builder(this, CANAL_FIJO)
                .setSmallIcon(R.drawable.ic_escudo)
                .setContentTitle("Vigilancia del pasaje activa")
                .setContentText("Te avisaremos si un vecino manda una alerta.")
                .setOngoing(true)
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
        urgente.setBypassDnd(true);
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
        if (consulta != null && escucha != null) consulta.removeEventListener(escucha);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
