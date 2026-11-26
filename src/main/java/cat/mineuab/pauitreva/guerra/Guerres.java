package cat.mineuab.pauitreva.guerra;

import cat.mineuab.pauitreva.Config;
import cat.mineuab.pauitreva.model.Capital;
import cat.mineuab.pauitreva.model.Chunk;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.ParametresGuerra;
import cat.mineuab.pauitreva.model.Registre;
import cat.mineuab.pauitreva.model.Resultat;
import cat.mineuab.pauitreva.model.Termes;
import cat.mineuab.pauitreva.model.Territori;
import cat.mineuab.pauitreva.model.TipusObjectiu;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import cat.mineuab.pauitreva.opac.ZonaOpac;
import cat.mineuab.pauitreva.ordres.Avisos;
import cat.mineuab.pauitreva.ordres.Missatges;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * Les operacions de guerra senceres: el {@link Registre} (les regles), OPAC (la zona en disputa i el traspàs
 * de chunks) i els avisos. Les fan servir les ordres, la pantalla i la batalla (la pau automàtica).
 */
public final class Guerres {

    /** Un error per explicar al jugador. */
    public static final class Error extends Exception {
        public Error(String missatge) {
            super(missatge);
        }
    }

    private Guerres() {
    }

    private static long ara() {
        return System.currentTimeMillis();
    }

    /**
     * Declara la guerra. Per a una conquesta, comprova a OPAC que l'objectiu és del defensor, d'una sola peça,
     * no massa gran i (si ho diu la config) tocant la frontera de l'atacant.
     */
    public static Guerra declarar(MinecraftServer server, Pais atacant, Pais defensor, TipusObjectiu objectiu,
                                  Set<Chunk> chunks) throws Error {
        if (objectiu == TipusObjectiu.REPARACIONS) {
            throw new Error("Les guerres per reparacions encara no es poden declarar: falta decidir com es paguen.");
        }
        Registre r = Dades.registre(server);
        IServerPartyAPI pa = PontOpac.partit(server, atacant.id());
        IServerPartyAPI pd = PontOpac.partit(server, defensor.id());
        if (pa == null || pd == null) {
            throw new Error("La party d'OPAC d'un dels dos països no hi és.");
        }
        Set<Chunk> terrA = ZonaOpac.territori(server, pa);
        Set<Chunk> terrD = ZonaOpac.territori(server, pd);
        ParametresGuerra p = Config.parametres();
        boolean conquesta = objectiu == TipusObjectiu.CONQUESTA;
        if (conquesta) {
            int max = p.maxChunksContra(terrD.size());
            Territori.Error err = Territori.validar(chunks, terrD, terrA, max, p.calFrontera());
            if (err != null) {
                throw new Error(Missatges.errorObjectiu(err, max));
            }
        } else {
            chunks = Set.of();
        }
        Resultat res = r.declararGuerra(atacant.id(), defensor.id(), objectiu, chunks, ara(), p,
                capitalDe(atacant, terrA), capitalDe(defensor, terrD));
        if (!res.bo()) {
            throw new Error(Missatges.resultat(res));
        }
        Guerra g = r.guerraEntre(atacant.id(), defensor.id()).orElseThrow();
        if (conquesta) {
            ZonaOpac.prepararZona(server, g, defensor);
        }
        Batalles.guerresCanviades(server);
        Avisos.anunciar(server, r.darrers(1).get(0).text(), Avisos.So.GUERRA,
                Component.literal("⚔ Guerra").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Missatges.pais(atacant).append(Component.literal(" contra ").withStyle(ChatFormatting.GRAY))
                        .append(Missatges.pais(defensor)),
                atacant.id(), defensor.id());
        long batalla = Config.finestres().properaObertura(g.inici());
        Avisos.xat(Avisos.jugadors(server, atacant.id(), defensor.id()), Missatges.gris(
                "La primera batalla serà el " + Missatges.data(batalla) + ". Mira-ho amb /guerra estat."));
        return g;
    }

    /** La capital del país si encara és seva; si no, la del mig del territori. */
    static Chunk capitalDe(Pais p, Set<Chunk> territori) {
        Capital cap = p.capital();
        if (cap != null) {
            Chunk c = new Chunk(cap.dimensio(), cap.chunkX(), cap.chunkZ());
            if (territori.contains(c)) {
                return c;
            }
        }
        return Territori.capitalPerDefecte(territori);
    }

    /** Signa la pau amb uns termes (proposada per un bàndol i imposada, o de l'admin). */
    public static Resultat signarPau(MinecraftServer server, Guerra g, UUID guanyador, Termes termes, boolean imposada) {
        ParametresGuerra p = Config.parametres();
        Resultat res = Dades.registre(server).ferPau(g.id(), guanyador, termes, imposada, ara(), p.treva(),
                p.humiliacio());
        if (res == Resultat.PAU_SIGNADA) {
            despresDeLaPau(server, g, termes);
        }
        return res;
    }

    /** {@code qui} accepta la pau que li han proposat (la que va veure, {@code creada}). */
    public static Resultat acceptarPau(MinecraftServer server, UUID qui, Guerra g, long creada) {
        Registre r = Dades.registre(server);
        Registre.PropostaPau pr = r.propostaPau(g.id()).orElse(null);
        if (pr == null) {
            return Resultat.NO_HI_HA_PROPOSTA;
        }
        ParametresGuerra p = Config.parametres();
        Resultat res = r.acceptarPau(qui, g.id(), creada, ara(), p.treva(), p.humiliacio());
        if (res == Resultat.PAU_SIGNADA) {
            despresDeLaPau(server, g, pr.termes());
        }
        return res;
    }

    public static Resultat rendir(MinecraftServer server, UUID qui, Guerra g) {
        Termes termes = Registre.termesRendicio(g, qui);
        ParametresGuerra p = Config.parametres();
        Resultat res = Dades.registre(server).rendir(qui, g.id(), ara(), p.treva(), p.humiliacio());
        if (res == Resultat.PAU_SIGNADA) {
            despresDeLaPau(server, g, termes);
        }
        return res;
    }

    /** La guerra ha arribat al límit de temps. */
    public static void pauAutomatica(MinecraftServer server, Guerra g) {
        Termes termes = Registre.termesAutomatics(g);
        Resultat res = Dades.registre(server).pauAutomatica(g.id(), ara(), Config.parametres());
        if (res == Resultat.PAU_SIGNADA) {
            despresDeLaPau(server, g, termes);
        }
    }

    /** Acaba una guerra sense guanyador (admin, o un país que es dissol). */
    public static Resultat acabarEnBlanc(MinecraftServer server, Guerra g) {
        return signarPau(server, g, g.atacant(), Termes.blanca(), false);
    }

    private static void despresDeLaPau(MinecraftServer server, Guerra g, Termes termes) {
        UUID nouAmo = null;
        if (!termes.chunks().isEmpty()) {
            IServerPartyAPI guanyador = PontOpac.partit(server, g.atacant());
            nouAmo = guanyador == null ? null : guanyador.getOwner().getUUID();
        }
        ZonaOpac.acabarZona(server, g, termes.chunks(), nouAmo);
        Batalles.guerresCanviades(server);
        Avisos.anunciar(server, Dades.registre(server).darrers(1).get(0).text(), Avisos.So.PAU,
                Component.literal("☮ Pau").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal(termes.esBlanca() ? "Pau blanca" : "S'ha signat un tractat").withStyle(ChatFormatting.YELLOW),
                g.atacant(), g.defensor());
    }
}
