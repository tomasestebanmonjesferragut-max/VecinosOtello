package com.example.vecinosotello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Flujo de activación de la alarma con el broker simulado (Mockito):
 * sin conexión no hay falso éxito, con conexión publica en el tópico de la comunidad y guarda el registro.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class EnviadorAlertasTest {

    private Transporte broker;
    private EnviadorAlertas.Registro registro;
    private EnviadorAlertas enviador;
    private final List<EnviadorAlertas.Estado> estados = new ArrayList<>();
    private final EnviadorAlertas.Aviso aviso = (e, a, d) -> estados.add(e);

    @Before
    public void preparar() {
        broker = mock(Transporte.class);
        registro = mock(EnviadorAlertas.Registro.class);
        enviador = new EnviadorAlertas(broker, registro, new Antirrebote(60_000));
    }

    private void brokerAcepta() {
        when(broker.conectado()).thenReturn(true);
        doAnswer(inv -> { ((Transporte.Resultado) inv.getArgument(2)).ok(); return null; })
                .when(broker).publicar(anyString(), any(), any());
    }

    @Test
    public void sinConexionInformaQueNoSeEnvio() {
        when(broker.conectado()).thenReturn(false);
        enviador.activar("Necesito ayuda", "uid1", "Profe Benja", "311A", 1_000, aviso);
        assertEquals(List.of(EnviadorAlertas.Estado.NO_ENVIADA), estados);
        verify(broker, never()).publicar(anyString(), any(), any());
        verify(registro, never()).guardar(any());
    }

    @Test
    public void conConexionPublicaQos1EnElTopicoYGuardaElRegistro() {
        brokerAcepta();
        Alerta a = enviador.activar("Necesito ayuda", "uid1", "Profe Benja", "311A", 1_000, aviso);
        ArgumentCaptor<byte[]> datos = ArgumentCaptor.forClass(byte[].class);
        verify(broker).publicar(eq("comunidades/otello/alertas"), datos.capture(), any());
        assertTrue(new String(datos.getValue(), StandardCharsets.UTF_8).contains(a.id));
        verify(registro).guardar(a);
        assertEquals(List.of(EnviadorAlertas.Estado.ENVIANDO, EnviadorAlertas.Estado.ENVIADA), estados);
    }

    @Test
    public void siElBrokerFallaNoSeGuardaNiSeMuestraExito() {
        when(broker.conectado()).thenReturn(true);
        doAnswer(inv -> { ((Transporte.Resultado) inv.getArgument(2)).fallo("sin respuesta"); return null; })
                .when(broker).publicar(anyString(), any(), any());
        enviador.activar("Necesito ayuda", "uid1", "Profe Benja", "311A", 1_000, aviso);
        assertEquals(List.of(EnviadorAlertas.Estado.ENVIANDO, EnviadorAlertas.Estado.NO_ENVIADA), estados);
        verify(registro, never()).guardar(any());
    }

    @Test
    public void elReintentoUsaElMismoId() {
        when(broker.conectado()).thenReturn(false);
        Alerta a = enviador.activar("Necesito ayuda", "uid1", "Profe Benja", "311A", 1_000, aviso);
        brokerAcepta();
        enviador.enviar(a, aviso); // botón "reintentar"
        verify(registro, times(1)).guardar(a);
        assertEquals(EnviadorAlertas.Estado.ENVIADA, estados.get(estados.size() - 1));
    }

    @Test
    public void noPermiteDosAlarmasSeguidas() {
        brokerAcepta();
        enviador.activar("Necesito ayuda", "uid1", "Profe Benja", "311A", 1_000, aviso);
        Alerta otra = enviador.activar("Necesito ayuda", "uid1", "Profe Benja", "311A", 20_000, aviso);
        assertNull(otra);
        assertEquals(EnviadorAlertas.Estado.ESPERAR, estados.get(estados.size() - 1));
        verify(broker, times(1)).publicar(anyString(), any(), any());
    }
}
