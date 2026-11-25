package cat.mineuab.pauitreva;

import cat.mineuab.pauitreva.ordres.Ordres;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Punt d'entrada del mod. De moment tot és de servidor (ordres i dades): el jar no registra cap paquet de
 * xarxa, i per tant es pot posar només al servidor sense que els jugadors l'hagin de tenir.
 */
@Mod(PauITreva.ID)
public final class PauITreva {
    public static final String ID = "pauitreva";
    public static final Logger LOG = LogUtils.getLogger();

    public PauITreva(IEventBus busDelMod, ModContainer contenidor) {
        contenidor.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> Ordres.registrar(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, e -> Manteniment.engegar(e.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> Manteniment.aturar());
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, e -> Manteniment.tick(e.getServer()));
    }
}
