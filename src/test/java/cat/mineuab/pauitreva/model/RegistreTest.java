package cat.mineuab.pauitreva.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistreTest {

    private static final long T0 = 1_764_000_000_000L;
    private static final long CADUCITAT = 48 * Registre.HORA;

    private final UUID cat = UUID.randomUUID();
    private final UUID occ = UUID.randomUUID();
    private final UUID vas = UUID.randomUUID();
    private Registre r;

    @BeforeEach
    void prepara() {
        r = new Registre();
        assertEquals(Resultat.FET, r.fundar(cat, "Catalunya", "cat", 0xFFCC00, T0));
        assertEquals(Resultat.FET, r.fundar(occ, "Occitània", "OCC", 0xCC0000, T0));
        assertEquals(Resultat.FET, r.fundar(vas, "Euskal Herria", "EH", 0x00AA00, T0));
    }

    @Test
    void fundarNormalitzaISiglaUnica() {
        assertEquals("CAT", r.pais(cat).orElseThrow().sigla());
        assertTrue(r.perSigla("cat").isPresent());
        assertEquals(Resultat.SIGLA_OCUPADA, r.fundar(UUID.randomUUID(), "Una altra", "CAT", 0, T0));
        assertEquals(Resultat.NOM_OCUPAT, r.fundar(UUID.randomUUID(), "  catalunya ", "XYZ", 0, T0));
        assertEquals(Resultat.JA_EXISTEIX, r.fundar(cat, "Repetida", "REP", 0, T0));
        assertEquals(Resultat.SIGLA_INVALIDA, r.fundar(UUID.randomUUID(), "Bona", "MASSALLARGA", 0, T0));
        assertEquals(Resultat.NOM_INVALID, r.fundar(UUID.randomUUID(), "x", "XX", 0, T0));
        assertEquals(3, r.paisos().size());
        assertEquals(3, r.diari().size());
    }

    @Test
    void canviarNomISigla() {
        assertEquals(Resultat.NOM_OCUPAT, r.canviarNom(cat, "occitània", T0));
        assertEquals(Resultat.FET, r.canviarNom(cat, "Catalunya", T0)); // el mateix nom és seu
        assertEquals(Resultat.FET, r.canviarNom(cat, "Principat", T0));
        assertEquals("Principat", r.pais(cat).orElseThrow().nom());
        assertEquals(Resultat.SIGLA_OCUPADA, r.canviarSigla(cat, "occ", T0));
        assertEquals(Resultat.FET, r.canviarSigla(cat, "pr", T0));
        assertEquals("PR", r.pais(cat).orElseThrow().sigla());
    }

    @Test
    void aliancaEsMutua() {
        assertEquals(Resultat.PROPOSADA, r.proposar(cat, occ, TipusRelacio.ALIANCA, 0, T0, CADUCITAT));
        assertEquals(TipusRelacio.NEUTRAL, r.relacio(cat, occ));
        assertEquals(Resultat.JA_PROPOSAT, r.proposar(cat, occ, TipusRelacio.ALIANCA, 0, T0, CADUCITAT));
        assertEquals(1, r.propostesRebudes(occ).size());

        assertEquals(Resultat.ACCEPTADA, r.acceptar(occ, cat, T0 + 1000));
        assertEquals(TipusRelacio.ALIANCA, r.relacio(cat, occ));
        assertEquals(TipusRelacio.ALIANCA, r.relacio(occ, cat));
        assertTrue(r.propostesRebudes(occ).isEmpty());
        assertEquals(Resultat.JA_HO_SOU, r.proposar(occ, cat, TipusRelacio.ALIANCA, 0, T0, CADUCITAT));
        assertEquals(Resultat.JA_HO_SOU, r.proposar(occ, cat, TipusRelacio.PACTE, 5, T0, CADUCITAT));
    }

    @Test
    void proposarElMateixQueL_altreAccepta() {
        assertEquals(Resultat.PROPOSADA, r.proposar(cat, vas, TipusRelacio.PACTE, 7, T0, CADUCITAT));
        // L'altre proposa el mateix (amb uns altres dies): s'accepta la primera, amb els seus 7 dies.
        assertEquals(Resultat.ACCEPTADA, r.proposar(vas, cat, TipusRelacio.PACTE, 30, T0 + 10, CADUCITAT));
        EstatRelacio e = r.estat(cat, vas).orElseThrow();
        assertEquals(TipusRelacio.PACTE, e.tipus());
        assertEquals(T0 + 10 + 7 * Registre.DIA, e.fins());
    }

    @Test
    void elPacteS_acabaSol() {
        r.proposar(cat, occ, TipusRelacio.PACTE, 3, T0, CADUCITAT);
        r.acceptar(occ, cat, T0);
        assertTrue(r.caducar(T0 + 2 * Registre.DIA).isEmpty());
        assertEquals(TipusRelacio.PACTE, r.relacio(cat, occ));

        List<Esdeveniment> nous = r.caducar(T0 + 3 * Registre.DIA);
        assertEquals(1, nous.size());
        assertEquals(Esdeveniment.PACTE_ACABAT, nous.get(0).tipus());
        assertEquals(TipusRelacio.NEUTRAL, r.relacio(cat, occ));
    }

    @Test
    void unPactePotPassarAAlianca() {
        r.proposar(cat, occ, TipusRelacio.PACTE, 10, T0, CADUCITAT);
        r.acceptar(occ, cat, T0);
        assertEquals(Resultat.PROPOSADA, r.proposar(cat, occ, TipusRelacio.ALIANCA, 0, T0, CADUCITAT));
        assertEquals(Resultat.ACCEPTADA, r.acceptar(occ, cat, T0));
        EstatRelacio e = r.estat(cat, occ).orElseThrow();
        assertEquals(TipusRelacio.ALIANCA, e.tipus());
        assertEquals(0, e.fins());
        // Una aliança no s'acaba sola.
        assertTrue(r.caducar(T0 + 365 * Registre.DIA).isEmpty());
        assertEquals(TipusRelacio.ALIANCA, r.relacio(cat, occ));
    }

    @Test
    void lesPropostesCaduquen() {
        r.proposar(cat, occ, TipusRelacio.ALIANCA, 0, T0, CADUCITAT);
        assertEquals(Resultat.NO_HI_HA_PROPOSTA, r.acceptar(occ, cat, T0 + CADUCITAT));
        r.caducar(T0 + CADUCITAT);
        assertTrue(r.totesLesPropostes().isEmpty());
    }

    @Test
    void rebutjar() {
        r.proposar(cat, occ, TipusRelacio.ALIANCA, 0, T0, CADUCITAT);
        assertEquals(Resultat.NO_HI_HA_PROPOSTA, r.rebutjar(cat, occ)); // al revés no n'hi ha
        assertEquals(Resultat.FET, r.rebutjar(occ, cat));
        assertTrue(r.totesLesPropostes().isEmpty());
        assertEquals(TipusRelacio.NEUTRAL, r.relacio(cat, occ));
    }

    @Test
    void trencarPacteQuedaAlDiari() {
        r.proposar(cat, occ, TipusRelacio.PACTE, 10, T0, CADUCITAT);
        r.acceptar(occ, cat, T0);
        assertEquals(Resultat.TRENCAT_PACTE, r.trencar(cat, occ, T0 + 5));
        assertEquals(TipusRelacio.NEUTRAL, r.relacio(cat, occ));
        assertEquals(Esdeveniment.PACTE_TRENCAT, r.darrers(1).get(0).tipus());
        assertEquals(Resultat.NO_HI_HA_RES, r.trencar(cat, occ, T0 + 6));
    }

    @Test
    void trencarAlianca() {
        r.proposar(cat, occ, TipusRelacio.ALIANCA, 0, T0, CADUCITAT);
        r.acceptar(occ, cat, T0);
        assertEquals(Resultat.TRENCADA_ALIANCA, r.trencar(occ, cat, T0 + 5));
        assertEquals(TipusRelacio.NEUTRAL, r.relacio(cat, occ));
    }

    @Test
    void noEsPotTractarAmbUnMateix() {
        assertEquals(Resultat.MATEIX_PAIS, r.proposar(cat, cat, TipusRelacio.ALIANCA, 0, T0, CADUCITAT));
        assertEquals(Resultat.TIPUS_INVALID, r.proposar(cat, occ, TipusRelacio.GUERRA, 0, T0, CADUCITAT));
        assertEquals(Resultat.DIES_INVALIDS, r.proposar(cat, occ, TipusRelacio.PACTE, 0, T0, CADUCITAT));
        assertEquals(Resultat.NO_EXISTEIX, r.proposar(cat, UUID.randomUUID(), TipusRelacio.ALIANCA, 0, T0, CADUCITAT));
    }

    @Test
    void dissoldreTreuRelacionsIPropostes() {
        r.proposar(cat, occ, TipusRelacio.ALIANCA, 0, T0, CADUCITAT);
        r.acceptar(occ, cat, T0);
        r.proposar(vas, cat, TipusRelacio.PACTE, 5, T0, CADUCITAT);
        assertEquals(Resultat.FET, r.dissoldre(cat, "", T0 + 1));
        assertFalse(r.pais(cat).isPresent());
        assertTrue(r.totesLesRelacions().isEmpty());
        assertTrue(r.totesLesPropostes().isEmpty());
        assertEquals(Esdeveniment.DISSOLUCIO, r.darrers(1).get(0).tipus());
        // La sigla queda lliure.
        assertEquals(Resultat.FET, r.fundar(UUID.randomUUID(), "Nova Catalunya", "CAT", 0, T0 + 2));
    }

    @Test
    void permisosIRangs() {
        Pais p = r.pais(cat).orElseThrow();
        assertTrue(p.pot(Rang.MINISTRE, Permis.DIPLOMACIA));
        assertFalse(p.pot(Rang.OFICIAL, Permis.DIPLOMACIA));
        assertEquals(Resultat.FET, r.canviarPermis(cat, Permis.DIPLOMACIA, Rang.OFICIAL));
        assertTrue(p.pot(Rang.OFICIAL, Permis.DIPLOMACIA));
        assertEquals(Resultat.TIPUS_INVALID, r.canviarPermis(cat, Permis.DISSOLDRE, Rang.CIUTADA));
        assertFalse(p.pot(Rang.MINISTRE, Permis.DISSOLDRE));
        assertFalse(p.pot(null, Permis.EDITAR));

        assertEquals("Ministre/a", p.nomRang(Rang.MINISTRE));
        assertEquals(Resultat.FET, r.canviarNomRang(cat, Rang.MINISTRE, "Conseller/a"));
        assertEquals("Conseller/a", p.nomRang(Rang.MINISTRE));
        assertEquals(Resultat.FET, r.canviarNomRang(cat, Rang.MINISTRE, ""));
        assertEquals("Ministre/a", p.nomRang(Rang.MINISTRE));
    }

    @Test
    void cadaCanviApujaLaVersio() {
        long v = r.versio();
        r.canviarColor(cat, 0x123456);
        assertNotEquals(v, r.versio());
        long v2 = r.versio();
        assertTrue(r.caducar(T0).isEmpty());
        assertEquals(v2, r.versio()); // sense res a caducar, no hi ha canvis
    }

    @Test
    void elDiariTeUnLimit() {
        for (int i = 0; i < Registre.MAX_DIARI + 20; i++) {
            r.apuntar(T0 + i, "prova", "entrada " + i);
        }
        assertEquals(Registre.MAX_DIARI, r.diari().size());
        assertEquals("entrada " + (Registre.MAX_DIARI + 19), r.darrers(1).get(0).text());
    }
}
