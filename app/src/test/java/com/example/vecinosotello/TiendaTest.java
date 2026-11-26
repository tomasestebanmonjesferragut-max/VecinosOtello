package com.example.vecinosotello;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Tienda: producto o servicio, calificación con estrellas y "ya no disponible". */
public class TiendaTest {

    @Test
    public void soloProductoOServicio() {
        assertTrue(Validaciones.esTipo("Producto"));
        assertTrue(Validaciones.esTipo("Servicio"));
        assertFalse(Validaciones.esTipo("Arriendo"));
        assertFalse(Validaciones.esTipo(null));
    }

    @Test
    public void estrellasDeUnoACinco() {
        assertTrue(Validaciones.estrellasValidas(1));
        assertTrue(Validaciones.estrellasValidas(5));
        assertFalse(Validaciones.estrellasValidas(0));
        assertFalse(Validaciones.estrellasValidas(6));
    }

    @Test
    public void promedioDeEstrellas() {
        assertEquals(4.0, Ventas.promedio(Arrays.asList(5L, 3L, 4L)), 0.001);
        assertEquals(4.5, Ventas.promedio(Arrays.asList(5L, 4L)), 0.001);
        assertEquals(0.0, Ventas.promedio(Collections.emptyList()), 0.001);
    }

    @Test
    public void textoDeEstrellas() {
        assertEquals("★ 4,5 (2)", Ventas.textoEstrellas(Arrays.asList(5L, 4L)));
        assertEquals("Sin calificaciones", Ventas.textoEstrellas(Collections.emptyList()));
    }

    @Test
    public void noDisponibleSeOcultaConElFiltro() {
        Ventas.Venta disponible = new Ventas.Venta("a", "u", "Benja", "311A", "Corte de pelo", "", "Servicios", 5000, false, 0L, "Servicio");
        Ventas.Venta agotado = new Ventas.Venta("b", "u", "Benja", "311A", "Pan", "", "Comida", 1000, true, 0L, "Producto");
        assertEquals(1, Ventas.filtrar(Arrays.asList(disponible, agotado), null, true).size());
        assertEquals("Servicio", disponible.tipo);
    }

    @Test
    public void ventaAntiguaSinTipoEsProducto() {
        assertEquals("Producto", new Ventas.Venta("c", "u", "B", "1", "Pan", "", "Comida", 1, false, 0L).tipo);
    }

    @Test
    public void validarTipoAlPublicar() {
        assertNull(Validaciones.validarTipo("Servicio"));
        assertEquals("Elige si es producto o servicio.", Validaciones.validarTipo(null));
    }
}
