package cat.mineuab.pauitreva.opac;

import cat.mineuab.pauitreva.model.Chunk;
import cat.mineuab.pauitreva.model.Guerra;
import cat.mineuab.pauitreva.mon.Dades;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import xaero.pac.common.claims.action.api.ClaimingAction;
import xaero.pac.common.server.claims.action.listener.api.IClaimActionListenerAPI;
import xaero.pac.common.server.claims.action.listener.override.api.ClaimActionPermissionOverride;
import xaero.pac.common.server.claims.action.listener.override.api.ClaimActionPermissionOverrideType;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

/**
 * No deixa reclamar ni desreclamar els chunks en disputa d'una guerra (ni canviar-los de sub-claim), perquè
 * ningú no se'n desfaci abans de la pau. Els chunks forçats sí que es poden canviar. Els canvis que fa el mod
 * mateix no passen per aquí.
 */
public final class VetClaims implements IClaimActionListenerAPI {

    @Override
    public String getName() {
        return "Pau i Treva";
    }

    @Override
    public ClaimActionPermissionOverride overrideClaimingActionPermission(UUID jugador, ResourceLocation dimensio,
            int x, int z, ClaimingAction accio, IServerClaimsManagerAPI claims, ClaimActionPermissionOverride ara,
            MinecraftServer server) {
        if (accio == ClaimingAction.FORCELOAD || accio == ClaimingAction.UNFORCELOAD) {
            return ara;
        }
        Chunk c = new Chunk(dimensio.toString(), x, z);
        for (Guerra g : Dades.registre(server).guerres()) {
            if (g.chunks().contains(c)) {
                return new ClaimActionPermissionOverride(ClaimActionPermissionOverrideType.FORBID,
                        Component.literal("Aquest chunk està en disputa en una guerra: no es pot tocar fins a la pau."));
            }
        }
        return ara;
    }

    @Override
    public void handleSuccessfulClaimingAction(UUID jugador, ResourceLocation dimensio, int x, int z,
                                               ClaimingAction accio, IServerClaimsManagerAPI claims,
                                               MinecraftServer server) {
    }
}
