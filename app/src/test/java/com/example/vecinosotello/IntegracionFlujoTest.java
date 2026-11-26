package com.example.vecinosotello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/** Prueba de integración del ciclo completo de envío, deduplicación y antirrebote de alertas. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class IntegracionFlujoTest {

    @Test
    public void flujoCompletoAlertaDeduplicacionYAntirrebote() {
        Transporte transporte = mock(Transporte.class);
        when(transporte.conectado()).thenReturn(true);
        doAnswer(inv -> { ((Transporte.Resultado) inv.getArgument(2)).ok(); return null; })
                .when(transporte).publicar(anyString(), any(), any());

        EnviadorAlertas.Registro registro = mock(EnviadorAlertas.Registro.class);
        Antirrebote antirrebote = new Antirrebote(60_000);
        EnviadorAlertas enviador = new EnviadorAlertas(transporte, registro, antirrebote);

        List<EnviadorAlertas.Estado> estados = new ArrayList<>();
        EnviadorAlertas.Aviso aviso = (e, a, d) -> estados.add(e);

        // Activar primera alerta
        Alerta alerta1 = enviador.activar("Necesito ayuda", "u1", "Benja", "311A", 1000L, aviso);
        assertNotNull(alerta1);
        assertEquals(EnviadorAlertas.Estado.ENVIADA, estados.get(estados.size() - 1));

        // Probar deduplicación del mensaje recibido por MQTT
        Deduplicador deduplicador = new Deduplicador(100);
        assertTrue(deduplicador.esNueva(alerta1.id));
        assertFalse(deduplicador.esNueva(alerta1.id)); // Duplicado detectado

        // Intentar segunda alerta inmediatamente (bloqueada por antirrebote)
        Alerta alerta2 = enviador.activar("Incendio o humo", "u1", "Benja", "311A", 2000L, aviso);
        assertNull(alerta2);
        assertEquals(EnviadorAlertas.Estado.ESPERAR, estados.get(estados.size() - 1));
    }
}
