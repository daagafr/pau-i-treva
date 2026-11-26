package cat.mineuab.pauitreva.client;

import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.pantalla.Xarxa;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** La part del client: la tecla N i rebre la pantalla. Al servidor dedicat no es carrega. */
@EventBusSubscriber(modid = PauITreva.ID, value = Dist.CLIENT)
public final class ClientPauITreva {

    public static final KeyMapping TECLA = new KeyMapping("key.pauitreva.pantalla", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N, "key.categories.pauitreva");

    private ClientPauITreva() {
    }

    @SubscribeEvent
    static void tecles(RegisterKeyMappingsEvent e) {
        e.register(TECLA);
    }

    @SubscribeEvent
    static void preparar(FMLClientSetupEvent e) {
        Xarxa.rebreAlClient = PantallaPais::rebre;
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ClientPauITreva::tick);
    }

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (TECLA.consumeClick()) {
            if (mc.player == null || mc.getConnection() == null || mc.screen != null) {
                continue;
            }
            if (!mc.getConnection().hasChannel(Xarxa.Accio.TIPUS)) {
                mc.player.displayClientMessage(Component.literal("Aquest servidor no té Pau i Treva.")
                        .withStyle(ChatFormatting.GRAY), true);
                continue;
            }
            obrir("obrir");
        }
    }

    /** Demana la pantalla al servidor (en una pestanya, o la darrera amb {@code "obrir"}) i l'obre quan arriba. */
    public static void obrir(String accio) {
        PantallaPais.esperant = true;
        PacketDistributor.sendToServer(new Xarxa.Accio(accio));
    }
}
