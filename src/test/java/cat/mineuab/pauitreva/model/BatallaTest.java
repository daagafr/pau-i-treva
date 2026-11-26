package cat.mineuab.pauitreva.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La batalla: captura per presència, punts per minut, baixes, vides i pau automàtica. */
class BatallaTest {

    private static final long T0 = 1_764_000_000_000L;
    private static final String OW = "minecraft:overworld";
    private static final ParametresGuerra P = ParametresGuerra.perDefecte();

    private final UUID cat = UUID.randomUUID();
    private final UUID occ = UUID.randomUUID();
    private final Chunk a = new Chunk(OW, 1, 1);
    private final Chunk b = new Chunk(OW, 1, 2);
    private final Chunk capCat = new Chunk(OW, -10, 0);
    private final Chunk capOcc = new Chunk(OW, 10, 0);
    private Registre r;
    private Guerra g;

    @BeforeEach
    void prepara() {
        r = new Registre();
        r.fundar(cat, "Catalunya", "CAT", 0xFFCC00, T0);
        r.fundar(occ, "Occitània", "OCC", 0xCC0000, T0);
        Set<Chunk> objectiu = new LinkedHashSet<>(List.of(a, b));
        assertEquals(Resultat.DECLARADA, r.declararGuerra(cat, occ, TipusObjectiu.CONQUESTA, objectiu, T0, P, capCat, capOcc));
        g = r.guerraEntre(cat, occ).orElseThrow();
    }

    @Test
    void laGuerraTeLimitICapitals() {
        assertEquals(T0 + P.preparacio(), g.inici());
        assertEquals(g.inici() + P.durada(), g.limit());
        assertEquals(capCat, g.capitalAtacant());
        assertEquals(capOcc, g.capitalDefensor());
        assertEquals(occ, g.quiPren(capCat));
        assertEquals(cat, g.quiPren(capOcc));
        assertEquals(cat, g.quiPren(a));
    }

    @Test
    void unAtacantSolCapturaUnChunkEnUnsQuatreMinuts() {
        List<Registre.Presencia> hiSoc = List.of(new Registre.Presencia(a, 1, 0));
        int segons = 0;
        List<Registre.CanviControl> canvis = List.of();
        while (canvis.isEmpty() && segons < 1000) {
            canvis = r.capturar(g.id(), hiSoc, 1, P);
            segons++;
        }
        assertEquals(Guerra.CONTROL_MAX / P.capturaBase(), segons);
        assertEquals(1, canvis.size());
        assertTrue(canvis.get(0).capturat());
        assertEquals(cat, canvis.get(0).quiPren());
        assertEquals(List.of(a), g.controlats());
        assertEquals(0, g.control(b));
    }

    @Test
    void contraElsQueVanEnMassaHiHaUnMaxim() {
        assertEquals(P.capturaMax(), P.deltaCaptura(50, 0, false));
        assertEquals(P.capturaBase() + P.capturaExtra(), P.deltaCaptura(2, 0, false));
        // Defensors presents fan recular, encara que no hi hagi atacants.
        assertEquals(-2 * P.pesDefensor(), P.deltaCaptura(0, 2, false));
        assertEquals(P.capturaBase() * P.factorCapital() / 100, P.deltaCaptura(1, 0, true));
    }

    @Test
    void unChunkAMigCapturarDecauSiNoHiHaNingu_iUnDeCapturatNo() {
        r.capturar(g.id(), List.of(new Registre.Presencia(a, 1, 0)), 100, P);
        int mig = g.control(a);
        assertTrue(mig > 0 && mig < Guerra.CONTROL_MAX);
        r.capturar(g.id(), List.of(), 10, P);
        assertEquals(mig - 10 * P.decadencia(), g.control(a));

        r.capturar(g.id(), List.of(new Registre.Presencia(a, 3, 0)), 1000, P);
        assertEquals(Guerra.CONTROL_MAX, g.control(a));
        r.capturar(g.id(), List.of(), 1000, P);
        assertEquals(Guerra.CONTROL_MAX, g.control(a));

        // Els defensors el recuperen, i se sap que l'atacant l'ha perdut.
        List<Registre.CanviControl> canvis = r.capturar(g.id(), List.of(new Registre.Presencia(a, 0, 1)), 1, P);
        assertEquals(1, canvis.size());
        assertFalse(canvis.get(0).capturat());
    }

    @Test
    void elDefensorPotPrendreLaCapitalDeLAtacant() {
        r.capturar(g.id(), List.of(new Registre.Presencia(capCat, 0, 2)), 1000, P);
        assertEquals(Guerra.CONTROL_MAX, g.controlCapitalAtacant());
        assertEquals(0, g.controlCapitalDefensor());
        r.puntuarMinut(g.id(), P);
        // El defensor suma per la capital i per la defensa (l'atacant no controla res).
        int esperat = Registre.perMinut(P.capitalPerHora()) + Registre.perMinut(P.defensaPerHora());
        assertEquals(-esperat, g.milpunts());
    }

