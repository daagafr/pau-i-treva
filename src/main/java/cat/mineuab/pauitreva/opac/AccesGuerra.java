package cat.mineuab.pauitreva.opac;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import xaero.pac.common.server.claims.protection.override.api.ChunkAccessOverride;
import xaero.pac.common.server.claims.protection.override.api.ChunkAccessOverrideType;
import xaero.pac.common.server.claims.protection.override.api.IChunkAccessOverriderAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI;

/**
 * La zona en disputa, només per als bel·ligerants (decidit el 26/11): durant la finestra, els membres dels dos
 * països en guerra tenen accés complet als chunks de l'objectiu (trencar, posar, obrir, fer servir); per a la resta
 * continuen protegits. OPAC ho pregunta a cada accés a un claim, i per això la resposta surt d'un mapa en memòria
 * que la batalla torna a fer cada pocs segons ({@link #posar}).
 */
public final class AccesGuerra implements IChunkAccessOverriderAPI {

    private static final ChunkAccessOverride PERMES = new ChunkAccessOverride(ChunkAccessOverrideType.ALLOW);

    /** Per a cada dimensió i chunk ({@link ChunkPos#asLong}), els països que hi tenen accés ara. */
    private static volatile Map<ResourceLocation, Map<Long, Set<UUID>>> zona = Map.of();

    public static void posar(Map<ResourceLocation, Map<Long, Set<UUID>>> nova) {
        zona = nova;
    }

    public static boolean buida() {
        return zona.isEmpty();
    }

    @Override
    public String getName() {
        return "Pau i Treva";
    }

    @Override
    public ChunkAccessOverride overrideChunkAccess(ResourceLocation dim, int x, int z, IPlayerConfigAPI config,
                                                   Entity qui, UUID quiId, MinecraftServer server,
                                                   ChunkAccessOverride ara) {
        if (dim == null || x == Integer.MIN_VALUE) {
            return ara;
        }
        Map<Long, Set<UUID>> perDim = zona.get(dim);
        if (perDim == null) {
            return ara;
        }
        Set<UUID> paisos = perDim.get(ChunkPos.asLong(x, z));
        if (paisos == null) {
            return ara;
        }
        IServerPartyAPI partit = PontOpac.partitDe(server, quiId);
        return partit != null && paisos.contains(partit.getId()) ? PERMES : ara;
    }
}
