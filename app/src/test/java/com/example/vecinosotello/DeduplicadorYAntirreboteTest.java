package com.example.vecinosotello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Recepción repetida del mismo evento MQTT y prevención de activaciones repetidas de la alarma. */
public class DeduplicadorYAntirreboteTest {

    @Test
    public void elMismoIdSoloSeProcesaUnaVez() {
        Deduplicador d = new Deduplicador(100);
        assertTrue(d.esNueva("alerta-1"));
        assertFalse(d.esNueva("alerta-1")); // copia repetida por QoS 1
        assertFalse(d.esNueva("alerta-1"));
        assertTrue(d.esNueva("alerta-2"));
        assertFalse(d.esNueva(null));
    }

    @Test
    public void guardaComoMaximoLaCapacidad() {
        Deduplicador d = new Deduplicador(2);
        d.esNueva("a");
        d.esNueva("b");
        d.esNueva("c");                 // "a" sale de la memoria
        assertTrue(d.esNueva("a"));
        assertFalse(d.esNueva("c"));
    }

    @Test
    public void antirreboteBloqueaUnMinutoDespuesDeEnviar() {
        Antirrebote a = new Antirrebote(60_000);
        assertTrue(a.puede(1_000_000));
        a.marcar(1_000_000);
        assertFalse(a.puede(1_030_000));
        assertEquals(30, a.segundosRestantes(1_030_000));
        assertTrue(a.puede(1_060_000));
    }
}
