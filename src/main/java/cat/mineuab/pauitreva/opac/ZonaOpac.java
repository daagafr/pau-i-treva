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
 * La part d'OPAC de la guerra: el territori com a chunks del model, la zona en disputa i el PvP.
 *
 * <p><b>Zona en disputa.</b> En declarar una conquesta, cada chunk de l'objectiu passa a una sub-config
 * {@value #SUB} del seu amo (el mateix amo, sense tocar els chunks forçats), i es recorda la sub-config que
 * tenia. Mentre la finestra és oberta, aquesta sub-config té {@code protectClaimedChunks = false}: s'hi pot
 * trencar, posar i explotar. Fora de la finestra torna a protegir. En signar la pau, els chunks cedits passen al
 * líder del guanyador i els altres tornen a la seva sub-config.
 *
 * <p><b>PvP.</b> L'opció {@code playersByPlayers} no la poden canviar els jugadors (ni, per defecte, els mods):
 * cal que sigui a {@code opConfigurablePlayerConfigOptions} de la config del servidor d'OPAC. Si hi és, durant la
 * finestra es posa a {@code true} a la config de tots els amos de claims dels països en guerra.
 */
public final class ZonaOpac {

    /** Id de la sub-config de guerra (OPAC: lletres, xifres, - i _, fins a 16). */
    public static final String SUB = "pit-guerra";
    private static final String MAIN = "main";

    private static boolean pvpBloquejat;

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
            if (SUB.equals(abans)) {
                continue; // ja hi és (una altra guerra o una càrrega repetida)
            }
            IPlayerConfigAPI sub = subDeGuerra(config, defensor);
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
     * Obre ({@code oberta}) o tanca la zona: {@code protectClaimedChunks} de la sub-config de guerra dels amos
     * dels chunks de l'objectiu. També torna a posar a la sub-config els chunks que algú n'hagi tret.
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
            if (config == null || !config.subConfigExists(SUB)) {
                continue;
            }
            IPlayerConfigAPI sub = config.getSubConfig(SUB);
            if (claim.getSubConfigIndex() != sub.getSubIndex()) {
                claims.claim(ResourceLocation.parse(c.dimensio()), claim.getPlayerId(), sub.getSubIndex(), c.x(), c.z(),
                        claim.isForceloadable());
            }
            if (amos.add(claim.getPlayerId())) {
                posar(sub, PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, !oberta);
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
        // La sub-config es queda (protegida) per a la propera guerra.
        for (UUID amo : amos) {
            IPlayerConfigAPI config = config(server, amo);
            if (config != null && config.subConfigExists(SUB)) {
                posar(config.getSubConfig(SUB), PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);
            }
        }
    }

    private static IPlayerConfigAPI subDeGuerra(IPlayerConfigAPI config, Pais defensor) {
        IPlayerConfigAPI sub = config.subConfigExists(SUB) ? config.getSubConfig(SUB) : config.createSubConfig(SUB);
        if (sub == null) {
            PauITreva.LOG.warn("Pau i Treva: no s'ha pogut crear la sub-config {} de {} (límit de sub-configs?)", SUB,
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

    // ---------------------------------------------------------------- PvP

    /**
     * Obre el PvP a la config de tots els membres dels països de {@code oberts} i el torna a tancar a les
     * configs on l'havíem obert i que ja no hi han de ser.
     *
     * @param obertPerNosaltres les configs on l'ha obert el mod (es desa al món, perquè si el servidor cau amb
     *                          la finestra oberta, en tornar s'han de tancar)
     * @return si la llista {@code obertPerNosaltres} ha canviat
     */
    public static boolean aplicarPvp(MinecraftServer server, Set<UUID> oberts, Set<UUID> obertPerNosaltres) {
        boolean canviat = false;
        Set<UUID> membres = new HashSet<>();
        for (UUID pais : oberts) {
            IServerPartyAPI partit = PontOpac.partit(server, pais);
            if (partit != null) {
                partit.getMemberInfoStream().forEach(m -> membres.add(m.getUUID()));
            }
        }
        for (UUID membre : membres) {
            IPlayerConfigAPI config = config(server, membre);
            if (config == null || Boolean.TRUE.equals(config.getRaw(PlayerConfigOptions.CLAIM_EXCEPTION_PLAYERS_BY_PLAYERS))) {
                continue; // ja és obert (per nosaltres o per un OP)
            }
            IPlayerConfigAPI.SetResult r = config.tryToSet(PlayerConfigOptions.CLAIM_EXCEPTION_PLAYERS_BY_PLAYERS, true);
            if (r == IPlayerConfigAPI.SetResult.SUCCESS) {
                canviat |= obertPerNosaltres.add(membre);
            } else if (!pvpBloquejat) {
                pvpBloquejat = true;
                PauITreva.LOG.warn("Pau i Treva: OPAC no deixa obrir el PvP de la guerra ({}). Afegeix "
                        + "\"claims.protection.exceptions.playersByPlayers\" a opConfigurablePlayerConfigOptions "
                        + "a openpartiesandclaims-server.toml", r);
            }
        }
        for (UUID membre : Set.copyOf(obertPerNosaltres)) {
            if (membres.contains(membre)) {
                continue;
            }
            IPlayerConfigAPI config = config(server, membre);
            if (config != null) {
                config.tryToReset(PlayerConfigOptions.CLAIM_EXCEPTION_PLAYERS_BY_PLAYERS);
            }
            obertPerNosaltres.remove(membre);
            canviat = true;
        }
        return canviat;
    }

    /** Si OPAC ha dit que no deixa obrir el PvP (cal tocar la config del servidor d'OPAC). */
    public static boolean pvpBloquejat() {
        return pvpBloquejat;
    }

    // ---------------------------------------------------------------- Ajudes

    private static IPlayerConfigAPI config(MinecraftServer server, UUID jugador) {
        return PontOpac.api(server).getPlayerConfigManager().getLoadedConfig(jugador);
    }

    private static <T> void posar(IPlayerConfigAPI config, xaero.pac.common.server.player.config.api.v2.IPlayerConfigOptionSpecAPI<T> opcio,
                                  T valor) {
        if (java.util.Objects.equals(config.getRaw(opcio), valor)) {
            return;
        }
        IPlayerConfigAPI.SetResult r = config.tryToSet(opcio, valor);
        if (r != IPlayerConfigAPI.SetResult.SUCCESS) {
            PauITreva.LOG.warn("Pau i Treva: OPAC no ha deixat posar {} = {} a {}/{}: {}", opcio.getId(), valor,
                    config.getPlayerId(), config.getSubId(), r);
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
