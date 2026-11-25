package cat.mineuab.pauitreva.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidacioTest {

    @Test
    void sigles() {
        assertEquals("CAT", Validacio.normalitzarSigla(" cat "));
        assertTrue(Validacio.siglaValida("CAT"));
        assertTrue(Validacio.siglaValida("U2"));
        assertFalse(Validacio.siglaValida("C"));
        assertFalse(Validacio.siglaValida("CATAL"));
        assertFalse(Validacio.siglaValida("CÀT"));
        assertFalse(Validacio.siglaValida("cat"));
    }

    @Test
    void noms() {
        assertTrue(Validacio.nomValid("Catalunya"));
        assertTrue(Validacio.nomValid("República d'Andorra"));
        assertTrue(Validacio.nomValid("Unió Col·lectivista"));
        assertTrue(Validacio.nomValid("Euskal Herria"));
        assertFalse(Validacio.nomValid("Ca"));
        assertFalse(Validacio.nomValid("1714"));          // ha de començar per lletra
        assertFalse(Validacio.nomValid("Nom<amb>coses"));
        assertFalse(Validacio.nomValid("x".repeat(Validacio.NOM_MAX + 1)));
        assertEquals("Hola món", Validacio.normalitzarText("  Hola    món "));
    }

    @Test
    void nomPerOpacPassaElFiltreD_opac() {
        for (String nom : new String[] {"Unió Col·lectivista", "St. Cugat", "República d'Andorra", "País (Nord)"}) {
            String o = Validacio.nomPerOpac(nom);
            assertTrue(Validacio.NOM_OPAC.matcher(o).matches(), o);
        }
        assertEquals("Unió Col-lectivista", Validacio.nomPerOpac("Unió Col·lectivista"));
        assertEquals("St Cugat", Validacio.nomPerOpac("St. Cugat"));
    }

    @Test
    void colors() {
        assertEquals(0xFFCC00, Validacio.color("#ffcc00").orElseThrow());
        assertEquals(0x00AA00, Validacio.color("00AA00").orElseThrow());
        assertTrue(Validacio.color("verd").isEmpty());
        assertTrue(Validacio.color("#12345").isEmpty());
        assertEquals("#00AA00", Validacio.colorText(0x00AA00));
    }

    @Test
    void banderesILemes() {
        assertTrue(Validacio.banderaValida(""));
        assertTrue(Validacio.banderaValida("mineuab:catalunya"));
        assertFalse(Validacio.banderaValida("Catalunya"));
        assertTrue(Validacio.lemaValid("Per la pau i la treva"));
        assertFalse(Validacio.lemaValid("x".repeat(Validacio.LEMA_MAX + 1)));
    }
}
