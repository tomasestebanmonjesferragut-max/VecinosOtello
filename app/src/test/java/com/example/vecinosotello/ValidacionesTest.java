package com.example.vecinosotello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Validación del registro, el chat, las ventas y los anuncios (casos de aceptación del enunciado). */
public class ValidacionesTest {

    private static final String CLAVE = "Casa3538";

    // ---- Registro ----

    @Test
    public void registroConCamposVaciosInformaElError() {
        assertEquals("Escribe tu nombre.", Validaciones.validarRegistro("", "", "", "", ""));
        assertEquals("Escribe el número de tu vivienda.", Validaciones.validarRegistro("Profe Benja", " ", "", "", ""));
        assertEquals("Escribe tu correo.", Validaciones.validarRegistro("Profe Benja", "311A", "", "", ""));
        assertEquals("Escribe tu contraseña.", Validaciones.validarRegistro("Profe Benja", "311A", "a@b.cl", "", ""));
    }

    @Test
    public void registroConVivienda311AEsValido() {
        assertNull(Validaciones.validarRegistro("Profe Benja", "311A", "benja@gmail.com", CLAVE, CLAVE));
        assertTrue(Validaciones.viviendaValida("311a"));                 // se acepta en minúscula
        assertEquals("311A", Validaciones.normalizarVivienda(" 311a ")); // y se guarda en mayúscula
        assertTrue(Validaciones.viviendaValida("3538"));
        assertTrue(Validaciones.viviendaValida("12BC"));
    }

    @Test
    public void viviendaConFormatoIncorrectoSeRechaza() {
        assertFalse(Validaciones.viviendaValida("A311"));
        assertFalse(Validaciones.viviendaValida("311ABC"));
        assertFalse(Validaciones.viviendaValida("31 1"));
        assertFalse(Validaciones.viviendaValida("123456"));
        assertNotNull(Validaciones.validarRegistro("Profe Benja", "Casa 5", "benja@gmail.com", CLAVE, CLAVE));
    }

    @Test
    public void nombreCorreoYClave() {
        assertNotNull(Validaciones.validarNombre("A"));
        assertNull(Validaciones.validarNombre("Benja"));
        assertNotNull(Validaciones.validarCorreo("benja@gmail"));
        assertNotNull(Validaciones.validarCorreo("benja gmail.com"));
        assertNull(Validaciones.validarCorreo("benja@gmail.com"));
        assertNotNull(Validaciones.validarClave("12345678"));  // sin letras
        assertNotNull(Validaciones.validarClave("abcdefgh"));  // sin números
        assertNotNull(Validaciones.validarClave("abc12"));     // muy corta
        assertNull(Validaciones.validarClave(CLAVE));
        assertEquals("Las contraseñas no coinciden.",
                Validaciones.validarRegistro("Benja", "311A", "benja@gmail.com", CLAVE, "Casa3539"));
    }

    @Test
    public void identidadComoEnElEnunciado() {
        assertEquals("Profe Benja · #311A", Validaciones.identidad("Profe Benja", "311A"));
    }

    // ---- Chat ----

    @Test
    public void mensajeVacioOLargoSeRechaza() {
        assertNotNull(Validaciones.validarMensaje("   "));
        assertNotNull(Validaciones.validarMensaje(new String(new char[501]).replace('\0', 'a')));
        assertNull(Validaciones.validarMensaje("Hola vecinos"));
    }

    // ---- Ventas ----

    @Test
    public void camposDeVentas() {
        assertNull(Validaciones.validarVenta("Empanadas", "2500", "Comida", ""));
        assertNull(Validaciones.validarVenta("Bicicleta", "$ 45.000", "Otros", "Aro 26"));
        assertEquals("Escribe el precio.", Validaciones.validarVenta("Empanadas", "", "Comida", ""));
        assertNotNull(Validaciones.validarVenta("Empanadas", "2500,5", "Comida", ""));   // sin decimales
        assertNotNull(Validaciones.validarVenta("Empanadas", "-100", "Comida", ""));
        assertNotNull(Validaciones.validarVenta("Empanadas", "2500", "Autos", ""));      // categoría inexistente
        assertNotNull(Validaciones.validarVenta("E", "2500", "Comida", ""));             // título muy corto
        assertEquals(Long.valueOf(45000), Validaciones.precioDesdeTexto("$ 45.000"));
        assertNull(Validaciones.precioDesdeTexto("999999999999"));                     // demasiado alto
        assertEquals("$ 45.000", Validaciones.precioBonito(45000));
    }

    // ---- Anuncios ----

    @Test
    public void camposDeAnuncios() {
        assertNull(Validaciones.validarAnuncio("Corte de agua", "El jueves de 9 a 14 h"));
        assertEquals("Escribe el título del anuncio.", Validaciones.validarAnuncio(" ", "Algo"));
        assertEquals("Escribe el contenido del anuncio.", Validaciones.validarAnuncio("Título", ""));
    }
}
