package cat.mineuab.pauitreva.mixin;

import cat.mineuab.pauitreva.guerra.PvpGuerra;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.pac.common.server.IServerData;
import xaero.pac.common.server.claims.protection.ChunkProtection;

/**
 * El PvP de guerra. OPAC decide el PvP pel lloc (l'opció {@code playersByPlayers} dels claims) i no té cap API
 * per fer-ne una excepció per a uns jugadors concrets. Aquí, a l'única funció d'OPAC que decideix si una
 * entitat pot atacar-ne una altra (cops, fletxes i dany), es diu «no protegit» quan atacant i víctima són
 * enemics en una guerra activa ({@link PvpGuerra#permet}). Tota la resta ho decideix OPAC com sempre.
 *
 * <p>Si una versió nova d'OPAC canvia aquesta funció, la injecció no es fa (no és obligatòria) i el joc no peta:
 * només no hi ha PvP de guerra, i {@code /guerra admin diagnosi} ho diu.
 */
@Mixin(value = ChunkProtection.class, remap = false)
public abstract class ProteccioOpacMixin {

    @Inject(method = "onEntityInteraction(Lxaero/pac/common/server/IServerData;Lnet/minecraft/world/entity/Entity;"
            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/InteractionHand;ZZZ)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void pauitreva$pvpDeGuerra(IServerData<?, ?> dades, Entity indirecta, Entity directa, Entity objectiu,
                                       ItemStack objecte, InteractionHand ma, boolean atac, boolean missatges,
                                       boolean excepcions, CallbackInfoReturnable<Boolean> resultat) {
        PvpGuerra.aplicat = true;
        if (PvpGuerra.permet(indirecta, directa, objectiu)) {
            resultat.setReturnValue(false);
        }
    }
}
