package com.example.vecinosotello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Generación de alertas y formato del mensaje MQTT (Robolectric da la implementación real de org.json). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AlertaTest {

    @Test
    public void cadaAlertaTieneIdUnicoYDatosMinimos() {
        Alerta a = Alerta.nueva("Necesito ayuda", "uid1", "Profe Benja", "311A", 1000L);
        Alerta b = Alerta.nueva("Necesito ayuda", "uid1", "Profe Benja", "311A", 1000L);
        assertNotEquals(a.id, b.id);
        assertEquals("otello", a.comunidad);
        assertEquals("uid1", a.uid);
        assertEquals(1000L, a.fecha);
        assertEquals(Alerta.ACTIVA, a.estado);
    }

    @Test
    public void viajaPorMqttSinPerderDatos() {
        Alerta a = Alerta.nueva("Robo o intento de robo", "uid1", "Profe Benja", "311A", 123456789L);
        Alerta r = Alerta.desdeJson(a.aJson());
        assertEquals(a.id, r.id);
        assertEquals(a.comunidad, r.comunidad);
        assertEquals(a.uid, r.uid);
        assertEquals("Profe Benja", r.nombre);
        assertEquals("311A", r.vivienda);
        assertEquals(a.tipo, r.tipo);
        assertEquals(123456789L, r.fecha);
        assertEquals(Alerta.ACTIVA, r.estado);
    }

    @Test
    public void mensajesAjenosODanadosSeIgnoran() {
        assertNull(Alerta.desdeJson("hola"));
        assertNull(Alerta.desdeJson("{}"));
        assertNull(Alerta.desdeJson("{\"id\":\"x\",\"uid\":\"u\",\"nombre\":\"\",\"vivienda\":\"1\",\"tipo\":\"t\"}"));
    }

    @Test
    public void muestraLaIdentidadDelEmisor() {
        Alerta a = Alerta.nueva("Necesito ayuda", "uid1", "Profe Benja", "311A", 0);
        assertEquals("Profe Benja · Vivienda #311A ha solicitado ayuda (Necesito ayuda).", a.descripcion());
    }

    @Test
    public void alertasSuperUrgentes() {
        assertTrue(Alerta.nueva("Necesito ayuda", "u", "n", "1", 0).esUrgente());
        assertTrue(Alerta.nueva("Incendio o humo", "u", "n", "1", 0).esUrgente());
        assertTrue(Alerta.nueva("Emergencia médica", "u", "n", "1", 0).esUrgente());
        assertFalse(Alerta.nueva("Persona sospechosa", "u", "n", "1", 0).esUrgente());
        assertFalse(Alerta.nueva("Auto sospechoso", "u", "n", "1", 0).esUrgente());
        assertFalse(Alerta.nueva("Ñuñoíno detectado", "u", "n", "1", 0).esUrgente());
        assertFalse(Alerta.nueva("Extranjero cerca", "u", "n", "1", 0).esUrgente());
    }
}
