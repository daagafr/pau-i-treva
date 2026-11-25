package cat.mineuab.pauitreva.opac;

import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.model.Rang;
import cat.mineuab.pauitreva.model.Validacio;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigOptionSpecAPI;
import xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions;

/**
 * Tot el que Pau i Treva llegeix o canvia d'Open Parties and Claims passa per aquí.
 *
 * <p>Un país és una party d'OPAC: mateix id, mateixos membres i mateixos rangs. Els claims del país són els
 * del líder (opció {@code partyOwnedClaims} d'OPAC), i per això el nom i el color del país es posen a la
 * config del líder.
 */
public final class PontOpac {

    private PontOpac() {
    }

    public static OpenPACServerAPI api(MinecraftServer server) {
        return OpenPACServerAPI.get(server);
    }

    /** La party on és el jugador, o {@code null}. */
    public static IServerPartyAPI partitDe(MinecraftServer server, UUID jugador) {
        return api(server).getPartyManager().getPartyByMember(jugador);
    }

    /** La party amb aquest id, o {@code null} si ja no existeix. */
    public static IServerPartyAPI partit(MinecraftServer server, UUID id) {
        return api(server).getPartyManager().getPartyById(id);
    }

    /** Crea una party nova amb el jugador de propietari. Pot tornar {@code null} si OPAC no ho deixa. */
    public static IServerPartyAPI crearPartit(ServerPlayer lider) {
        return api(lider.getServer()).getPartyManager().createPartyForOwner(lider);
    }

    /** El rang del jugador dins del país, o {@code null} si no n'és membre. */
    public static Rang rang(IServerPartyAPI partit, UUID jugador) {
        IPartyMemberAPI membre = partit.getMemberInfo(jugador);
        if (membre == null) {
            return null;
        }
        if (membre.isOwner()) {
            return Rang.LIDER;
        }
        return switch (membre.getRank()) {
            case ADMIN -> Rang.MINISTRE;
            case MODERATOR -> Rang.OFICIAL;
            case CLAIMER -> Rang.COLON;
            default -> Rang.CIUTADA;
        };
    }

    public static String nomLider(IServerPartyAPI partit) {
        return partit.getOwner().getUsername();
    }

    /** Els noms dels membres, de més rang a menys. */
    public static List<IPartyMemberAPI> membres(IServerPartyAPI partit) {
        return partit.getMemberInfoStream()
                .sorted((x, y) -> {
                    int rx = x.isOwner() ? 99 : x.getRank().ordinal();
                    int ry = y.isOwner() ? 99 : y.getRank().ordinal();
                    return rx != ry ? Integer.compare(ry, rx) : x.getUsername().compareToIgnoreCase(y.getUsername());
                })
                .toList();
    }

    /**
     * Posa el nom i el color del país als claims i a la party del líder. Només toca el que ha canviat.
     *
     * @return {@code false} si la party o la config del líder no hi són
     */
    public static boolean aplicarIdentitat(MinecraftServer server, Pais pais) {
        IServerPartyAPI partit = partit(server, pais.id());
        if (partit == null) {
            return false;
        }
        IPlayerConfigAPI config = api(server).getPlayerConfigManager().getLoadedConfig(partit.getOwner().getUUID());
        if (config == null) {
            return false;
        }
        // OPAC llegeix el color 0 com «el de per defecte»: el negre pur es posa gairebé negre.
        int color = pais.color() == 0 ? 0x010101 : pais.color();
        String nom = Validacio.nomPerOpac(pais.nom());
        posar(config, PlayerConfigOptions.CLAIMS_COLOR, color);
        posar(config, PlayerConfigOptions.CLAIMS_NAME, nom);
        posar(config, PlayerConfigOptions.PARTY_NAME, nom);
        return true;
    }

    /** Els canvis que OPAC ja ha rebutjat, perquè l'avís surti un sol cop i no cada 30 s. */
    private static final Set<String> REBUTJATS = new HashSet<>();

    private static <T> void posar(IPlayerConfigAPI config, IPlayerConfigOptionSpecAPI<T> opcio, T valor) {
        if (Objects.equals(config.getRaw(opcio), valor)) {
            return;
        }
        String clau = config.getPlayerId() + " " + opcio.getId() + " " + valor;
        if (REBUTJATS.contains(clau)) {
            return;
        }
        IPlayerConfigAPI.SetResult r = config.tryToSet(opcio, valor);
        if (r != IPlayerConfigAPI.SetResult.SUCCESS) {
            REBUTJATS.add(clau);
            PauITreva.LOG.warn("Pau i Treva: OPAC no ha deixat posar {} = {} a {}: {}", opcio.getId(), valor,
                    config.getPlayerId(), r);
        }
    }

    /** Fa aliades les dues parties, en tots dos sentits (a OPAC, ser aliat és d'una sola direcció). */
    public static void aliar(MinecraftServer server, UUID a, UUID b) {
        IServerPartyAPI pa = partit(server, a);
        IServerPartyAPI pb = partit(server, b);
        if (pa != null && !pa.isAlly(b)) {
            pa.addAllyParty(b);
        }
        if (pb != null && !pb.isAlly(a)) {
            pb.addAllyParty(a);
        }
    }

    /** Treu l'aliança entre les dues parties, en tots dos sentits. */
    public static void desaliar(MinecraftServer server, UUID a, UUID b) {
        IServerPartyAPI pa = partit(server, a);
        IServerPartyAPI pb = partit(server, b);
        if (pa != null && pa.isAlly(b)) {
            pa.removeAllyParty(b);
        }
        if (pb != null && pb.isAlly(a)) {
            pb.removeAllyParty(a);
        }
    }

    /**
     * Els chunks reclamats pels membres del país (amb claims de party, els del líder ja són els del país),
     * per a cada dimensió, com a parells {x, z}.
     */
    public static Map<String, List<int[]>> territori(MinecraftServer server, IServerPartyAPI partit) {
        IServerClaimsManagerAPI claims = api(server).getServerClaimsManager();
        Map<String, List<int[]>> territori = new TreeMap<>();
        partit.getMemberInfoStream().forEach(m -> {
            if (!claims.hasPlayerInfo(m.getUUID())) {
                return;
            }
            IServerPlayerClaimInfoAPI info = claims.getPlayerInfo(m.getUUID());
            info.getStream().forEach(dim -> {
                List<int[]> llista = territori.computeIfAbsent(dim.getKey().toString(), k -> new ArrayList<>());
                dim.getValue().getStream().forEach(posicions ->
                        posicions.getStream().forEach(chunk -> llista.add(new int[] {chunk.x, chunk.z})));
            });
        });
        return territori;
    }

    /** Si el chunk és reclamat per algun membre del país (o pel país, amb claims de party). */
    public static boolean chunkDelPais(MinecraftServer server, IServerPartyAPI partit, ResourceLocation dimensio,
                                       int chunkX, int chunkZ) {
        IPlayerChunkClaimAPI claim = api(server).getServerClaimsManager().get(dimensio, chunkX, chunkZ);
        return claim != null && partit.getMemberInfo(claim.getPlayerId()) != null;
    }
}
