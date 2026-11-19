package com.example.vecinosotello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DatosTest {

    private static final String FORMATO = "Usa el correo de tu casa: otello + número + sanjoaquin@gmail.com";

    @Test
    public void cadaCasaConNumeroPuedeEntrarConSuCorreo() {
        assertNull(Datos.validar("otello3538SanJoaquin@gmail.com", "Casa3538"));
        assertNull(Datos.validar("otello3530sanjoaquin@gmail.com", "abc12345"));
        assertNull(Datos.validar(" otello3598sanjoaquin@gmail.com ", "Pinar2026x"));
    }

    @Test
    public void correoConOtroFormatoNoSirve() {
        assertEquals(FORMATO, Datos.validar("vecino@gmail.com", "Casa3538"));
        assertEquals(FORMATO, Datos.validar("otello3538@gmail.com", "Casa3538"));
    }

    @Test
    public void numeroQueNoEstaEnElPasaje() {
        assertEquals("Ese número de casa no está registrado en el pasaje.",
                Datos.validar("otello9999sanjoaquin@gmail.com", "Casa3538"));
    }

    @Test
    public void claveConLetrasYNumeros() {
        String correo = "otello3538sanjoaquin@gmail.com";
        assertEquals(Datos.MENSAJE_CLAVE, Datos.validar(correo, "Ab1"));        // muy corta
        assertEquals(Datos.MENSAJE_CLAVE, Datos.validar(correo, "12345678"));   // sin letras
        assertEquals(Datos.MENSAJE_CLAVE, Datos.validar(correo, "abcdefgh"));   // sin números
        assertEquals(Datos.MENSAJE_CLAVE, Datos.validar(correo, "abc 1234"));   // con espacio
    }

    @Test
    public void laCasaSaleDelCorreo() {
        assertEquals("3538", Datos.numeroDelCorreo("otello3538SanJoaquin@gmail.com"));
        Datos.miNumero = "3538";
        assertNotNull(Datos.miCasa());
        assertEquals("E2", Datos.miCasa().codigo);
        Datos.miNumero = "3531";
        assertEquals("O1", Datos.miCasa().codigo);
    }

    @Test
    public void alertaDelChatMarcaLaCasa() {
        Datos.alertas.clear();
        Datos.marcarAlerta("N° 3530");
        assertEquals(1, Datos.alertas.size());
        Datos.marcarAlerta("Vecino sin número");
        assertEquals(1, Datos.alertas.size());
    }

    @Test
    public void alertasSuperUrgentes() {
        assertEquals(true, Datos.esUrgente("Necesito ayuda en el N° 3538"));
        assertEquals(true, Datos.esUrgente("Robo o intento de robo en el N° 3530"));
        assertEquals(true, Datos.esUrgente("Humo o incendio"));               // chip rápido del chat
        assertEquals(true, Datos.esUrgente("Emergencia médica en el N° 3598"));
        assertEquals(false, Datos.esUrgente("Persona sospechosa en el N° 3531"));
        assertEquals(false, Datos.esUrgente("Corte de luz"));
    }

    @Test
    public void hayVeintisieteCasas() {
        assertEquals(27, Datos.casas.size());
    }
}
