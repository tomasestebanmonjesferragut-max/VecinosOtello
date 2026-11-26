package com.example.vecinosotello;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Pruebas de interfaz (Espresso) del inicio de sesión y el registro. No usan la red:
 * comprueban que la app informa errores comprensibles y no avanza con datos inválidos.
 * Ejecutar en un teléfono o emulador desde Android Studio (clic derecho → Run 'InicioSesionUiTest').
 */
@RunWith(AndroidJUnit4.class)
public class InicioSesionUiTest {

    @BeforeClass
    public static void sinSesion() {
        FirebaseAuth.getInstance().signOut(); // que la pantalla de inicio no entre sola
    }

    @Rule
    public ActivityScenarioRule<LoginActivity> regla = new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void entrarConCamposVaciosMuestraError() {
        onView(withId(R.id.entrar)).perform(scrollTo(), click());
        onView(withId(R.id.error)).check(matches(withText("Escribe tu correo.")));
    }

    @Test
    public void correoInvalidoMuestraError() {
        onView(withId(R.id.correo)).perform(replaceText("vecino-sin-arroba"), closeSoftKeyboard());
        onView(withId(R.id.entrar)).perform(scrollTo(), click());
        onView(withId(R.id.error)).check(matches(withText("El correo no es válido.")));
    }

    @Test
    public void elOjoMuestraYOcultaLaContrasena() {
        onView(withId(R.id.clave)).perform(replaceText("Casa3538"), closeSoftKeyboard());
        onView(withId(R.id.ver_clave)).perform(click());
        onView(withId(R.id.ver_clave)).check(matches(withContentDescription("Ocultar contraseña")));
        onView(withId(R.id.ver_clave)).perform(click());
        onView(withId(R.id.ver_clave)).check(matches(withContentDescription("Mostrar contraseña")));
    }

    @Test
    public void registroConCamposVaciosNoAvanza() {
        onView(withId(R.id.registrarme)).perform(scrollTo(), click());
        onView(withId(R.id.crear)).perform(scrollTo(), click());
        onView(withId(R.id.error)).check(matches(withText("Escribe tu nombre.")));
    }

    @Test
    public void registroAceptaVivienda311A() {
        onView(withId(R.id.registrarme)).perform(scrollTo(), click());
        onView(withId(R.id.nombre)).perform(replaceText("Profe Benja"));
        onView(withId(R.id.vivienda)).perform(replaceText("311A"), closeSoftKeyboard());
        onView(withId(R.id.crear)).perform(scrollTo(), click());
        // La vivienda pasó la validación: el siguiente error es el correo vacío
        onView(withId(R.id.error)).check(matches(withText("Escribe tu correo.")));
    }

    @Test
    public void registroRechazaViviendaInvalida() {
        onView(withId(R.id.registrarme)).perform(scrollTo(), click());
        onView(withId(R.id.nombre)).perform(replaceText("Profe Benja"));
        onView(withId(R.id.vivienda)).perform(replaceText("CASA"), closeSoftKeyboard());
        onView(withId(R.id.crear)).perform(scrollTo(), click());
        onView(withText("La vivienda debe ser un número con hasta 2 letras, por ejemplo 311A.")).check(matches(isDisplayed()));
    }
}
