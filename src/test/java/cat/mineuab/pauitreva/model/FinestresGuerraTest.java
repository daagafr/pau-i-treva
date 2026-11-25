package cat.mineuab.pauitreva.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class FinestresGuerraTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private final FinestresGuerra f = FinestresGuerra.perDefecte();

    private static long t(int any, int mes, int dia, int hora, int minut) {
        return LocalDateTime.of(any, mes, dia, hora, minut).atZone(MADRID).toInstant().toEpochMilli();
    }

    @Test
    void diesFeiners() {
        // Dimecres 25/11/2026
        assertFalse(f.oberta(t(2026, 11, 25, 17, 59)));
        assertTrue(f.oberta(t(2026, 11, 25, 18, 0)));
        assertTrue(f.oberta(t(2026, 11, 25, 22, 59)));
        assertFalse(f.oberta(t(2026, 11, 25, 23, 0)));
        assertFalse(f.oberta(t(2026, 11, 25, 12, 30)));
    }

    @Test
    void capDeSetmana() {
        // Dissabte 28 i diumenge 29/11/2026
        assertTrue(f.oberta(t(2026, 11, 28, 12, 0)));
        assertFalse(f.oberta(t(2026, 11, 28, 11, 59)));
        assertTrue(f.oberta(t(2026, 11, 29, 22, 30)));
        assertFalse(f.oberta(t(2026, 11, 29, 23, 0)));
    }

    @Test
    void properaObertura() {
        // Divendres a les 23:30 → dissabte a les 12:00
        assertEquals(t(2026, 11, 28, 12, 0), f.properaObertura(t(2026, 11, 27, 23, 30)));
        // Dilluns a les 10:00 → dilluns a les 18:00
        assertEquals(t(2026, 11, 30, 18, 0), f.properaObertura(t(2026, 11, 30, 10, 0)));
        // Ja oberta → ara mateix
        long ara = t(2026, 11, 30, 19, 0);
        assertEquals(ara, f.properaObertura(ara));
        assertEquals(t(2026, 11, 30, 23, 0), f.tancament(ara));
        assertEquals(-1, f.tancament(t(2026, 11, 30, 10, 0)));
    }

    @Test
    void canviDHora() {
        // Diumenge 25/10/2026 canvia l'hora a Europa: la finestra continua sent de 12:00 a 23:00 locals.
        assertTrue(f.oberta(t(2026, 10, 25, 12, 0)));
        assertFalse(f.oberta(t(2026, 10, 25, 11, 0)));
    }

    @Test
    void llegirLaConfig() {
        assertEquals(f, FinestresGuerra.llegir("18:00-23:00", "12:00-23:00", "Europe/Madrid").orElseThrow());
        assertTrue(FinestresGuerra.llegir("23:00-18:00", "12:00-23:00", "Europe/Madrid").isEmpty());
        assertTrue(FinestresGuerra.llegir("18-23", "12:00-23:00", "Europe/Madrid").isEmpty());
        assertTrue(FinestresGuerra.llegir("18:00-23:00", "12:00-23:00", "Lluna/Plena").isEmpty());
    }
}
