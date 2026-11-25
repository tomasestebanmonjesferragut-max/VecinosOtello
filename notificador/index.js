/*
 * Notificador de anuncios (entorno de confianza).
 *
 * La app NO puede enviar notificaciones: solo guarda el anuncio en Realtime Database (y las reglas
 * permiten hacerlo únicamente al administrador). Este programa corre en un equipo de confianza con
 * la clave de servicio de Firebase, escucha los anuncios nuevos y envía un mensaje FCM al tema
 * "comunidad-otello-anuncios". Al tocar la notificación, la app abre ese anuncio.
 *
 * Uso:
 *   1. Firebase → Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada.
 *      Guardarla como notificador/clave-servicio.json (está en .gitignore: nunca se sube a Git).
 *   2. npm install
 *   3. npm start
 */
const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const RUTA_CLAVE = process.env.GOOGLE_APPLICATION_CREDENTIALS || path.join(__dirname, "clave-servicio.json");
const TEMA = "comunidad-otello-anuncios";

if (!fs.existsSync(RUTA_CLAVE)) {
  console.error("Falta la clave de servicio en " + RUTA_CLAVE + " (ver instrucciones al inicio de index.js).");
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(require(RUTA_CLAVE)),
  databaseURL: "https://vecinosotello-default-rtdb.firebaseio.com",
});

const inicio = Date.now();
const enviados = new Set(); // evita notificar dos veces el mismo anuncio si Firebase repite el evento

console.log("Notificador activo. Esperando anuncios nuevos…");

admin.database().ref("anuncios").orderByChild("fecha").startAt(inicio)
  .on("child_added", async (snap) => {
    const a = snap.val();
    if (!a || enviados.has(snap.key)) return;
    enviados.add(snap.key);
    const contenido = String(a.contenido || "");
    try {
      const id = await admin.messaging().send({
        topic: TEMA,
        // Mensaje de datos con prioridad alta: lo procesa MensajeriaService aunque la app esté cerrada
        data: {
          tipo: "anuncio",
          anuncioId: snap.key,
          titulo: String(a.titulo || "Anuncio"),
          contenido: contenido.length > 200 ? contenido.slice(0, 197) + "…" : contenido,
          importante: String(a.importante === true),
        },
        android: { priority: "high", ttl: 24 * 60 * 60 * 1000 },
      });
      console.log(new Date().toLocaleString() + "  anuncio " + snap.key + " notificado (" + id + ")");
    } catch (e) {
      enviados.delete(snap.key);
      console.error("No se pudo notificar el anuncio " + snap.key + ": " + e.message);
    }
  }, (e) => {
    console.error("Se perdió el acceso a Realtime Database: " + e.message);
    process.exit(1);
  });
