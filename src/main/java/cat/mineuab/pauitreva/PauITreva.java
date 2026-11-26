package cat.mineuab.pauitreva;

import cat.mineuab.pauitreva.guerra.Batalles;
import cat.mineuab.pauitreva.opac.AccesGuerra;
import cat.mineuab.pauitreva.opac.VetClaims;
import cat.mineuab.pauitreva.ordres.Ordres;
import cat.mineuab.pauitreva.ordres.OrdresGuerra;
import cat.mineuab.pauitreva.pantalla.Xarxa;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import xaero.pac.common.event.api.v2.OPACServerAddonRegisterEvent;

/**
 * Punt d'entrada del mod. Gairebé tot és de servidor (ordres, dades, guerra i avisos amb paquets de Minecraft
 * normals). La pantalla del país (tecla N) fa servir dos paquets propis <b>opcionals</b>: qui no té el mod pot
 * entrar igualment al servidor i fer servir les ordres.
 */
@Mod(PauITreva.ID)
public final class PauITreva {
    public static final String ID = "pauitreva";
    public static final Logger LOG = LogUtils.getLogger();

    public PauITreva(IEventBus busDelMod, ModContainer contenidor) {
        contenidor.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        busDelMod.addListener(RegisterPayloadHandlersEvent.class, Xarxa::registrar);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> {
            Ordres.registrar(e.getDispatcher());
            OrdresGuerra.registrar(e.getDispatcher());
        });
        NeoForge.EVENT_BUS.addListener(OPACServerAddonRegisterEvent.class, e -> {
            e.getContext().getClaimActionListenerManagerAPI().register(new VetClaims());
            e.getContext().getChunkAccessOverriderManagerAPI().register(new AccesGuerra());
        });
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, e -> {
            Manteniment.engegar(e.getServer());
            Batalles.engegar(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> {
            Manteniment.aturar();
            Batalles.aturar();
        });
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> {
            Manteniment.tick(e.getServer());
            Batalles.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, Batalles::mort);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer jugador) {
                Batalles.entrar(jugador);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer jugador) {
                Xarxa.sortir(jugador);
            }
        });
    }
}
