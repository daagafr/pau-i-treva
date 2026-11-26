package cat.mineuab.pauitreva.client;

import cat.mineuab.pauitreva.PauITreva;
import cat.mineuab.pauitreva.pantalla.Xarxa;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Les banderes que ha enviat el servidor, convertides en textures. Duren tota la sessió del joc: si el servidor
 * en torna a enviar una amb el mateix id, la nova substitueix la vella.
 */
public final class BanderesClient {

    /** Una bandera a punt per dibuixar: la textura i la seva mida en píxels. */
    public record Textura(ResourceLocation rl, int ample, int alt) {
    }

    private static final Map<String, Textura> TEXTURES = new HashMap<>();

    private BanderesClient() {
    }

    static void rebre(Xarxa.Imatge imatge) {
        try {
            NativeImage img = NativeImage.read(imatge.png());
            String cami = "bandera/" + imatge.id().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "/");
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(PauITreva.ID, cami);
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(img));
            TEXTURES.put(imatge.id(), new Textura(rl, img.getWidth(), img.getHeight()));
        } catch (IOException | RuntimeException e) {
            PauITreva.LOG.warn("Pau i Treva: la bandera {} no és un PNG que es pugui llegir", imatge.id(), e);
        }
    }

    /** La textura d'una bandera, o {@code null} si encara no ha arribat (o l'id és buit). */
    public static Textura textura(String id) {
        return id == null || id.isEmpty() ? null : TEXTURES.get(id);
    }
}
