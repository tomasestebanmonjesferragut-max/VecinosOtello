package com.example.vecinosotello;

import java.util.Locale;

/**
 * Reglas de validación de formularios (registro, chat, ventas y anuncios).
 * Devuelven null si todo está bien o un mensaje de error comprensible para el usuario.
 * Las mismas reglas se repiten en database.rules.json para que no se puedan saltar desde fuera de la app.
 */
public final class Validaciones {

    public static final int NOMBRE_MIN = 2, NOMBRE_MAX = 40;
    public static final int MENSAJE_MAX = 500;
    public static final int TITULO_VENTA_MAX = 60, DESCRIPCION_MAX = 300;
    public static final int TITULO_ANUNCIO_MAX = 80, CONTENIDO_ANUNCIO_MAX = 1000;
    public static final long PRECIO_MAX = 100_000_000L;
    public static final String[] CATEGORIAS = {"Comida", "Ropa", "Hogar", "Tecnología", "Servicios", "Otros"};

    private Validaciones() { }

    /** Vivienda: 1 a 5 números y hasta 2 letras al final (ej. 3538, 311A). Se guarda en mayúsculas. */
    public static String normalizarVivienda(String vivienda) {
        return vivienda == null ? "" : vivienda.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean viviendaValida(String vivienda) {
        return normalizarVivienda(vivienda).matches("[0-9]{1,5}[A-Z]{0,2}");
    }

    public static String validarNombre(String nombre) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.isEmpty()) return "Escribe tu nombre.";
        if (n.length() < NOMBRE_MIN || n.length() > NOMBRE_MAX)
            return "El nombre debe tener entre " + NOMBRE_MIN + " y " + NOMBRE_MAX + " caracteres.";
        return null;
    }

    public static String validarCorreo(String correo) {
        String c = correo == null ? "" : correo.trim();
        if (c.isEmpty()) return "Escribe tu correo.";
        if (!c.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) return "El correo no es válido.";
        return null;
    }

    /** Clave de al menos 8 caracteres con letras y números. */
    public static String validarClave(String clave) {
        if (clave == null || clave.isEmpty()) return "Escribe tu contraseña.";
        if (clave.length() < 8 || !clave.matches(".*[A-Za-z].*") || !clave.matches(".*\\d.*"))
            return "La contraseña debe tener al menos 8 caracteres, con letras y números.";
        return null;
    }

    /** Registro: nombre, vivienda, correo, clave y su confirmación. */
    public static String validarRegistro(String nombre, String vivienda, String correo, String clave, String repetir) {
        String e = validarNombre(nombre);
        if (e != null) return e;
        if (normalizarVivienda(vivienda).isEmpty()) return "Escribe el número de tu vivienda.";
        if (!viviendaValida(vivienda)) return "La vivienda debe ser un número con hasta 2 letras, por ejemplo 311A.";
        e = validarCorreo(correo);
        if (e != null) return e;
        e = validarClave(clave);
        if (e != null) return e;
        if (!clave.equals(repetir)) return "Las contraseñas no coinciden.";
        return null;
    }

    public static String validarLogin(String correo, String clave) {
        String e = validarCorreo(correo);
        if (e != null) return e;
        return clave == null || clave.isEmpty() ? "Escribe tu contraseña." : null;
    }

    public static String validarMensaje(String texto) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) return "Escribe un mensaje.";
        if (t.length() > MENSAJE_MAX) return "El mensaje no puede pasar de " + MENSAJE_MAX + " caracteres.";
        return null;
    }

    /** Precio en pesos chilenos, entero (sin decimales para evitar errores de redondeo). */
    public static Long precioDesdeTexto(String texto) {
        String t = texto == null ? "" : texto.trim().replace(".", "").replace("$", "").replace(" ", "");
        if (!t.matches("[0-9]{1,9}")) return null;
        long p = Long.parseLong(t);
        return p <= PRECIO_MAX ? p : null;
    }

    public static String validarVenta(String titulo, String precio, String categoria, String descripcion) {
        String t = titulo == null ? "" : titulo.trim();
        if (t.length() < 2) return "Escribe qué vendes (al menos 2 letras).";
        if (t.length() > TITULO_VENTA_MAX) return "El título no puede pasar de " + TITULO_VENTA_MAX + " caracteres.";
        if (precio == null || precio.trim().isEmpty()) return "Escribe el precio.";
        if (precioDesdeTexto(precio) == null) return "El precio debe ser un número entero en pesos, por ejemplo 2500.";
        if (!esCategoria(categoria)) return "Elige una categoría.";
        if (descripcion != null && descripcion.trim().length() > DESCRIPCION_MAX)
            return "La descripción no puede pasar de " + DESCRIPCION_MAX + " caracteres.";
        return null;
    }

    /** Lo que se ofrece en la tienda. */
    public static final String[] TIPOS = {"Producto", "Servicio"};

    public static boolean esTipo(String tipo) {
        for (String t : TIPOS) if (t.equals(tipo)) return true;
        return false;
    }

    public static String validarTipo(String tipo) {
        return esTipo(tipo) ? null : "Elige si es producto o servicio.";
    }

    /** Calificación de un producto o servicio: de 1 a 5 estrellas. */
    public static boolean estrellasValidas(int estrellas) { return estrellas >= 1 && estrellas <= 5; }

    public static boolean esCategoria(String categoria) {
        for (String c : CATEGORIAS) if (c.equals(categoria)) return true;
        return false;
    }

    public static String validarAnuncio(String titulo, String contenido) {
        String t = titulo == null ? "" : titulo.trim(), c = contenido == null ? "" : contenido.trim();
        if (t.isEmpty()) return "Escribe el título del anuncio.";
        if (t.length() > TITULO_ANUNCIO_MAX) return "El título no puede pasar de " + TITULO_ANUNCIO_MAX + " caracteres.";
        if (c.isEmpty()) return "Escribe el contenido del anuncio.";
        if (c.length() > CONTENIDO_ANUNCIO_MAX) return "El contenido no puede pasar de " + CONTENIDO_ANUNCIO_MAX + " caracteres.";
        return null;
    }

    /** Identificación que se muestra en mensajes, ventas y alertas: "Profe Benja · #311A". */
    public static String identidad(String nombre, String vivienda) {
        return nombre + " · #" + vivienda;
    }

    /** $ 12.500 */
    public static String precioBonito(long precio) {
        return "$ " + String.format(Locale.ROOT, "%,d", precio).replace(',', '.');
    }
}
