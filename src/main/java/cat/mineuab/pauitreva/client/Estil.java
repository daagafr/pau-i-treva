package cat.mineuab.pauitreva.client;

import cat.mineuab.pauitreva.PauITreva;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * L'aspecte de la pantalla del país: fusta fosca i or, inspirat en els jocs de Paradox però propi. Tot es dibuixa
 * amb rectangles (sense textures), menys les icones ({@code textures/gui/icones.png}, de {@code eines/icones.py}).
 */
public final class Estil {

    // Colors (ARGB).
    public static final int NEGRE = 0xFF0E0B08;
    public static final int FONS = 0xFF1E1A16;
    public static final int FONS_CLAR = 0xFF2A231C;
    public static final int FONS_FOSC = 0xFF16120F;
    public static final int INSERIT = 0xFF120F0C;
    public static final int OR = 0xFFC9A14A;
    public static final int OR_CLAR = 0xFFE8C977;
    public static final int OR_FOSC = 0xFF8C6F35;
    public static final int OR_APAGAT = 0xFF4A3F33;
    public static final int TEXT = 0xFFE8DCC0;
    public static final int TEXT_GRIS = 0xFFA8997C;
    public static final int TEXT_APAGAT = 0xFF6E6252;

    /** L'ordre de les icones a l'atles: el mateix que NOMS a eines/icones.py. */
    private static final List<String> ICONES = List.of("corona", "membres", "territori", "capital", "guerra", "pau",
            "alianca", "pacte", "treva", "baixes", "vides", "diari", "diplomacia", "punts", "objectiu", "escut");
    private static final ResourceLocation ATLES = ResourceLocation.fromNamespaceAndPath(PauITreva.ID, "textures/gui/icones.png");

    private Estil() {
    }

    /** Costat d'una icona a l'atles, en píxels. Es dibuixa en 16 unitats de GUI: a l'escala 2, píxel a píxel. */
    private static final int COSTAT = 32;

    /** Una icona en 16×16 unitats de GUI. Si no existeix, l'escut. */
    public static void icona(GuiGraphics g, String nom, int x, int y) {
        int i = ICONES.indexOf(nom);
        if (i < 0) {
            i = ICONES.indexOf("escut");
        }
        g.blit(ATLES, x, y, 16, 16, i * COSTAT, 0, COSTAT, COSTAT, ICONES.size() * COSTAT, COSTAT);
    }

