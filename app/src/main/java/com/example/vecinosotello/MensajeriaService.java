package com.example.vecinosotello;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Recibe por Firebase Cloud Messaging los anuncios oficiales (tema "comunidad-otello-anuncios").
 * El notificador los envía como mensajes de datos con prioridad alta, así que este método se
 * ejecuta con la app abierta, en segundo plano o cerrada, y arma una notificación que abre el anuncio.
 */
public class MensajeriaService extends FirebaseMessagingService {

    static final String CANAL_ANUNCIOS = "anuncios";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensaje) {
        Map<String, String> d = mensaje.getData();
        if (!"anuncio".equals(d.get("tipo")) || d.get("anuncioId") == null) return;
        String id = d.get("anuncioId");
        boolean importante = "true".equals(d.get("importante"));

        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel canal = new NotificationChannel(CANAL_ANUNCIOS, "Anuncios oficiales", NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription("Información publicada por la administración del pasaje");
        nm.createNotificationChannel(canal);

        Intent abrir = new Intent(this, AnunciosActivity.class)
                .putExtra(LoginActivity.EXTRA_ANUNCIO, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, id.hashCode(), abrir,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String texto = d.get("contenido") == null ? "" : d.get("contenido");
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CANAL_ANUNCIOS)
                .setSmallIcon(R.drawable.ic_calendario)
                .setColor(ContextCompat.getColor(this, R.color.primario))
                .setContentTitle((importante ? "Importante · " : "Anuncio · ") + d.get("titulo"))
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            nm.notify(id.hashCode(), b.build());
        } catch (SecurityException e) {
            // El usuario negó el permiso de notificaciones: el anuncio igual queda en la pantalla de Anuncios.
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        // No se guarda el token: los anuncios se envían al tema, al que la app se suscribe al iniciar sesión.
    }
}
