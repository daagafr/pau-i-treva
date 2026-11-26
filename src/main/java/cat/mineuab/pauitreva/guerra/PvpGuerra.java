package cat.mineuab.pauitreva.guerra;

import cat.mineuab.pauitreva.Config;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * Qui pot fer mal a qui durant la guerra, per damunt de la protecció d'OPAC (decidit el 26/11): només els enemics
 * d'una guerra activa, amb la finestra oberta, i tots dos al territori dels seus dos països (o a la natura, on el
 * PvP ja hi és sempre). Ni companys del mateix país, ni neutrals, ni al territori d'un tercer país.
 */
public final class PvpGuerra {

    /** Si OPAC ha cridat mai la funció on hi ha l'excepció (vol dir que el mixin s'ha aplicat). */
    public static volatile boolean aplicat;

    private PvpGuerra() {
    }

    /**
     * @param indirecta qui ho fa en última instància (el que ha tirat la fletxa), o {@code null}
     * @param directa   el que toca (el jugador o la fletxa)
     */
    public static boolean permet(Entity indirecta, Entity directa, Entity objectiu) {
        if (!Batalles.finestraOberta() || !(objectiu instanceof ServerPlayer victima)) {
            return false;
        }
        ServerPlayer atacant = jugador(indirecta);
        if (atacant == null) {
            atacant = jugador(directa);
        }
        if (atacant == null || atacant == victima || !Config.o(Config.OBRIR_PVP, true)) {
            return false;
        }
        MinecraftServer server = victima.getServer();
        if (server == null) {
            return false;
        }
        IServerPartyAPI pa = PontOpac.partitDe(server, atacant.getUUID());
        IServerPartyAPI pv = PontOpac.partitDe(server, victima.getUUID());
        if (pa == null || pv == null || pa.getId().equals(pv.getId())) {
            return false;
        }
        Guerra g = Dades.registre(server).guerraEntre(pa.getId(), pv.getId()).orElse(null);
        if (g == null || !g.activa(System.currentTimeMillis())) {
            return false;
        }
        return deUnDelsDos(server, victima, pa.getId(), pv.getId()) && deUnDelsDos(server, atacant, pa.getId(), pv.getId());
    }

    private static ServerPlayer jugador(Entity e) {
        if (e instanceof ServerPlayer j) {
            return j;
        }
        if (e instanceof Projectile p && p.getOwner() instanceof ServerPlayer j) {
            return j;
        }
        return null;
    }

    /** Si el jugador és a la natura o en un chunk d'un dels dos països. */
    private static boolean deUnDelsDos(MinecraftServer server, ServerPlayer j, UUID a, UUID b) {
        IPlayerChunkClaimAPI claim = PontOpac.api(server).getServerClaimsManager()
                .get(j.level().dimension().location(), j.chunkPosition());
        if (claim == null) {
            return true;
        }
        IServerPartyAPI amo = PontOpac.partitDe(server, claim.getPlayerId());
        return amo != null && (amo.getId().equals(a) || amo.getId().equals(b));
    }
}
