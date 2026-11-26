package cat.mineuab.pauitreva.pantalla;

import cat.mineuab.pauitreva.PauITreva;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Els dos paquets de la pantalla, tots dos <b>opcionals</b>: qui no té el mod pot entrar al servidor i fa servir
 * les ordres, i un client amb el mod pot entrar a un servidor que no el té.
 *
 * <ul>
 *   <li>{@link Mostra} (servidor → client): la pantalla que s'ha de dibuixar.</li>
 *   <li>{@link Accio} (client → servidor): obrir la pantalla, canviar de pestanya o fer una ordre del mod.</li>
 * </ul>
 */
public final class Xarxa {

    /** Les ordres que es poden fer des de la pantalla. Qualsevol altra cosa es descarta. */
    private static final List<String> ORDRES = List.of("/nacio", "/diplomacia", "/guerra");

    /** La pestanya que té oberta cada jugador, per tornar-la a enviar després d'una acció. */
    private static final Map<UUID, String> OBERTES = new HashMap<>();

    /** El que fa el client quan rep una pantalla. El posa el codi de client (al servidor dedicat no hi és). */
    public static Consumer<Pantalla> rebreAlClient = p -> {
    };

    public record Mostra(Pantalla pantalla) implements CustomPacketPayload {
        public static final Type<Mostra> TIPUS = new Type<>(ResourceLocation.fromNamespaceAndPath(PauITreva.ID, "pantalla"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Mostra> CODI = Pantalla.CODI.map(Mostra::new, Mostra::pantalla);

        @Override
        public Type<Mostra> type() {
            return TIPUS;
        }
    }

    public record Accio(String accio) implements CustomPacketPayload {
        public static final Type<Accio> TIPUS = new Type<>(ResourceLocation.fromNamespaceAndPath(PauITreva.ID, "accio"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Accio> CODI =
                ByteBufCodecs.stringUtf8(512).<RegistryFriendlyByteBuf>cast().map(Accio::new, Accio::accio);

        @Override
        public Type<Accio> type() {
            return TIPUS;
        }
    }

    private Xarxa() {
    }

    public static void registrar(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1").optional();
        r.playToClient(Mostra.TIPUS, Mostra.CODI, (p, context) -> rebreAlClient.accept(p.pantalla()));
        r.playToServer(Accio.TIPUS, Accio.CODI, (p, context) -> {
            if (context.player() instanceof ServerPlayer jugador) {
                accio(jugador, p.accio());
            }
        });
    }

    /** Si el jugador té el mod (i, per tant, la pantalla). */
    public static boolean teLaPantalla(ServerPlayer jugador) {
        return jugador.connection.hasChannel(Mostra.TIPUS);
    }

    static void accio(ServerPlayer jugador, String accio) {
        List<Component> avisos = new ArrayList<>();
        String pestanya = OBERTES.getOrDefault(jugador.getUUID(), Vistes.PAIS);
        if (accio.equals("obrir")) {
            pestanya = OBERTES.getOrDefault(jugador.getUUID(), Vistes.PAIS);
        } else if (accio.startsWith("pestanya:")) {
            pestanya = accio.substring("pestanya:".length());
        } else if (permesa(accio)) {
            fer(jugador, accio, avisos);
        } else {
            PauITreva.LOG.warn("Pau i Treva: acció de pantalla no permesa de {}: {}", jugador.getGameProfile().getName(), accio);
            return;
        }
        mostrar(jugador, pestanya, avisos);
    }

    /** Si una acció de la pantalla és una ordre que es pot fer des d'aquí (només les del mod). */
    public static boolean permesa(String accio) {
        return accio.startsWith("/") && ORDRES.stream().anyMatch(o -> accio.equals(o) || accio.startsWith(o + " "))
                && accio.indexOf('\n') < 0;
    }

    /** Envia (o torna a enviar) la pantalla d'una pestanya. */
    public static void mostrar(ServerPlayer jugador, String pestanya, List<Component> avisos) {
        if (!teLaPantalla(jugador)) {
            return;
        }
        OBERTES.put(jugador.getUUID(), pestanya);
        PacketDistributor.sendToPlayer(jugador, new Mostra(Vistes.construir(jugador, pestanya, avisos)));
    }

    /** Fa una ordre com si l'hagués escrit el jugador i en guarda les respostes per ensenyar-les a la pantalla. */
    private static void fer(ServerPlayer jugador, String ordre, List<Component> avisos) {
        CommandSource recull = new CommandSource() {
            @Override
            public void sendSystemMessage(Component missatge) {
                avisos.add(missatge);
                jugador.sendSystemMessage(missatge);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack font = jugador.createCommandSourceStack().withSource(recull);
        jugador.server.getCommands().performPrefixedCommand(font, ordre);
        if (avisos.size() > 6) {
            avisos.subList(0, avisos.size() - 6).clear();
            avisos.add(0, Component.literal("…").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** En sortir, s'oblida la pestanya. */
    public static void sortir(ServerPlayer jugador) {
        OBERTES.remove(jugador.getUUID());
    }
}