    @Test
    void ocupacioDonaPuntsCadaMinut() {
        r.capturar(g.id(), List.of(new Registre.Presencia(a, 1, 0), new Registre.Presencia(b, 1, 0)), 1000, P);
        for (int i = 0; i < 60; i++) {
            r.puntuarMinut(g.id(), P);
        }
        // Una hora amb tot l'objectiu: els punts d'ocupació per hora.
        assertEquals(P.ocupacioPerHora(), g.punts());
    }

    @Test
    void lesBaixesDonenPuntsFinsAUnMaximPerVictima() {
        UUID jugador = UUID.randomUUID();
        for (int i = 0; i < P.baixesMaxVictima(); i++) {
            assertTrue(r.baixa(g.id(), occ, jugador, P));
        }
        assertFalse(r.baixa(g.id(), occ, jugador, P));
        assertEquals(-P.baixesMaxVictima() * P.puntsBaixa(), g.punts());
        assertEquals(P.baixesMaxVictima() + 1, g.baixesDefensor());
        r.novaFinestra();
        assertTrue(r.baixa(g.id(), occ, jugador, P));
    }

    @Test
    void quiGastaLesVidesJaNoCaptura() {
        UUID jugador = UUID.randomUUID();
        for (int i = 0; i < P.vides() - 1; i++) {
            r.mort(g.id(), jugador);
        }
        assertTrue(r.potCapturar(g.id(), jugador, P));
        r.mort(g.id(), jugador);
        assertFalse(r.potCapturar(g.id(), jugador, P));
        r.novaFinestra();
        assertTrue(r.potCapturar(g.id(), jugador, P));
    }

    @Test
    void termesPerParaula() {
        r.capturar(g.id(), List.of(new Registre.Presencia(a, 1, 0)), 1000, P);
        assertEquals(Set.of(a, b), Registre.termes(g, cat, Registre.QuinsChunks.OBJECTIU, false).chunks());
        assertEquals(Set.of(a), Registre.termes(g, cat, Registre.QuinsChunks.CONTROLATS, false).chunks());
        // El defensor no pot guanyar chunks.
        assertTrue(Registre.termes(g, occ, Registre.QuinsChunks.OBJECTIU, true).chunks().isEmpty());
    }

    @Test
    void pauAutomaticaAmbElQueEsPotPagar() {
        r.capturar(g.id(), List.of(new Registre.Presencia(a, 1, 0), new Registre.Presencia(b, 1, 0)), 1000, P);
        r.sumarPunts(g.id(), cat, 60);
        // Cada chunk val 50: amb 60 punts, només un.
        Termes t = Registre.termesAutomatics(g);
        assertEquals(1, t.chunks().size());
        assertFalse(t.humiliacio());

        assertEquals(1, r.guerresAlLimit(g.limit()).size());
        assertEquals(Resultat.PAU_SIGNADA, r.pauAutomatica(g.id(), g.limit(), P));
        assertTrue(r.guerres().isEmpty());
        assertEquals(TipusRelacio.TREVA, r.relacio(cat, occ));
        assertTrue(r.darrers(1).get(0).text().contains("cedeix 1 chunk"));
        assertEquals(Esdeveniment.LIMIT, r.darrers(2).get(1).tipus());
    }

    @Test
    void pauAutomaticaAFavorDelDefensorHumiliaLAtacant() {
        r.sumarPunts(g.id(), occ, 55);
        Termes t = Registre.termesAutomatics(g);
        assertTrue(t.chunks().isEmpty());
        assertTrue(t.humiliacio());
        r.pauAutomatica(g.id(), g.limit(), P);
        assertTrue(r.pais(cat).orElseThrow().humiliat(g.limit()));
        assertFalse(r.pais(cat).orElseThrow().humiliat(g.limit() + P.humiliacio()));
        assertFalse(r.pais(occ).orElseThrow().humiliat(g.limit()));
    }

    @Test
    void pauAutomaticaSensePuntsEsBlanca() {
        assertTrue(Registre.termesAutomatics(g).esBlanca());
        r.sumarPunts(g.id(), occ, 10);
        assertTrue(Registre.termesAutomatics(g).esBlanca());
    }

    @Test
    void maximDeGuerresPerPais() {
        UUID ara = UUID.randomUUID();
        UUID tres = UUID.randomUUID();
        r.fundar(ara, "Aragó", "ARA", 0xFFFF00, T0);
        r.fundar(tres, "Tercer", "TRE", 0x00FF00, T0);
        assertEquals(Resultat.DECLARADA, r.declararGuerra(ara, occ, TipusObjectiu.HUMILIACIO, Set.of(), T0, P, null, null));
        // Occitània ja en té dues (el màxim per defecte).
        assertEquals(Resultat.MASSA_GUERRES, r.declararGuerra(tres, occ, TipusObjectiu.HUMILIACIO, Set.of(), T0, P, null, null));
    }

    @Test
    void retirarLaPau() {
        r.proposarPau(cat, g.id(), cat, Termes.blanca(), T0);
        assertEquals(Resultat.FET, r.retirarPau(occ, g.id()));
        assertTrue(r.propostaPau(g.id()).isEmpty());
        assertEquals(Resultat.NO_HI_HA_PROPOSTA, r.retirarPau(occ, g.id()));
    }
}
