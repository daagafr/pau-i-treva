package cat.mineuab.pauitreva;

import cat.mineuab.pauitreva.guerra.Guerres;
import cat.mineuab.pauitreva.model.Esdeveniment;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.EstatRelacio;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.Parella;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.TipusRelacio;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import cat.mineuab.pauitreva.ordres.Missatges;
import cat.mineuab.pauitreva.web.ApiWeb;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * Feina periòdica (cada 30 s per defecte): fa caducar propostes, pactes i treves, dissol els països que ja
 * no tenen party, torna a posar el nom i el color de cada país a OPAC i comprova que els aliats d'OPAC
 * quadrin amb les aliances.
 */
public final class Manteniment {

    /**
     * Quantes vegades seguides ha de faltar la party d'un país abans de dissoldre'l. Així, si en arrencar OPAC
     * encara no té les parties a punt, no es perd cap país.
     */
    private static final int ABSENCIES_PER_DISSOLDRE = 3;

    private static boolean engegat;
    private static long ticks;
    private static final Map<UUID, Integer> absencies = new HashMap<>();

    private Manteniment() {
    }

    /** La primera passada es fa al cap d'un interval, no en arrencar. */
    public static void engegar(MinecraftServer server) {
        engegat = true;
        ticks = 0;
        absencies.clear();
        boolean api;
        int port;
        try {
            api = Config.API_ACTIVA.get();
            port = Config.API_PORT.get();
        } catch (IllegalStateException e) {
            api = true;
            port = 25582;
        }
        if (api) {
            ApiWeb.engegar(port);
            ApiWeb.actualitzar(server);
        }
    }

    public static void aturar() {
        engegat = false;
        ApiWeb.aturar();
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
                    int vegades = absencies.merge(p.id(), 1, Integer::sum);
                    PauITreva.LOG.warn("Pau i Treva: la party de {} no hi és ({} de {})", Registre.etiqueta(p),
                            vegades, ABSENCIES_PER_DISSOLDRE);
                    if (vegades >= ABSENCIES_PER_DISSOLDRE) {
                        absencies.remove(p.id());
                        // Abans, les guerres s'acaben en blanc perquè la zona en disputa torni a OPAC com era.
                        for (Guerra g : r.guerresDe(p.id())) {
                            Guerres.acabarEnBlanc(server, g);
                        }
                        r.dissoldre(p.id(), "la seva party d'OPAC ja no existeix", ara);
                        Missatges.anunciar(server, r.darrers(1).get(0).text());
                    }
                    continue;
                }
                absencies.remove(p.id());
                PontOpac.aplicarIdentitat(server, p);
            }

            for (Map.Entry<Parella, EstatRelacio> e : Map.copyOf(r.totesLesRelacions()).entrySet()) {
                if (e.getValue().tipus() == TipusRelacio.ALIANCA) {
                    PontOpac.aliar(server, e.getKey().a(), e.getKey().b());
                }
            }

            ApiWeb.actualitzar(server);
        } catch (RuntimeException e) {
            // Mai no ha de tombar el servidor: s'avisa al registre i es torna a provar la propera vegada.
            PauITreva.LOG.error("Pau i Treva: error al manteniment", e);
        }
    }
}
