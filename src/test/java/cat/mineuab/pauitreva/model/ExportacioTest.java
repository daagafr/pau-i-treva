package cat.mineuab.pauitreva.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
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
                        new Exportacio.Membre("Anty48", Rang.MINISTRE)),
                        Map.of("minecraft:overworld", List.of(new int[] {1, 2}, new int[] {3, -4})))
                : null, T0 + 5);

        assertTrue(json.contains("\"territori\":{\"minecraft:overworld\":[[1,2],[3,-4]]}"));
        assertTrue(json.contains("\"territori\":{}")); // Occitània, sense party

        assertTrue(json.startsWith("{\"versio\":2,\"generat\":" + (T0 + 5) + ",\"finestres\":{\"oberta\":"));
        assertTrue(json.contains("\"humiliatFins\":null"));
        assertTrue(json.contains("\"guerres\":[]"));
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
        Exportacio.Finestres f = new Exportacio.Finestres(false, -1, 1000, "18:00-23:00", "12:00-23:00", "Europe/Madrid");
        assertEquals("{\"versio\":2,\"generat\":0,\"finestres\":{\"oberta\":false,\"tancament\":null,"
                        + "\"properaObertura\":1000,\"feiners\":\"18:00-23:00\",\"capDeSetmana\":\"12:00-23:00\","
                        + "\"zona\":\"Europe/Madrid\"},\"paisos\":[],\"relacions\":[],\"guerres\":[],\"diari\":[]}",
                Exportacio.estat(new Registre(), id -> null, 0, f));
    }

    @Test
    void guerraALApi() {
        Registre r = new Registre();
        UUID cat = UUID.randomUUID();
        UUID occ = UUID.randomUUID();
        r.fundar(cat, "Catalunya", "CAT", 0xFFCC00, T0);
        r.fundar(occ, "Occitània", "OCC", 0xCC0000, T0);
        Chunk a = new Chunk("minecraft:overworld", 4, -2);
        r.declararGuerra(cat, occ, TipusObjectiu.CONQUESTA, java.util.Set.of(a), T0, ParametresGuerra.perDefecte(),
                null, new Chunk("minecraft:overworld", 9, 9));
        Guerra g = r.guerraEntre(cat, occ).orElseThrow();
        r.capturar(g.id(), List.of(new Registre.Presencia(a, 1, 0)), 125, ParametresGuerra.perDefecte());
        String json = Exportacio.estat(r, id -> null, T0);
        assertTrue(json.contains("\"atacant\":\"CAT\",\"defensor\":\"OCC\",\"objectiu\":\"conquesta\""));
        assertTrue(json.contains("\"chunks\":[{\"dimensio\":\"minecraft:overworld\",\"x\":4,\"z\":-2,\"control\":50}]"));
        assertTrue(json.contains("\"capitals\":{\"atacant\":null,\"defensor\":{\"dimensio\":\"minecraft:overworld\",\"x\":9,\"z\":9,\"control\":0}}"));
        assertTrue(json.contains("\"activa\":false"));
        assertEquals(count(json, '{'), count(json, '}'));
        assertEquals(count(json, '['), count(json, ']'));
    }

    private static long count(String s, char c) {
        return s.chars().filter(x -> x == c).count();
    }
}
