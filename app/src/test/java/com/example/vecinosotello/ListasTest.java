package com.example.vecinosotello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/** Filtros de ventas, orden de anuncios, fechas y casas del mapa. */
public class ListasTest {

    private static Ventas.Venta venta(String titulo, String categoria, boolean vendido) {
        return new Ventas.Venta(titulo, "u", "Benja", "311A", titulo, "", categoria, 1000, vendido, 0L);
    }

    @Test
    public void ventasSeFiltranPorCategoriaYDisponibilidad() {
        List<Ventas.Venta> todas = Arrays.asList(venta("Pan", "Comida", false), venta("Torta", "Comida", true),
                venta("Polera", "Ropa", false));
        assertEquals(3, Ventas.filtrar(todas, null, false).size());
        assertEquals(2, Ventas.filtrar(todas, "Comida", false).size());
        assertEquals(1, Ventas.filtrar(todas, "Comida", true).size());
        assertEquals(2, Ventas.filtrar(todas, null, true).size());
        assertEquals(0, Ventas.filtrar(todas, "Hogar", false).size());
    }

    @Test
    public void anunciosImportantesQuedanFijadosArriba() {
        List<Anuncios.Anuncio> orden = Anuncios.ordenar(Arrays.asList(
                new Anuncios.Anuncio("a", "Viejo", "x", "Admin", 100L, false),
                new Anuncios.Anuncio("b", "Nuevo", "x", "Admin", 300L, false),
                new Anuncios.Anuncio("c", "Importante viejo", "x", "Admin", 50L, true)));
        assertEquals("c", orden.get(0).id);
        assertEquals("b", orden.get(1).id);
        assertEquals("a", orden.get(2).id);
    }

    @Test
    public void fechasCortas() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.NOVEMBER, 25, 21, 4, 0);
        long hoy = c.getTimeInMillis();
        assertEquals("Hoy 21:04", Fechas.corta(hoy, hoy));
        assertEquals("Ayer 21:04", Fechas.corta(hoy - 24 * 60 * 60 * 1000L, hoy));
        assertEquals("Enviando…", Fechas.corta(null, hoy));
    }

    @Test
    public void lasCasasDelMapaSeBuscanPorVivienda() {
        assertEquals(27, Datos.casas.size());
        assertEquals("E2", Datos.casaPorNumero("3538").codigo);
        assertNull(Datos.casaPorNumero("311A")); // vivienda válida pero no dibujada en el mapa
        Sesion.vivienda = "3538";
        assertTrue(Datos.miCasa().esMia());
        Sesion.vivienda = null;
    }
}
