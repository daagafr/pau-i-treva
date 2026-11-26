package cat.mineuab.pauitreva.proves;

import cat.mineuab.pauitreva.PauITreva;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.StructureGridSpawner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Fa les GameTests en un servidor dedicat normal (el servidor de GameTest de Minecraft no té la memòria de
 * perfils que OPAC fa servir en arrencar). Només actua amb {@code -Dpauitreva.proves=true}: espera uns segons,
 * llança totes les proves, i quan s'acaben escriu {@code resultat-proves.txt} i atura el servidor.
 */
@EventBusSubscriber(modid = PauITreva.ID)
public final class ExecutorProves {

    private static MultipleTestTracker seguiment;
    private static int espera = 100;
    private static boolean acabat;

    private ExecutorProves() {
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post e) {
        if (!Boolean.getBoolean("pauitreva.proves") || acabat) {
            return;
        }
        MinecraftServer server = e.getServer();
        if (seguiment == null) {
            if (--espera > 0) {
                return;
            }
            ServerLevel nivell = server.overworld();
            GameTestRunner runner = GameTestRunner.Builder
                    .fromBatches(GameTestBatchFactory.fromTestFunction(GameTestRegistry.getAllTestFunctions(), nivell), nivell)
                    .newStructureSpawner(new StructureGridSpawner(new BlockPos(2000, -59, 2000), 8, false))
                    .build();
            seguiment = new MultipleTestTracker(runner.getTestInfos());
            PauITreva.LOG.info("Pau i Treva: comencen {} proves", seguiment.getTotalCount());
            runner.start();
            return;
        }
        if (!seguiment.isDone()) {
            return;
        }
        acabat = true;
        List<String> linies = new ArrayList<>();
        Collection<GameTestInfo> fallades = seguiment.getFailedRequired();
        linies.add((fallades.isEmpty() ? "OK" : "FALLADES " + fallades.size()) + " de " + seguiment.getTotalCount());
        for (GameTestInfo f : fallades) {
            linies.add(f.getTestName() + ": " + (f.getError() == null ? "?" : f.getError().getMessage()));
        }
        linies.forEach(l -> PauITreva.LOG.info("Pau i Treva (proves): {}", l));
        try {
            Files.write(Path.of("resultat-proves.txt"), linies, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            PauITreva.LOG.error("No s'ha pogut escriure el resultat de les proves", ex);
        }
        server.halt(false);
    }
}
