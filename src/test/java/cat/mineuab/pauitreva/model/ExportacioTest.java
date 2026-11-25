package cat.mineuab.pauitreva.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExportacioTest {

    private static final long T0 = 1_764_000_000_000L;

    @Test
    void estatAmbPaisosRelacionsIDiari() {
        Registre r = new Registre();
        UUID cat = UUID.randomUUID();
        UUID occ = UUID.randomUUID();
        r.fundar(cat, "Catalunya", "CAT", 0xFFCC00, T0);
        r.fundar(occ, "Occitània", "OCC", 0xCC0000, T0);
        r.canviarLema(cat, "Diu \"hola\" <b>");
        r.canviarNomRang(cat, Rang.LIDER, "President/a");
        r.proposar(cat, occ, TipusRelacio.PACTE, 3, T0, Registre.DIA);
        r.acceptar(occ, cat, T0);

        String json = Exportacio.estat(r, id -> id.equals(cat)
                ? new Exportacio.InfoPartit("Dagafr", List.of(new Exportacio.Membre("Dagafr", Rang.LIDER),
                        new Exportacio.Membre("Anty48", Rang.MINISTRE)))
                : null, T0 + 5);

        assertTrue(json.startsWith("{\"versio\":1,\"generat\":" + (T0 + 5)));
        assertTrue(json.contains("\"sigla\":\"CAT\""));
        assertTrue(json.contains("\"color\":\"#FFCC00\""));
        assertTrue(json.contains("\"lema\":\"Diu \\\"hola\\\" \\u003cb>\""));
        assertTrue(json.contains("{\"nom\":\"Dagafr\",\"rang\":\"lider\",\"titol\":\"President/a\"}"));
        assertTrue(json.contains("{\"nom\":\"Anty48\",\"rang\":\"ministre\",\"titol\":\"Ministre/a\"}"));
        assertTrue(json.contains("\"lider\":null")); // Occitània no té party a la prova
        assertTrue(json.contains("\"tipus\":\"pacte\""));
        assertTrue(json.contains("\"fins\":" + (T0 + 3 * Registre.DIA)));
        assertTrue(json.contains("\"tipus\":\"fundacio\""));
        assertFalse(json.contains("\n"));
        assertEquals(count(json, '{'), count(json, '}'));
        assertEquals(count(json, '['), count(json, ']'));
    }

    @Test
    void registreBuit() {
        assertEquals("{\"versio\":1,\"generat\":0,\"paisos\":[],\"relacions\":[],\"diari\":[]}",
                Exportacio.estat(new Registre(), id -> null, 0));
    }

    private static long count(String s, char c) {
        return s.chars().filter(x -> x == c).count();
    }
}
