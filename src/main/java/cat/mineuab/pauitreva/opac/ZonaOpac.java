package cat.mineuab.pauitreva.opac;

import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Chunk;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.Validacio;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;

/**
 * La part d'OPAC de la guerra: el territori com a chunks del model i la zona en disputa.
 *
 * <p><b>Zona en disputa.</b> En declarar una conquesta, cada chunk de l'objectiu passa a una sub-config
 * de guerra del seu amo ({@link #sub}) (el mateix amo, sense tocar els chunks forçats), i es recorda la sub-config que
 * tenia. La sub-config continua protegida: qui hi pot trencar, posar i obrir coses durant la finestra ho decideix
 * {@link AccesGuerra} (només els dos països en guerra). El que sí que canvia durant la finestra són les explosions
 * ({@code blocksByExplosions} i {@code entitiesByExplosions}), que no es poden filtrar per qui les fa. La primera
 * opció només la pot canviar un mod si és a {@code opConfigurablePlayerConfigOptions} d'OPAC. En signar la pau, els
 * chunks cedits passen al líder del guanyador i els altres tornen a la seva sub-config.
 *
 * <p>El PvP de la guerra no passa per aquí: el fa {@code ProteccioOpacMixin}.
 */
public final class ZonaOpac {

    /**
     * Prefix de les sub-configs de guerra (OPAC: lletres, xifres, - i _, fins a 16). N'hi ha una per guerra
     * oberta ({@link Guerra#zona()}), perquè cada guerra obre i tanca la seva zona sense tocar la d'una altra.
     */
    public static final String PREFIX = "pit-guerra-";

    /** La sub-config de la zona d'aquesta guerra. */
    public static String sub(Guerra g) {
        return PREFIX + g.zona();
    }
    private static final String MAIN = "main";

    /** Els canvis que OPAC ha rebutjat, perquè l'avís surti un sol cop i no cada 3 s. */
    private static final Set<String> REBUTJATS = new HashSet<>();
    private static boolean explosionsBloquejades;

    private ZonaOpac() {
    }

    // ---------------------------------------------------------------- Territori

    /** Tot el territori d'un país: els chunks reclamats pels membres (amb claims de party, els del líder). */
    public static Set<Chunk> territori(MinecraftServer server, IServerPartyAPI partit) {
        Set<Chunk> r = new LinkedHashSet<>();
        PontOpac.territori(server, partit).forEach((dim, llista) -> {
            for (int[] c : llista) {
                r.add(new Chunk(dim, c[0], c[1]));
            }
        });
        return r;
    }

    public static IPlayerChunkClaimAPI claim(MinecraftServer server, Chunk c) {
        ResourceLocation dim = ResourceLocation.tryParse(c.dimensio());
        return dim == null ? null : PontOpac.api(server).getServerClaimsManager().get(dim, c.x(), c.z());
    }

    /** El país (id de party) que té aquest chunk, o {@code null}. */
    public static UUID paisDelChunk(MinecraftServer server, Chunk c) {
        IPlayerChunkClaimAPI claim = claim(server, c);
        if (claim == null) {
            return null;
        }
        IServerPartyAPI partit = PontOpac.partitDe(server, claim.getPlayerId());
        return partit == null ? null : partit.getId();
    }

    // ---------------------------------------------------------------- Zona en disputa

    /**
     * Passa els chunks de l'objectiu a la sub-config de guerra del seu amo, protegits (fins que s'obri la
     * finestra), i apunta a la guerra la sub-config que tenien.
     */
    public static void prepararZona(MinecraftServer server, Guerra g, Pais defensor) {
        IServerClaimsManagerAPI claims = PontOpac.api(server).getServerClaimsManager();
        for (Chunk c : g.chunks()) {
            IPlayerChunkClaimAPI claim = claim(server, c);
            if (claim == null) {
                PauITreva.LOG.warn("Pau i Treva: el chunk {} de l'objectiu no és de ningú", c);
                continue;
            }
            IPlayerConfigAPI config = config(server, claim.getPlayerId());
            if (config == null) {
                continue;
            }
            IPlayerConfigAPI actual = config.getEffectiveSubConfig(claim.getSubConfigIndex());
            String abans = actual.getSubId() == null ? MAIN : actual.getSubId();
            if (abans.equals(sub(g))) {
                continue; // ja hi és (una càrrega repetida, o un canvi de líder)
            }
            IPlayerConfigAPI sub = subDeGuerra(config, sub(g), defensor);
            if (sub == null) {
                continue;
            }
            g.recordarSubconfig(c, abans);
            claims.claim(ResourceLocation.parse(c.dimensio()), claim.getPlayerId(), sub.getSubIndex(), c.x(), c.z(),
                    claim.isForceloadable());
        }
        aplicarZona(server, g, false);
    }

