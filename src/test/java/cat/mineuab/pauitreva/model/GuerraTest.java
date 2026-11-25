package cat.mineuab.pauitreva.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuerraTest {

    private static final long T0 = 1_764_000_000_000L;
    private static final long PREP = 24 * Registre.HORA;
    private static final long TREVA = 7 * Registre.DIA;
    private static final String OW = "minecraft:overworld";

    private final UUID cat = UUID.randomUUID();
    private final UUID occ = UUID.randomUUID();
    private final Set<Chunk> quatre = Set.of(new Chunk(OW, 1, 1), new Chunk(OW, 1, 2), new Chunk(OW, 2, 1),
            new Chunk(OW, 2, 2));
    private Registre r;

    @BeforeEach
    void prepara() {
        r = new Registre();
        r.fundar(cat, "Catalunya", "CAT", 0xFFCC00, T0);
        r.fundar(occ, "Occitània", "OCC", 0xCC0000, T0);
    }

    private Guerra declara() {
        assertEquals(Resultat.DECLARADA, r.declararGuerra(cat, occ, TipusObjectiu.CONQUESTA, quatre, T0, PREP, 20));
        return r.guerraEntre(cat, occ).orElseThrow();
    }

    @Test
    void declararPosaLaRelacioEnGuerra() {
        Guerra g = declara();
        assertEquals(TipusRelacio.GUERRA, r.relacio(cat, occ));
        assertFalse(g.activa(T0 + PREP - 1));
        assertTrue(g.activa(T0 + PREP));
        assertEquals(Esdeveniment.GUERRA, r.darrers(1).get(0).tipus());
        assertTrue(r.darrers(1).get(0).text().contains("conquerir 4 chunks"));
        assertEquals(Resultat.EN_GUERRA, r.declararGuerra(occ, cat, TipusObjectiu.HUMILIACIO, Set.of(), T0, PREP, 20));
        assertEquals(Resultat.EN_GUERRA, r.proposar(cat, occ, TipusRelacio.ALIANCA, 0, T0, Registre.DIA));
        assertEquals(Resultat.EN_GUERRA, r.trencar(cat, occ, T0));
    }

    @Test
    void noEsPotDeclararAAliatsNiAmbPacte() {
        r.proposar(cat, occ, TipusRelacio.ALIANCA, 0, T0, Registre.DIA);
        r.acceptar(occ, cat, T0);
        assertEquals(Resultat.ALIATS, r.declararGuerra(cat, occ, TipusObjectiu.HUMILIACIO, Set.of(), T0, PREP, 20));
        r.trencar(cat, occ, T0);
        r.proposar(cat, occ, TipusRelacio.PACTE, 5, T0, Registre.DIA);
        r.acceptar(occ, cat, T0);
        assertEquals(Resultat.EN_PACTE, r.declararGuerra(cat, occ, TipusObjectiu.HUMILIACIO, Set.of(), T0, PREP, 20));
    }

    @Test
    void objectiusValids() {
        assertEquals(Resultat.OBJECTIU_INVALID, r.declararGuerra(cat, occ, TipusObjectiu.CONQUESTA, Set.of(), T0, PREP, 20));
        assertEquals(Resultat.OBJECTIU_INVALID, r.declararGuerra(cat, occ, TipusObjectiu.CONQUESTA, quatre, T0, PREP, 3));
        assertEquals(Resultat.OBJECTIU_INVALID, r.declararGuerra(cat, occ, TipusObjectiu.HUMILIACIO, quatre, T0, PREP, 20));
        assertEquals(Resultat.MATEIX_PAIS, r.declararGuerra(cat, cat, TipusObjectiu.HUMILIACIO, Set.of(), T0, PREP, 20));
    }

    @Test
    void puntsLimitatsIDelPuntDeVistaDeCadaPais() {
        Guerra g = declara();
        r.sumarPunts(g.id(), cat, 30);
        r.sumarPunts(g.id(), occ, 10);
        assertEquals(20, g.puntsDe(cat));
        assertEquals(-20, g.puntsDe(occ));
        r.sumarPunts(g.id(), cat, 500);
        assertEquals(Guerra.PUNTS_MAX, g.punts());
        assertEquals(Resultat.TIPUS_INVALID, r.sumarPunts(g.id(), UUID.randomUUID(), 5));
    }

    @Test
    void imposarLaPauCostaPunts() {
        Guerra g = declara();
        Termes dos = new Termes(Set.of(new Chunk(OW, 1, 1), new Chunk(OW, 1, 2)), false);
        assertEquals(50, dos.cost(g));
        r.sumarPunts(g.id(), cat, 40);
        assertEquals(Resultat.PUNTS_INSUFICIENTS, r.ferPau(g.id(), cat, dos, true, T0 + PREP, TREVA));
        r.sumarPunts(g.id(), cat, 10);
        assertEquals(Resultat.PAU_SIGNADA, r.ferPau(g.id(), cat, dos, true, T0 + PREP, TREVA));
        assertTrue(r.guerres().isEmpty());
        assertEquals(TipusRelacio.TREVA, r.relacio(cat, occ));
        assertTrue(r.darrers(1).get(0).text().contains("cedeix 2 chunks"));
        // Durant la treva no hi ha guerra ni pacte, però sí aliança.
        assertEquals(Resultat.EN_TREVA, r.declararGuerra(cat, occ, TipusObjectiu.HUMILIACIO, Set.of(), T0 + PREP, PREP, 20));
        assertEquals(1, r.caducar(T0 + PREP + TREVA).size());
        assertEquals(TipusRelacio.NEUTRAL, r.relacio(cat, occ));
    }

    @Test
    void elsChunksHanDeSerDeLObjectiuINomesPerAlAtacant() {
        Guerra g = declara();
        r.sumarPunts(g.id(), occ, 100);
        Termes aliens = new Termes(Set.of(new Chunk(OW, 9, 9)), false);
        assertEquals(Resultat.OBJECTIU_INVALID, r.ferPau(g.id(), cat, aliens, false, T0, TREVA));
        Termes delDefensor = new Termes(Set.of(new Chunk(OW, 1, 1)), false);
        assertEquals(Resultat.OBJECTIU_INVALID, r.ferPau(g.id(), occ, delDefensor, true, T0, TREVA));
        // El defensor que guanya pot humiliar l'atacant.
        assertEquals(Resultat.PAU_SIGNADA, r.ferPau(g.id(), occ, new Termes(Set.of(), true), true, T0, TREVA));
        assertTrue(r.darrers(1).get(0).text().contains("queda humiliat"));
    }

    @Test
    void pauNegociada() {
        Guerra g = declara();
        Termes tot = new Termes(quatre, true);
        assertEquals(Resultat.PAU_PROPOSADA, r.proposarPau(cat, g.id(), cat, tot, T0));
        assertEquals(Resultat.NO_HI_HA_PROPOSTA, r.acceptarPau(cat, g.id(), T0, TREVA)); // no es pot acceptar la pròpia
        // Sense punts, però l'altre hi està d'acord.
        assertEquals(Resultat.PAU_SIGNADA, r.acceptarPau(occ, g.id(), T0, TREVA));
        assertTrue(r.propostaPau(g.id()).isEmpty());
    }

    @Test
    void rendirse() {
        Guerra g = declara();
        assertEquals(Resultat.PAU_SIGNADA, r.rendir(occ, g.id(), T0, TREVA));
        assertTrue(r.darrers(1).get(0).text().contains("cedeix 4 chunks"));
    }

    @Test
    void dissoldreAcabaLesGuerres() {
        Guerra g = declara();
        r.proposarPau(occ, g.id(), occ, Termes.blanca(), T0);
        r.dissoldre(occ, "", T0);
        assertTrue(r.guerres().isEmpty());
        assertTrue(r.totesLesPropostesPau().isEmpty());
    }
}
