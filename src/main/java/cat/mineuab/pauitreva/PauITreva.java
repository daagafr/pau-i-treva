package cat.mineuab.pauitreva;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Punt d'entrada del mod. */
@Mod(PauITreva.ID)
public final class PauITreva {
    public static final String ID = "pauitreva";
    public static final Logger LOG = LogUtils.getLogger();

    public PauITreva(IEventBus busDelMod, ModContainer contenidor) {
        LOG.info("Pau i Treva carregat");
    }
}