    /**
     * Obre ({@code oberta}) o tanca la zona a les explosions, a la sub-config de guerra dels amos dels chunks de
     * l'objectiu, i la deixa protegida (l'accés dels bel·ligerants el dona {@link AccesGuerra}). També torna a posar
     * a la sub-config els chunks que algú n'hagi tret.
     */
    public static void aplicarZona(MinecraftServer server, Guerra g, boolean oberta) {
        IServerClaimsManagerAPI claims = PontOpac.api(server).getServerClaimsManager();
        Set<UUID> amos = new HashSet<>();
        for (Chunk c : g.chunks()) {
            IPlayerChunkClaimAPI claim = claim(server, c);
            if (claim == null) {
                continue;
            }
            IPlayerConfigAPI config = config(server, claim.getPlayerId());
            if (config == null || !config.subConfigExists(sub(g))) {
                continue;
            }
            IPlayerConfigAPI sub = config.getSubConfig(sub(g));
            if (claim.getSubConfigIndex() != sub.getSubIndex()) {
                claims.claim(ResourceLocation.parse(c.dimensio()), claim.getPlayerId(), sub.getSubIndex(), c.x(), c.z(),
                        claim.isForceloadable());
            }
            if (amos.add(claim.getPlayerId())) {
                posar(sub, PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);
                posar(sub, PlayerConfigOptions.CLAIM_EXCEPTION_BLOCKS_BY_EXPLOSIONS,
                        oberta || perDefecte(server, PlayerConfigOptions.CLAIM_EXCEPTION_BLOCKS_BY_EXPLOSIONS));
                posar(sub, PlayerConfigOptions.CLAIM_EXCEPTION_ENTITIES_BY_EXPLOSIONS,
                        oberta || perDefecte(server, PlayerConfigOptions.CLAIM_EXCEPTION_ENTITIES_BY_EXPLOSIONS));
            }
        }
    }

    /**
     * Acaba la zona en signar la pau: els chunks {@code cedits} passen a {@code nouAmo} (el líder del
     * guanyador, a la seva config principal) i els altres tornen a la sub-config que tenien.
     */
    public static void acabarZona(MinecraftServer server, Guerra g, Set<Chunk> cedits, UUID nouAmo) {
        IServerClaimsManagerAPI claims = PontOpac.api(server).getServerClaimsManager();
        Set<UUID> amos = new HashSet<>();
        for (Chunk c : g.chunks()) {
            IPlayerChunkClaimAPI claim = claim(server, c);
            if (claim == null) {
                continue;
            }
            ResourceLocation dim = ResourceLocation.parse(c.dimensio());
            amos.add(claim.getPlayerId());
            if (cedits.contains(c) && nouAmo != null) {
                claims.claim(dim, nouAmo, -1, c.x(), c.z(), claim.isForceloadable());
                continue;
            }
            IPlayerConfigAPI config = config(server, claim.getPlayerId());
            if (config == null) {
                continue;
            }
            String abans = g.subconfigs().getOrDefault(c, MAIN);
            int index = MAIN.equals(abans) || !config.subConfigExists(abans) ? -1 : config.getSubConfig(abans).getSubIndex();
            if (claim.getSubConfigIndex() != index) {
                claims.claim(dim, claim.getPlayerId(), index, c.x(), c.z(), claim.isForceloadable());
            }
        }
        // La sub-config es queda (protegida) per a la propera guerra que tingui aquest número.
        for (UUID amo : amos) {
            IPlayerConfigAPI config = config(server, amo);
            if (config != null && config.subConfigExists(sub(g))) {
                IPlayerConfigAPI sub = config.getSubConfig(sub(g));
                posar(sub, PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);
                posar(sub, PlayerConfigOptions.CLAIM_EXCEPTION_BLOCKS_BY_EXPLOSIONS,
                        perDefecte(server, PlayerConfigOptions.CLAIM_EXCEPTION_BLOCKS_BY_EXPLOSIONS));
                posar(sub, PlayerConfigOptions.CLAIM_EXCEPTION_ENTITIES_BY_EXPLOSIONS,
                        perDefecte(server, PlayerConfigOptions.CLAIM_EXCEPTION_ENTITIES_BY_EXPLOSIONS));
            }
        }
    }

