package com.example.vecinosotello;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pruebas de rendimiento: verifica que las operaciones críticas (deduplicación masiva y validaciones) se ejecutan eficientemente. */
public class RendimientoTest {

    @Test
    public void deduplicacionMasivaEsRapida() {
        Deduplicador deduplicador = new Deduplicador(5000);
        long inicio = System.currentTimeMillis();
        for (int i = 0; i < 10_000; i++) {
            deduplicador.esNueva("alerta-" + (i % 2000));
        }
        long duracion = System.currentTimeMillis() - inicio;
        // 10,000 inserciones/consultas deben tomar menos de 250 ms
        assertTrue("La deduplicación masiva tardó demasiado: " + duracion + "ms", duracion < 250);
    }

    @Test
    public void validacionesMasivasSonRapidas() {
        long inicio = System.currentTimeMillis();
        for (int i = 0; i < 5_000; i++) {
            Validaciones.validarRegistro("Vecino " + i, "311A", "vecino" + i + "@gmail.com", "Casa3538", "Casa3538");
            Validaciones.validarMensaje("Mensaje de prueba número " + i);
        }
        long duracion = System.currentTimeMillis() - inicio;
        assertTrue("Las validaciones masivas tardaron demasiado: " + duracion + "ms", duracion < 250);
    }
}
