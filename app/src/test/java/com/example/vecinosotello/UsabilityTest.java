package com.example.vecinosotello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Pruebas de usabilidad y accesibilidad textual: claridad de mensajes, formato de precios y descripciones de alertas. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class UsabilityTest {

    @Test
    public void mensajesDeErrorSonClaroyAccesibles() {
        assertEquals("Escribe tu nombre.", Validaciones.validarRegistro("", "311A", "a@b.cl", "Casa3538", "Casa3538"));
        assertEquals("Escribe tu correo.", Validaciones.validarRegistro("Juan", "311A", "", "Casa3538", "Casa3538"));
        assertEquals("La vivienda debe ser un número con hasta 2 letras, por ejemplo 311A.", Validaciones.validarRegistro("Juan", "INVALID", "a@b.cl", "Casa3538", "Casa3538"));
    }

    @Test
    public void formatoDePreciosYFechasEsAmigable() {
        assertEquals("$ 45.000", Validaciones.precioBonito(45000));
        assertEquals("Empanadas", Ventas.filtrar(java.util.List.of(new Ventas.Venta("v1", "u", "Benja", "311A", "Empanadas", "Ricas", "Comida", 2500, false, 0L)), "Comida", false).get(0).titulo);
    }

    @Test
    public void descripcionDeAlertaEsClaraParaLectoresDePantalla() {
        Alerta alerta = Alerta.nueva("Incendio o humo", "uid1", "María", "102B", 1000L);
        String desc = alerta.descripcion();
        assertTrue(desc.contains("María"));
        assertTrue(desc.contains("102B"));
        assertTrue(desc.contains("Incendio o humo"));
    }
}