    private static IPlayerConfigAPI subDeGuerra(IPlayerConfigAPI config, String id, Pais defensor) {
        IPlayerConfigAPI sub = config.subConfigExists(id) ? config.getSubConfig(id) : config.createSubConfig(id);
        if (sub == null) {
            PauITreva.LOG.warn("Pau i Treva: no s'ha pogut crear la sub-config {} de {} (límit de sub-configs?)", id,
                    config.getPlayerId());
            return null;
        }
        // Al mapa, la zona en disputa surt amb el nom del país i el color barrejat amb vermell.
        posar(sub, PlayerConfigOptions.CLAIMS_NAME, Validacio.nomPerOpac(defensor.nom() + " (en disputa)"));
        posar(sub, PlayerConfigOptions.CLAIMS_COLOR, barreja(defensor.color(), 0xFF0000));
        return sub;
    }

    static int barreja(int a, int b) {
        int r = (((a >> 16) & 0xFF) + ((b >> 16) & 0xFF)) / 2;
        int g = (((a >> 8) & 0xFF) + ((b >> 8) & 0xFF)) / 2;
        int bl = ((a & 0xFF) + (b & 0xFF)) / 2;
        int c = (r << 16) | (g << 8) | bl;
        return c == 0 ? 0x010101 : c;
    }

    // ---------------------------------------------------------------- Ajudes

    /** Si OPAC no ha deixat obrir la zona a les explosions (cal tocar la config del servidor d'OPAC). */
    public static boolean explosionsBloquejades() {
        return explosionsBloquejades;
    }

    /** El valor d'una opció a la config per defecte dels jugadors del servidor (no el de fàbrica d'OPAC). */
    private static boolean perDefecte(MinecraftServer server, xaero.pac.common.server.player.config.api.v2.IPlayerConfigOptionSpecAPI<Boolean> opcio) {
        IPlayerConfigAPI d = PontOpac.api(server).getPlayerConfigManager().getDefaultConfig();
        return d != null && Boolean.TRUE.equals(d.getEffective(opcio));
    }

    private static IPlayerConfigAPI config(MinecraftServer server, UUID jugador) {
        return PontOpac.api(server).getPlayerConfigManager().getLoadedConfig(jugador);
    }

    private static <T> void posar(IPlayerConfigAPI config, xaero.pac.common.server.player.config.api.v2.IPlayerConfigOptionSpecAPI<T> opcio,
                                  T valor) {
        if (java.util.Objects.equals(config.getRaw(opcio), valor)) {
            return;
        }
        String clau = config.getPlayerId() + "/" + config.getSubId() + " " + opcio.getId() + " " + valor;
        if (REBUTJATS.contains(clau)) {
            return;
        }
        IPlayerConfigAPI.SetResult r = config.tryToSet(opcio, valor);
        if (r != IPlayerConfigAPI.SetResult.SUCCESS) {
            REBUTJATS.add(clau);
            if (opcio == PlayerConfigOptions.CLAIM_EXCEPTION_BLOCKS_BY_EXPLOSIONS) {
                explosionsBloquejades = true;
            }
            PauITreva.LOG.warn("Pau i Treva: OPAC no ha deixat posar {} = {} a {}/{}: {}{}", opcio.getId(), valor,
                    config.getPlayerId(), config.getSubId(), r, opcio == PlayerConfigOptions.CLAIM_EXCEPTION_BLOCKS_BY_EXPLOSIONS
                            ? ". Afegeix \"claims.protection.exceptions.blocksByExplosions\" a opConfigurablePlayerConfigOptions"
                            + " a openpartiesandclaims-server.toml" : "");
        }
    }

    /** Els amos dels claims del territori d'un país, amb quants chunks té cadascun (per a la migració). */
    public static Map<UUID, Integer> amos(MinecraftServer server, IServerPartyAPI partit) {
        Map<UUID, Integer> r = new HashMap<>();
        IServerClaimsManagerAPI claims = PontOpac.api(server).getServerClaimsManager();
        partit.getMemberInfoStream().forEach(m -> {
            if (claims.hasPlayerInfo(m.getUUID())) {
                IServerPlayerClaimInfoAPI info = claims.getPlayerInfo(m.getUUID());
                if (info.getClaimCount() > 0) {
                    r.put(m.getUUID(), info.getClaimCount());
                }
            }
        });
        return r;
    }
}
