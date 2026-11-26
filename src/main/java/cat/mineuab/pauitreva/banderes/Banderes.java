package cat.mineuab.pauitreva.banderes;

import cat.mineuab.pauitreva.PauITreva;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

/**
 * Les banderes que hi ha al servidor: les pintures dels datapacks en el format d'Immersive Paintings
 * ({@code data/<espai>/paintings/<nom>.png} i {@code .json}), com les 70 de {@code mineuab-banderes}. L'id d'una
 * bandera és el de la pintura ({@code mineuab:catalunya}): és la mateixa que es pot penjar al joc.
 *
 * <p>Es llegeixen totes en arrencar el servidor (i quan es tornen a carregar els datapacks) i es guarden en
 * memòria: el fil de l'API web i la pantalla les fan servir sense tocar el gestor de recursos.
 */
public final class Banderes {

    /** Una bandera: l'id, el nom que surt al json, i la imatge. */
    public record Bandera(String id, String nom, byte[] png) {
    }

    private static final int MIDA_MAX = 256 * 1024;
    private static volatile Map<String, Bandera> totes = Map.of();

    private Banderes() {
    }

    /** Torna a llegir les banderes dels datapacks. S'ha de cridar al fil del servidor. */
    public static void carregar(MinecraftServer server) {
        Map<String, Bandera> noves = new LinkedHashMap<>();
        var recursos = server.getResourceManager().listResources("paintings", rl -> rl.getPath().endsWith(".png"));
        recursos.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    ResourceLocation rl = e.getKey();
                    String nomFitxer = rl.getPath().substring("paintings/".length(), rl.getPath().length() - ".png".length());
                    String id = rl.getNamespace() + ":" + nomFitxer;
                    try (InputStream in = e.getValue().open()) {
                        byte[] png = in.readNBytes(MIDA_MAX + 1);
                        if (png.length > MIDA_MAX) {
                            PauITreva.LOG.warn("Pau i Treva: la bandera {} és massa gran (més de {} KB)", id, MIDA_MAX / 1024);
                            return;
                        }
                        noves.put(id, new Bandera(id, nom(server, rl.getNamespace(), nomFitxer), png));
                    } catch (IOException ex) {
                        PauITreva.LOG.warn("Pau i Treva: no s'ha pogut llegir la bandera {}", id, ex);
                    }
                });
        totes = Collections.unmodifiableMap(noves);
        PauITreva.LOG.info("Pau i Treva: {} banderes", noves.size());
    }

    /** El nom del json de la pintura, o el del fitxer amb espais si no n'hi ha. */
    private static String nom(MinecraftServer server, String espai, String fitxer) {
        Optional<Resource> json = server.getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(espai, "paintings/" + fitxer + ".json"));
        if (json.isPresent()) {
            try (var lector = new InputStreamReader(json.get().open(), StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(lector).getAsJsonObject();
                if (o.has("name")) {
                    return o.get("name").getAsString();
                }
            } catch (IOException | RuntimeException ex) {
                PauITreva.LOG.warn("Pau i Treva: el json de la bandera {}:{} no es pot llegir", espai, fitxer);
            }
        }
        String n = fitxer.replace('_', ' ');
        return n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    public static Optional<Bandera> bandera(String id) {
        return Optional.ofNullable(totes.get(id));
    }

    public static boolean existeix(String id) {
        return totes.containsKey(id);
    }

    /** Totes, per ordre d'id. */
    public static List<Bandera> totes() {
        return List.copyOf(totes.values());
    }
}
