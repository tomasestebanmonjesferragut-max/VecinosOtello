package com.example.vecinosotello;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

/** Acceso único a Realtime Database y a los temas de notificación de FCM. */
public final class Firebase {

    /** Comunidad de esta app: separa datos (Realtime Database) y tópicos (MQTT y FCM). */
    public static final String COMUNIDAD = "otello";
    /** Tema FCM al que se envían los anuncios del administrador. */
    public static final String TEMA_ANUNCIOS = "comunidad-" + COMUNIDAD + "-anuncios";

    private static final String URL = "https://vecinosotello-default-rtdb.firebaseio.com"; // us-central1
    private static FirebaseDatabase db;

    private Firebase() { }

    public static synchronized FirebaseDatabase db() {
        if (db == null) {
            db = FirebaseDatabase.getInstance(URL);
            db.setPersistenceEnabled(true); // copia local: se ve sin conexión y se envía al volver
        }
        return db;
    }

    static void suscribirAnuncios() {
        FirebaseMessaging.getInstance().subscribeToTopic(TEMA_ANUNCIOS);
    }

    static void desuscribirAnuncios() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(TEMA_ANUNCIOS);
    }
}
