package cat.mineuab.pauitreva.ordres;

import cat.mineuab.pauitreva.opac.PontOpac;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * Avisos a pantalla (títol, subtítol, barra d'acció) i sons. Són paquets de Minecraft normals: funcionen sense
 * que el jugador tingui el mod.
 */
public final class Avisos {

    /** Els sons de cada mena d'avís. */
    public enum So {
        GUERRA(SoundEvents.RAID_HORN),
        PAU(SoundEvents.BELL_BLOCK),
        DIPLOMACIA(SoundEvents.PLAYER_LEVELUP),
        FINESTRA(SoundEvents.NOTE_BLOCK_BELL),
        CAPTURA(SoundEvents.EXPERIENCE_ORB_PICKUP),
        PERDUA(SoundEvents.NOTE_BLOCK_BASS),
        SUAU(SoundEvents.NOTE_BLOCK_CHIME);

        private final SoundEvent so;

        So(SoundEvent so) {
            this.so = so;
        }

        So(Holder<SoundEvent> so) {
            this.so = so.value();
        }
    }

    private Avisos() {
    }

    /** Els jugadors connectats d'un país. */
    public static List<ServerPlayer> jugadors(MinecraftServer server, UUID pais) {
        IServerPartyAPI partit = PontOpac.partit(server, pais);
        return partit == null ? List.of() : partit.getOnlineMemberStream().toList();
    }

    /** Els jugadors connectats de diversos països, sense repetir. */
    public static List<ServerPlayer> jugadors(MinecraftServer server, UUID... paisos) {
        return Stream.of(paisos).flatMap(p -> jugadors(server, p).stream()).distinct().toList();
    }

    /** Títol i subtítol al mig de la pantalla, amb so. {@code subtitol} pot ser {@code null}. */
    public static void titol(Collection<ServerPlayer> jugadors, Component titol, Component subtitol, So so) {
        for (ServerPlayer j : jugadors) {
            j.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            if (subtitol != null) {
                j.connection.send(new ClientboundSetSubtitleTextPacket(subtitol));
            }
            j.connection.send(new ClientboundSetTitleTextPacket(titol));
            so(j, so);
        }
    }

    /** Un text curt a sobre de la barra d'objectes. */
    public static void accio(ServerPlayer jugador, Component text, So so) {
        jugador.connection.send(new ClientboundSetActionBarTextPacket(text));
        if (so != null) {
            so(jugador, so);
        }
    }

    public static void xat(Collection<ServerPlayer> jugadors, Component text) {
        for (ServerPlayer j : jugadors) {
            j.sendSystemMessage(text);
        }
    }

    public static void so(ServerPlayer jugador, So so) {
        if (so != null) {
            jugador.playNotifySound(so.so, SoundSource.MASTER, 0.8f, 1.0f);
        }
    }

    /**
     * Un avís per a tothom: al xat (com {@link Missatges#anunciar}) i un so suau. Els països afectats, a més,
     * el veuen en gran amb el so de la mena d'avís.
     */
    public static void anunciar(MinecraftServer server, String text, So so, Component titol, Component subtitol,
                                UUID... afectats) {
        Missatges.anunciar(server, text);
        List<ServerPlayer> seus = jugadors(server, afectats);
        for (ServerPlayer j : server.getPlayerList().getPlayers()) {
            if (!seus.contains(j)) {
                so(j, So.SUAU);
            }
        }
        if (titol != null) {
            titol(seus, titol, subtitol, so);
        } else {
            seus.forEach(j -> so(j, so));
        }
    }
}
