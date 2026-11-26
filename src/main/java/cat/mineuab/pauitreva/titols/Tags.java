package cat.mineuab.pauitreva.titols;

import cat.mineuab.pauitreva.Config;
import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.model.Pais;
import cat.mineuab.pauitreva.mon.Dades;
import cat.mineuab.pauitreva.opac.PontOpac;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

/**
 * El tag del país de cada jugador connectat ({@code #F1C40F&l[CAT]&r }), per al xat i per damunt del cap.
 *
 * <ul>
 *   <li><b>Xat i llista del TAB</b>: el tag es posa com a prefix del jugador a LuckPerms, només amb el context
 *       {@code server=create} (al MineUAB no hi surt). La pila de prefixos de la config de LuckPerms del Create
 *       el posa després del rang i de la universitat (vegeu {@code desplegament/}).</li>
 *   <li><b>Damunt del cap</b>: el placeholder {@code %pauitreva-tag%} del TAB, amb només el país.</li>
 * </ul>
 *
 * <p>Tots dos són opcionals: si LuckPerms o el TAB no hi són, no es fa res. El mapa el posa al dia el fil del
 * servidor; el TAB el llegeix des del seu fil.
 */
public final class Tags {

    private static final Map<UUID, String> TAGS = new ConcurrentHashMap<>();
    private static boolean avisatLuckPerms;

    private Tags() {
    }

    /** El tag que porta ara un jugador connectat, o buit si no és de cap país. Es pot cridar des de qualsevol fil. */
    public static String tag(UUID jugador) {
        return TAGS.getOrDefault(jugador, "");
    }

    /** El text del tag d'un país, segons la config ({@code tagFormat}). */
    public static String text(Pais p) {
        String color = String.format(Locale.ROOT, "%06X", p.color() & 0xFFFFFF);
        return Config.o(Config.TAG_FORMAT, "#%color%&l[%sigla%]&r ").replace("%color%", color).replace("%sigla%", p.sigla());
    }

    /** Torna a calcular el tag de tots els jugadors connectats i escriu a LuckPerms el que hagi canviat. */
    public static void actualitzar(MinecraftServer server) {
        Set<UUID> connectats = new HashSet<>();
        for (ServerPlayer j : server.getPlayerList().getPlayers()) {
            connectats.add(j.getUUID());
            actualitzar(server, j);
        }
        TAGS.keySet().retainAll(connectats);
    }

    public static void actualitzar(MinecraftServer server, ServerPlayer j) {
        IServerPartyAPI partit = PontOpac.partitDe(server, j.getUUID());
        Pais p = partit == null ? null : Dades.registre(server).pais(partit.getId()).orElse(null);
        String nou = p == null ? "" : text(p);
        String abans = TAGS.put(j.getUUID(), nou);
        if (!nou.equals(abans) && Config.o(Config.TAG_LUCKPERMS, true) && ModList.get().isLoaded("luckperms")) {
            try {
                LuckPermsTags.posar(j.getUUID(), nou.isEmpty() ? null : nou, Config.o(Config.TAG_PRIORITAT, 5),
                        Config.o(Config.TAG_SERVIDOR, "create"));
            } catch (RuntimeException | LinkageError e) {
                if (!avisatLuckPerms) {
                    avisatLuckPerms = true;
                    PauITreva.LOG.warn("Pau i Treva: no s'ha pogut posar el tag del país a LuckPerms", e);
                }
            }
        }
    }

    public static void sortir(ServerPlayer j) {
        TAGS.remove(j.getUUID());
    }

    /** En arrencar: el placeholder del TAB, si hi és. */
    public static void engegar() {
        if (ModList.get().isLoaded("tab")) {
            try {
                TabTags.registrar();
            } catch (RuntimeException | LinkageError e) {
                PauITreva.LOG.warn("Pau i Treva: no s'ha pogut registrar el placeholder del TAB", e);
            }
        }
    }
}
