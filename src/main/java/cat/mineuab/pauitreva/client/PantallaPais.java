package cat.mineuab.pauitreva.client;

import cat.mineuab.pauitreva.pantalla.Pantalla;
import cat.mineuab.pauitreva.pantalla.Pantalla.Barra;
import cat.mineuab.pauitreva.pantalla.Pantalla.Boto;
import cat.mineuab.pauitreva.pantalla.Pantalla.Entrada;
import cat.mineuab.pauitreva.pantalla.Pantalla.Escut;
import cat.mineuab.pauitreva.pantalla.Pantalla.Fila;
import cat.mineuab.pauitreva.pantalla.Pantalla.Icona;
import cat.mineuab.pauitreva.pantalla.Pantalla.Peca;
import cat.mineuab.pauitreva.pantalla.Pantalla.Pestanya;
import cat.mineuab.pauitreva.pantalla.Pantalla.Seccio;
import cat.mineuab.pauitreva.pantalla.Pantalla.Text;
import cat.mineuab.pauitreva.pantalla.Xarxa;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * La pantalla del país (tecla N). No sap res de països: dibuixa el que li envia el servidor ({@link Pantalla}) amb
 * l'estil de {@link Estil} i, quan es fa clic a un botó, li torna l'acció amb el que hi hagi escrit als camps.
 *
 * <p>Disposició: un marc gran; a dalt, l'escut i el nom del país amb els xips; sota, les pestanyes; i el contingut,
 * que es pot desplaçar, fet de seccions amb marc.
 */
public final class PantallaPais extends Screen {

    private static final int AMPLE_MAX = 560;
    private static final int LINIA = 10;
    private static final int ALT_FILA = 18;
    private static final int ALT_CAPCALERA = 50;

    /** Si s'ha demanat obrir la pantalla (amb la tecla) i encara no ha arribat. */
    static boolean esperant;

    private Pantalla dades;
    private int scroll;
    private int alturaContingut;
    private int x0;
    private int y0;
    private int ample;
    private int alt;
    private int cx;
    private int cw;
    private int dalt;
    private int baix;
    private final List<Dibuix> caixes = new ArrayList<>();
    private final List<Dibuix> fons = new ArrayList<>();
    private final List<Dibuix> dibuixos = new ArrayList<>();
    private final List<Linia> textos = new ArrayList<>();
    private final List<Ajuda> ajudes = new ArrayList<>();
    private final Map<String, EditBox> entrades = new HashMap<>();
    /** El que l'usuari ha escrit, perquè no es perdi en fer scroll. Es buida quan arriben dades noves. */
    private final Map<String, String> escrit = new HashMap<>();

    /** Una cosa que es dibuixa al contingut (ja desplaçada). */
    private interface Dibuix {
        void dibuixar(GuiGraphics g);
    }

    private record Linia(FormattedCharSequence text, int x, int y, int color) {
    }

    /** Una zona on, en passar-hi el ratolí, surt un text d'ajuda. {@code contingut}: retallada a la zona del contingut. */
    private record Ajuda(int x, int y, int w, int h, Component text, boolean contingut) {
    }

    private PantallaPais(Pantalla dades) {
        super(Component.literal("Pau i Treva"));
        this.dades = dades;
    }

