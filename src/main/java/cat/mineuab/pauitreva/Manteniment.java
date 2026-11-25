package cat.mineuab.pauitreva;

import cat.mineuab.pauitreva.model.Esdeveniment;
import cat.mineuab.pauitreva.model.EstatRelacio;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.Parella;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.TipusRelacio;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import cat.mineuab.pauitreva.ordres.Missatges;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

/**
 * Feina periòdica (cada 30 s per defecte): fa caducar propostes, pactes i treves, dissol els països que ja
 * no tenen party, torna a posar el nom i el color de cada país a OPAC i comprova que els aliats d'OPAC
 * quadrin amb les aliances.
 */
public final class Manteniment {

    private static boolean engegat;
    private static long ticks;

    private Manteniment() {
    }

    public static void engegar(MinecraftServer server) {
        engegat = true;
        ticks = 0;
        fer(server);
    }

    public static void aturar() {
        engegat = false;
    }

    public static void tick(MinecraftServer server) {
        if (!engegat) {
            return;
        }
        int segons;
        try {
            segons = Config.SEGONS_MANTENIMENT.get();
        } catch (IllegalStateException e) {
            segons = 30;
        }
        if (++ticks % (segons * 20L) == 0) {
            fer(server);
        }
    }

    public static void fer(MinecraftServer server) {
        try {
            Registre r = Dades.registre(server);
            long ara = System.currentTimeMillis();

            for (Esdeveniment e : r.caducar(ara)) {
                Missatges.anunciar(server, e.text());
            }

            for (Pais p : List.copyOf(r.paisos())) {
                if (PontOpac.partit(server, p.id()) == null) {
                    r.dissoldre(p.id(), "la seva party d'OPAC ja no existeix", ara);
                    Missatges.anunciar(server, r.darrers(1).get(0).text());
                    continue;
                }
                PontOpac.aplicarIdentitat(server, p);
            }

            for (Map.Entry<Parella, EstatRelacio> e : Map.copyOf(r.totesLesRelacions()).entrySet()) {
                if (e.getValue().tipus() == TipusRelacio.ALIANCA) {
                    PontOpac.aliar(server, e.getKey().a(), e.getKey().b());
                }
            }
        } catch (RuntimeException e) {
            // Mai no ha de tombar el servidor: s'avisa al registre i es torna a provar la propera vegada.
            PauITreva.LOG.error("Pau i Treva: error al manteniment", e);
        }
    }
}