    /** El marc gran de la finestra: vores doble d'or, fons de fusta fosca i quatre reblons als racons. */
    public static void marc(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, NEGRE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, OR_FOSC);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, NEGRE);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, OR);
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, NEGRE);
        g.fillGradient(x + 5, y + 5, x + w - 5, y + h - 5, FONS_CLAR, FONS_FOSC);
        for (int[] c : new int[][] {{x, y}, {x + w - 7, y}, {x, y + h - 7}, {x + w - 7, y + h - 7}}) {
            g.fill(c[0], c[1], c[0] + 7, c[1] + 7, NEGRE);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 6, c[1] + 6, OR_CLAR);
            g.fill(c[0] + 2, c[1] + 2, c[0] + 5, c[1] + 5, OR_FOSC);
            g.fill(c[0] + 3, c[1] + 3, c[0] + 4, c[1] + 4, OR_CLAR);
        }
    }

    /** Un requadre enfonsat (on hi ha el contingut, els camps de text...). */
    public static void inserit(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF5A4626);
        g.fill(x, y, x + w - 1, y + h - 1, NEGRE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, INSERIT);
    }

    /** El fons d'una secció: una mica més clar que el contingut, amb una vora d'or apagat. */
    public static void caixa(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OR_APAGAT);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF241E18, 0xFF1A1612);
    }

    /** El títol d'una secció: icona, text d'or i una línia que s'acaba amb un rombe. */
    public static void titolSeccio(GuiGraphics g, Font f, int x, int y, int w, Component titol, String icona) {
        int tx = x + 4;
        if (!icona.isEmpty()) {
            icona(g, icona, x + 3, y - 1);
            tx += 18;
        }
        g.drawString(f, titol, tx, y + 3, OR_CLAR, true);
        int lx = tx + f.width(titol) + 6;
        int ly = y + 7;
        if (lx < x + w - 12) {
            g.fill(lx, ly, x + w - 10, ly + 1, OR_FOSC);
            g.fill(x + w - 10, ly - 1, x + w - 8, ly + 2, OR);
        }
    }

    /**
     * Un escut heràldic del color del país amb la sigla a dins. Mides normals: 30×36 (capçalera) i 12×14 (llistes).
     * Sense sigla, l'escut del mod (or i vermell).
     */
    public static void escut(GuiGraphics g, Font f, int x, int y, int w, int h, int color, String sigla) {
        forma(g, x, y, w, h, NEGRE);
        forma(g, x + 1, y + 1, w - 2, h - 2, OR);
        int c = 0xFF000000 | (sigla.isEmpty() ? 0xC0392B : color);
        forma(g, x + 2, y + 2, w - 4, h - 4, c);
        // Una mica de volum: la meitat de dalt més clara.
        int clar = 0x30FFFFFF;
        g.fill(x + 2, y + 2, x + w - 2, y + 2 + (h - 4) / 3, clar);
        if (w >= 20 && !sigla.isEmpty()) {
            boolean fosc = lluminositat(color) > 150;
            int tc = fosc ? 0xFF1A1410 : 0xFFF2EAD8;
            g.drawString(f, sigla, x + (w - f.width(sigla)) / 2, y + h / 2 - 6, tc, !fosc);
        } else if (sigla.isEmpty()) {
            g.fill(x + w / 2 - 1, y + 3, x + w / 2 + 1, y + h - 5, OR_CLAR);
        }
    }

    /** La silueta d'un escut: rectangle a dalt i punxa a baix. */
    private static void forma(GuiGraphics g, int x, int y, int w, int h, int color) {
        int recte = h * 55 / 100;
        for (int r = 0; r < h; r++) {
            int entra = r < recte ? 0 : (int) Math.ceil((r - recte + 1) * (w / 2.0) / (h - recte));
            if (w - 2 * entra <= 0) {
                break;
            }
            g.fill(x + entra, y + r, x + w - entra, y + r + 1, color);
        }
    }

    /** Una barra: la part de l'esquerra d'un color fins a {@code valor} (0–1), la resta d'un altre, i el text al mig. */
    public static void barra(GuiGraphics g, Font f, int x, int y, int w, int h, float valor, int esquerra, int dreta,
                             Component text) {
        g.fill(x, y, x + w, y + h, NEGRE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, OR_FOSC);
        int ix = x + 2;
        int iw = w - 4;
        int talla = ix + Math.round(iw * Math.max(0, Math.min(1, valor)));
        g.fillGradient(ix, y + 2, talla, y + h - 2, 0xFF000000 | aclarir(esquerra), 0xFF000000 | esquerra);
        g.fillGradient(talla, y + 2, ix + iw, y + h - 2, 0xFF000000 | aclarir(dreta), 0xFF000000 | dreta);
        g.fill(talla - (talla > ix ? 1 : 0), y + 1, talla + 1, y + h - 1, OR_CLAR);
        g.fill(x + w / 2, y + 1, x + w / 2 + 1, y + 3, NEGRE);
        g.fill(x + w / 2, y + h - 3, x + w / 2 + 1, y + h - 1, NEGRE);
        if (!text.getString().isEmpty()) {
            g.drawString(f, text, x + (w - f.width(text)) / 2, y + (h - 8) / 2, 0xFFFFFFFF, true);
        }
    }

    /** Un text d'ajuda amb l'estil de la pantalla (fosc, vora d'or), a prop del ratolí i dins de la pantalla. */
    public static void ajuda(GuiGraphics g, Font f, Component text, int mx, int my, int ampleP, int altP) {
        List<net.minecraft.util.FormattedCharSequence> linies = f.split(text, 220);
        int w = 0;
        for (var l : linies) {
            w = Math.max(w, f.width(l));
        }
        int h = linies.size() * 10;
        int x = mx + 12;
        int y = my - 4;
        if (x + w + 8 > ampleP) {
            x = mx - w - 16;
        }
        if (y + h + 8 > altP) {
            y = altP - h - 8;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x - 4, y - 4, x + w + 4, y + h + 3, NEGRE);
        g.fill(x - 3, y - 3, x + w + 3, y + h + 2, OR);
        g.fillGradient(x - 2, y - 2, x + w + 2, y + h + 1, 0xF02A231C, 0xF0161210);
        for (int i = 0; i < linies.size(); i++) {
            g.drawString(f, linies.get(i), x, y + i * 10, TEXT, true);
        }
        g.pose().popPose();
    }

    private static int aclarir(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 40);
        int gg = Math.min(255, ((c >> 8) & 0xFF) + 40);
        int b = Math.min(255, (c & 0xFF) + 40);
        return (r << 16) | (gg << 8) | b;
    }

    private static int lluminositat(int c) {
        return (((c >> 16) & 0xFF) * 299 + ((c >> 8) & 0xFF) * 587 + (c & 0xFF) * 114) / 1000;
    }
}