    /** Ha arribat una pantalla del servidor: s'actualitza la que hi ha o, si s'havia demanat, s'obre. */
    static void rebre(Pantalla p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PantallaPais oberta) {
            boolean mateixa = oberta.dades.activa().equals(p.activa());
            oberta.dades = p;
            oberta.escrit.clear();
            if (!mateixa) {
                oberta.scroll = 0;
            }
            oberta.rebuildWidgets();
        } else if (esperant) {
            esperant = false;
            mc.setScreen(new PantallaPais(p));
        }
    }

    // ---------------------------------------------------------------- Disposició

    @Override
    protected void init() {
        caixes.clear();
        fons.clear();
        dibuixos.clear();
        textos.clear();
        ajudes.clear();
        entrades.clear();
        ample = Math.min(width - 12, AMPLE_MAX);
        alt = height - 10;
        x0 = (width - ample) / 2;
        y0 = 5;

        ajudesCapcalera();

        // Pestanyes, enganxades a la part de dalt del contingut.
        int tx = x0 + 10;
        int ty = y0 + ALT_CAPCALERA + 4;
        for (Pestanya t : dades.pestanyes()) {
            int w = BotoPau.amplada(font, t.nom(), !t.icona().isEmpty());
            if (tx + w > x0 + ample - 10) {
                break;
            }
            BotoPau b = new BotoPau(tx, ty, w, 20, t.nom(), t.icona(), true, x -> enviar("pestanya:" + t.id()));
            b.active = !t.id().equals(dades.activa());
            addRenderableWidget(b);
            tx += w + 2;
        }

        int px = x0 + 8;
        int py = ty + 20;
        cx = px + 8;
        cw = ample - 16 - 16 - 6;
        dalt = py + 6;
        baix = y0 + alt - 14;

        int y = dalt - scroll;
        if (!dades.avisos().isEmpty()) {
            int inici = y;
            y += 5;
            for (Component avis : dades.avisos()) {
                for (FormattedCharSequence l : font.split(avis, cw - 12)) {
                    textos.add(new Linia(l, cx + 8, y, Estil.TEXT));
                    y += LINIA;
                }
            }
            y += 4;
            int iy = inici;
            int fy = y;
            fons.add(g -> {
                g.fill(cx, iy, cx + cw, fy, 0xFF3A2A16);
                g.fill(cx, iy, cx + 3, fy, Estil.OR);
                g.fill(cx, fy - 1, cx + cw, fy, Estil.OR_FOSC);
            });
            y += 6;
        }

        int iniciSeccio = Integer.MIN_VALUE;
        for (Fila f : dades.files()) {
            if (f.peces().size() == 1 && f.peces().get(0) instanceof Seccio s) {
                tancarSeccio(iniciSeccio, y);
                if (iniciSeccio != Integer.MIN_VALUE) {
                    y += 8;
                }
                iniciSeccio = y;
                int sy = y + 4;
                dibuixos.add(g -> Estil.titolSeccio(g, font, cx + 2, sy, cw - 4, s.titol(), s.icona()));
                y += 22;
                continue;
            }
            int marge = iniciSeccio == Integer.MIN_VALUE ? 0 : 8;
            int ex = cx + marge;
            int ew = cw - 2 * marge;
            if (f.peces().stream().allMatch(p -> p instanceof Text)) {
                MutableComponent tot = Component.empty();
                f.peces().forEach(p -> tot.append(((Text) p).text()));
                List<FormattedCharSequence> linies = font.split(tot, ew);
                for (FormattedCharSequence l : linies) {
                    textos.add(new Linia(l, ex, y, Estil.TEXT));
                    y += LINIA;
                }
                y += linies.isEmpty() ? LINIA : 3;
                continue;
            }
            int fx = ex;
            int altLinia = ALT_FILA;
            for (Peca peca : f.peces()) {
                int w = amplada(peca);
                if (fx > ex && fx + w > ex + ew) {
                    fx = ex;
                    y += altLinia + 2;
                    altLinia = ALT_FILA;
                }
                posar(peca, fx, y, w);
                altLinia = Math.max(altLinia, alcada(peca));
                fx += w + 4;
            }
            y += altLinia + 4;
        }
        tancarSeccio(iniciSeccio, y);
        alturaContingut = y + 6 + scroll - dalt;
    }

    private void tancarSeccio(int inici, int fi) {
        if (inici == Integer.MIN_VALUE) {
            return;
        }
        int x = cx;
        int w = cw;
        caixes.add(g -> Estil.caixa(g, x, inici, w, fi - inici + 2));
    }

    /** Les ajudes de la capçalera (l'escut i els xips), que no es desplacen. */
    private void ajudesCapcalera() {
        Pantalla.Capcalera c = dades.capcalera();
        int ex = x0 + 14;
        int ey = y0 + 9;
        int xx = nomX();
        int xy = y0 + 34;
        for (Peca p : c.xips()) {
            if (p instanceof Icona i) {
                int w = 16 + 3 + font.width(i.text());
                if (xx + w > x0 + ample - 12) {
                    break;
                }
                if (!i.ajuda().getString().isEmpty()) {
                    ajudes.add(new Ajuda(xx, xy, w, 16, i.ajuda(), false));
                }
                xx += w + 10;
            }
        }
        ajudes.add(new Ajuda(ex, ey, ambBandera() ? 54 : 30, 36, c.sigla().isEmpty() ? Component.literal("Pau i Treva")
                : Component.literal("[" + c.sigla() + "] ").append(c.nom()), false));
    }

    /** Si la capçalera porta la bandera del país (en lloc de l'escut de color). */
    private boolean ambBandera() {
        return BanderesClient.textura(dades.capcalera().bandera()) != null;
    }

    /** On comença el nom del país a la capçalera. */
    private int nomX() {
        return x0 + 14 + (ambBandera() ? 60 : 40);
    }

    private void dibuixarCapcalera(GuiGraphics g) {
        Pantalla.Capcalera c = dades.capcalera();
        int ex = x0 + 14;
        int ey = y0 + 9;
        if (ambBandera()) {
            Estil.bandera(g, c.bandera(), ex + 3, ey + 3, 48, 32, false);
        } else {
            Estil.escut(g, font, ex, ey, 30, 36, c.color(), c.sigla());
        }
        int nx = nomX();
        g.pose().pushPose();
        g.pose().translate(nx, y0 + 10, 0);
        g.pose().scale(1.6f, 1.6f, 1f);
        g.drawString(font, c.nom(), 0, 0, Estil.OR_CLAR, true);
        g.pose().popPose();
        int sx = nx + Math.round(font.width(c.nom()) * 1.6f) + 10;
        if (sx + font.width(c.subtitol()) < x0 + ample - 14) {
            g.drawString(font, c.subtitol(), sx, y0 + 14, Estil.TEXT_GRIS, true);
        }
        int xx = nx;
        int xy = y0 + 34;
        for (Peca p : c.xips()) {
            if (p instanceof Icona i) {
                int w = 16 + 3 + font.width(i.text());
                if (xx + w > x0 + ample - 12) {
                    break;
                }
                Estil.icona(g, i.icona(), xx, xy);
                g.drawString(font, i.text(), xx + 19, xy + 4, Estil.TEXT, true);
                xx += w + 10;
            }
        }
        // Línia d'or sota la capçalera.
        g.fill(x0 + 8, y0 + ALT_CAPCALERA + 2, x0 + ample - 8, y0 + ALT_CAPCALERA + 3, Estil.OR_FOSC);
    }

    /** Quanta alçada ocupa una peça (les banderes poden ser més altes que una fila normal). */
    private int alcada(Peca peca) {
        return peca instanceof Pantalla.Bandera b ? b.alt() + (b.ample() >= 30 ? 8 : 2) : ALT_FILA;
    }

    private int amplada(Peca peca) {
        return switch (peca) {
            case Text t -> font.width(t.text());
            case Boto b -> Math.max(20, BotoPau.amplada(font, b.text(), false));
            case Entrada e -> Math.max(24, e.ample());
            case Icona i -> 16 + 3 + font.width(i.text());
            case Barra b -> Math.max(30, b.ample());
            case Escut e -> BanderesClient.textura(e.bandera()) != null ? 16 : 12;
            case Pantalla.Bandera b -> b.ample() + (b.ample() >= 30 ? 8 : 2);
            case Seccio s -> font.width(s.titol());
        };
    }

    /** Posa una peça d'una fila a {@code (x, y)}: un giny, o una cosa per dibuixar. */
    private void posar(Peca peca, int x, int y, int w) {
        switch (peca) {
            case Text t -> textos.add(new Linia(t.text().getVisualOrderText(), x, y + 5, Estil.TEXT));
            case Boto b -> {
                BotoPau boto = new BotoPau(x, y, w, ALT_FILA, b.text(), z -> clicar(b.accio()));
                boto.active = !b.accio().isEmpty();
                if (!b.ajuda().getString().isEmpty()) {
                    ajudes.add(new Ajuda(x, y, w, ALT_FILA, b.ajuda(), true));
                }
                afegir(boto, y);
            }
            case Entrada e -> {
                fons.add(g -> Estil.inserit(g, x, y, w, ALT_FILA));
                EditBox camp = new EditBox(font, x + 5, y + 5, w - 8, 10, Component.literal(e.id()));
                camp.setBordered(false);
                camp.setTextColor(Estil.TEXT);
                camp.setMaxLength(Math.max(1, e.max()));
                camp.setValue(escrit.getOrDefault(e.id(), e.valor()));
                camp.setResponder(v -> escrit.put(e.id(), v));
                if (!e.ajuda().getString().isEmpty()) {
                    ajudes.add(new Ajuda(x, y, w, ALT_FILA, e.ajuda(), true));
                }
                entrades.put(e.id(), camp);
                afegir(camp, y);
            }
            case Icona i -> {
                dibuixos.add(g -> {
                    Estil.icona(g, i.icona(), x, y + 1);
                    g.drawString(font, i.text(), x + 19, y + 5, Estil.TEXT, true);
                });
                if (!i.ajuda().getString().isEmpty()) {
                    ajudes.add(new Ajuda(x, y, w, ALT_FILA, i.ajuda(), true));
                }
            }
            case Barra b -> {
                dibuixos.add(g -> Estil.barra(g, font, x, y + 2, w, ALT_FILA - 4, b.valor(), b.colorEsquerra(),
                        b.colorDreta(), b.text()));
                if (!b.ajuda().getString().isEmpty()) {
                    ajudes.add(new Ajuda(x, y, w, ALT_FILA, b.ajuda(), true));
                }
            }
            case Escut e -> {
                if (BanderesClient.textura(e.bandera()) != null) {
                    dibuixos.add(g -> Estil.bandera(g, e.bandera(), x + 1, y + 4, 15, 10, false));
                } else {
                    dibuixos.add(g -> Estil.escut(g, font, x, y + 2, 12, 14, e.color(), e.sigla()));
                }
                if (!e.ajuda().getString().isEmpty()) {
                    ajudes.add(new Ajuda(x, y, w, ALT_FILA, e.ajuda(), true));
                }
            }
            case Pantalla.Bandera b -> {
                int marge = b.ample() >= 30 ? 4 : 1;
                BotoBandera boto = new BotoBandera(x, y, b.ample() + 2 * marge, b.alt() + 2 * marge, b.id(), marge,
                        z -> clicar(b.accio()));
                boto.active = !b.accio().isEmpty();
                if (!b.ajuda().getString().isEmpty()) {
                    ajudes.add(new Ajuda(x, y, b.ample() + 2 * marge, b.alt() + 2 * marge, b.ajuda(), true));
                }
                afegir(boto, y, b.alt() + 2 * marge);
            }
            case Seccio s -> textos.add(new Linia(s.titol().getVisualOrderText(), x, y + 5, Estil.OR_CLAR));
        }
    }

    /** Afegeix un botó o un camp, i l'amaga si queda fora de la zona visible. */
    private void afegir(AbstractWidget w, int y) {
        afegir(w, y, ALT_FILA);
    }

    private void afegir(AbstractWidget w, int y, int alt) {
        w.visible = y >= dalt && y + alt <= baix;
        addRenderableWidget(w);
    }

    // ---------------------------------------------------------------- Accions

    private void clicar(String accio) {
        if (accio.isEmpty()) {
            return;
        }
        String a = accio;
        for (Map.Entry<String, EditBox> e : entrades.entrySet()) {
            a = a.replace("{" + e.getKey() + "}", e.getValue().getValue().trim());
        }
        enviar(a.trim());
    }

    private static void enviar(String accio) {
        PacketDistributor.sendToServer(new Xarxa.Accio(accio));
    }

    // ---------------------------------------------------------------- Dibuix

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        Estil.marc(g, x0, y0, ample, alt);
        dibuixarCapcalera(g);
        int px = x0 + 8;
        int py = y0 + ALT_CAPCALERA + 24;
        Estil.inserit(g, px, py, ample - 16, y0 + alt - 8 - py);
        g.enableScissor(px + 1, dalt - 4, px + ample - 17, baix + 2);
        for (Dibuix d : caixes) {
            d.dibuixar(g);
        }
        for (Dibuix d : fons) {
            d.dibuixar(g);
        }
        for (Dibuix d : dibuixos) {
            d.dibuixar(g);
        }
        for (Linia l : textos) {
            if (l.y() + LINIA >= dalt - 4 && l.y() <= baix) {
                g.drawString(font, l.text(), l.x(), l.y(), l.color(), true);
            }
        }
        g.disableScissor();
        int visible = baix - dalt;
        if (alturaContingut > visible) {
            int bx = x0 + ample - 20;
            g.fill(bx, dalt, bx + 4, baix, Estil.NEGRE);
            int barra = Math.max(14, visible * visible / alturaContingut);
            int pos = dalt + (visible - barra) * scroll / Math.max(1, alturaContingut - visible);
            g.fill(bx, pos, bx + 4, pos + barra, Estil.OR_FOSC);
            g.fill(bx + 1, pos + 1, bx + 3, pos + barra - 1, Estil.OR);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        for (Ajuda a : ajudes) {
            if (a.contingut() && (my < dalt || my > baix)) {
                continue;
            }
            if (mx >= a.x() && mx < a.x() + a.w() && my >= a.y() && my < a.y() + a.h()) {
                Estil.ajuda(g, font, a.text(), mx, my, width, height);
                return;
            }
        }
        Style estil = estilA(mx, my);
        if (estil != null && estil.getHoverEvent() != null) {
            g.renderComponentHoverEffect(font, estil, mx, my);
        }
    }

    /** L'estil del text que hi ha sota el ratolí (per ensenyar el text d'ajuda dels noms dels països). */
    private Style estilA(int mx, int my) {
        if (my < dalt || my > baix) {
            return null;
        }
        for (Linia l : textos) {
            if (my >= l.y() && my < l.y() + LINIA - 1 && mx >= l.x()) {
                return font.getSplitter().componentStyleAtWidth(l.text(), mx - l.x());
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int max = Math.max(0, alturaContingut - (baix - dalt));
        int nou = Math.max(0, Math.min(max, scroll - (int) (sy * 16)));
        if (nou != scroll) {
            scroll = nou;
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int tecla, int codi, int mods) {
        boolean escrivint = entrades.values().stream().anyMatch(EditBox::isFocused);
        if (!escrivint && ClientPauITreva.TECLA.matches(tecla, codi)) {
            onClose();
            return true;
        }
        return super.keyPressed(tecla, codi, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
