package cat.mineuab.pauitreva;

import cat.mineuab.pauitreva.guerra.Batalles;
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
import cat.mineuab.pauitreva.opac.ZonaOpac;
import cat.mineuab.pauitreva.titols.Tags;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import cat.mineuab.pauitreva.ordres.Missatges;
import cat.mineuab.pauitreva.web.ApiWeb;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import cat.mineuab.pauitreva.model.Chunk;
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
    /**
     * Per a cada país, el líder i el seu territori a l'última mirada (cada 5 s). Si el líder canvia, només
     * es passen al nou els chunks d'aquesta foto: així, si el vell ja té una altra party, els seus claims nous
     * no s'hi barregen.
     */
    private static final Map<UUID, UUID> liderVist = new HashMap<>();
    private static final Map<UUID, Set<Chunk>> territoriVist = new HashMap<>();

    private Manteniment() {
    }

    /** La primera passada es fa al cap d'un interval, no en arrencar. */
    public static void engegar(MinecraftServer server) {
        engegat = true;
        ticks = 0;
        absencies.clear();
        liderVist.clear();
        territoriVist.clear();
        boolean api;
        int port;
        try {
            api = Config.API_ACTIVA.get();
            port = Config.API_PORT.get();
        } catch (IllegalStateException e) {
            api = true;
            port = 25582;
        }
        tancarPvpVell(server);
        if (api) {
            ApiWeb.engegar(port);
            ApiWeb.actualitzar(server);
        }
    }

    /** Els jugadors a qui la 0.2.0 va obrir el PvP (playersByPlayers): se'ls torna a posar el de per defecte. */
    private static void tancarPvpVell(MinecraftServer server) {
        Dades dades = Dades.dades(server);
        if (dades.pvpPerTancar().isEmpty()) {
            return;
        }
        var configs = PontOpac.api(server).getPlayerConfigManager();
        var perDefecte = configs.getDefaultConfig();
        boolean valor = perDefecte != null && Boolean.TRUE.equals(perDefecte.getEffective(
                xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions.CLAIM_EXCEPTION_PLAYERS_BY_PLAYERS));
        for (UUID id : dades.pvpPerTancar()) {
            var config = configs.getLoadedConfig(id);
            if (config != null) {
                config.tryToSet(xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions.CLAIM_EXCEPTION_PLAYERS_BY_PLAYERS, valor);
            }
        }
        PauITreva.LOG.info("Pau i Treva: s'ha tornat a tancar el PvP que la 0.2.0 havia obert a {} jugadors",
                dades.pvpPerTancar().size());
        dades.pvpPerTancar().clear();
        dades.setDirty();
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
        } else if (ticks % 100 == 0) {
            mirarLiders(server);
        }
    }

    /**
     * Si la party ha canviat de propietari (OPAC no toca els claims quan passa): treu el nom i el color del país
     * de la config del líder vell i, amb claims de party, li passa tots els chunks al nou, perquè el territori
     * continuï sent del país. Els chunks en disputa tornen a la zona de guerra del nou amo.
     */
    /** Cada 5 s: els canvis de líder, i la foto del territori de cada líder. */
    public static void mirarLiders(MinecraftServer server) {
        try {
            Registre r = Dades.registre(server);
            long ara = System.currentTimeMillis();
            for (Pais p : List.copyOf(r.paisos())) {
                if (PontOpac.partit(server, p.id()) != null) {
                    canviDeLider(server, r, p, ara);
                }
            }
        } catch (RuntimeException e) {
            PauITreva.LOG.error("Pau i Treva: error mirant els líders", e);
        }
    }

    private static void canviDeLider(MinecraftServer server, Registre r, Pais p, long ara) {
        IServerPartyAPI partit = PontOpac.partit(server, p.id());
        UUID nou = partit.getOwner().getUUID();
        UUID vell = p.lider();
        if (vell != null && !vell.equals(nou)) {
            PontOpac.treureIdentitat(server, vell, p);
            // Només el territori que tenia el vell quan era el líder d'aquest país. Sense foto (just després
            // d'arrencar), tot, però només si el vell no és a cap altra party.
            Set<Chunk> foto = vell.equals(liderVist.get(p.id())) ? territoriVist.get(p.id()) : null;
            IServerPartyAPI deVell = PontOpac.partitDe(server, vell);
            boolean enUnaAltra = deVell != null && !deVell.getId().equals(p.id());
            if (PontOpac.claimsDePartit() && (foto != null || !enUnaAltra)) {
                int[] n = PontOpac.traspassarClaims(server, vell, nou, false, foto);
                PauITreva.LOG.info("Pau i Treva: {} canvia de líder; {} chunks ({} forçats) passen al nou",
                        Registre.etiqueta(p), n[0], n[1]);
                for (Guerra g : r.guerresDe(p.id())) {
                    if (g.defensor().equals(p.id())) {
                        ZonaOpac.prepararZona(server, g, p);
                    }
                }
                Batalles.guerresCanviades(server);
            }
            r.apuntar(ara, Esdeveniment.LIDER, Registre.etiqueta(p) + " té un nou líder: "
                    + partit.getOwner().getUsername() + ".");
            Missatges.anunciar(server, r.darrers(1).get(0).text());
        }
        r.posarLider(p.id(), nou);
        liderVist.put(p.id(), nou);
        territoriVist.put(p.id(), PontOpac.chunksDe(server, nou));
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
                canviDeLider(server, r, p, ara);
                PontOpac.aplicarIdentitat(server, p);
            }

            for (Map.Entry<Parella, EstatRelacio> e : Map.copyOf(r.totesLesRelacions()).entrySet()) {
                if (e.getValue().tipus() == TipusRelacio.ALIANCA) {
                    PontOpac.aliar(server, e.getKey().a(), e.getKey().b());
                }
            }

            Tags.actualitzar(server);
            ApiWeb.actualitzar(server);
        } catch (RuntimeException e) {
            // Mai no ha de tombar el servidor: s'avisa al registre i es torna a provar la propera vegada.
            PauITreva.LOG.error("Pau i Treva: error al manteniment", e);
        }
    }
}
