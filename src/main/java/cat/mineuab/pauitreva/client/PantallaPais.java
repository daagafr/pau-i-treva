package cat.mineuab.pauitreva.client;

import cat.mineuab.pauitreva.pantalla.Pantalla;
import cat.mineuab.pauitreva.pantalla.Pantalla.Boto;
import cat.mineuab.pauitreva.pantalla.Pantalla.Entrada;
import cat.mineuab.pauitreva.pantalla.Pantalla.Fila;
import cat.mineuab.pauitreva.pantalla.Pantalla.Peca;
import cat.mineuab.pauitreva.pantalla.Pantalla.Pestanya;
import cat.mineuab.pauitreva.pantalla.Pantalla.Text;
import cat.mineuab.pauitreva.pantalla.Xarxa;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * La pantalla del país (tecla N). No sap res de països: pinta el que li envia el servidor ({@link Pantalla}) i,
 * quan es fa clic a un botó, li torna l'acció amb el que hi hagi escrit als camps.
 */
public final class PantallaPais extends Screen {

    private static final int AMPLE_MAX = 460;
    private static final int LINIA = 10;
    private static final int ALT_FILA = 20;

    /** Si s'ha demanat obrir la pantalla (amb la tecla) i encara no ha arribat. */
    static boolean esperant;

    private Pantalla dades;
    private int scroll;
    private int alturaContingut;
    private int x0;
    private int ample;
    private int dalt;
    private int baix;
    private final List<Linia> textos = new ArrayList<>();
    private final Map<String, EditBox> entrades = new HashMap<>();
    /** El que l'usuari ha escrit, perquè no es perdi en fer scroll. Es buida quan arriben dades noves. */
    private final Map<String, String> escrit = new HashMap<>();

    private record Linia(FormattedCharSequence text, int x, int y) {
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

    @Override
    protected void init() {
        textos.clear();
        entrades.clear();
        ample = Math.min(width - 20, AMPLE_MAX);
        x0 = (width - ample) / 2;

        int tx = x0;
        int ty = 20;
        for (Pestanya t : dades.pestanyes()) {
            int w = font.width(t.nom()) + 14;
            if (tx + w > x0 + ample) {
                tx = x0;
                ty += 20;
            }
            Button b = Button.builder(t.nom(), x -> enviar("pestanya:" + t.id())).bounds(tx, ty, w, 18).build();
            b.active = !t.id().equals(dades.activa());
            addRenderableWidget(b);
            tx += w + 2;
        }
        dalt = ty + 24;
        baix = height - 10;

        int y = dalt - scroll;
        for (Component avis : dades.avisos()) {
            for (FormattedCharSequence l : font.split(avis, ample - 8)) {
                textos.add(new Linia(l, x0 + 4, y + 3));
                y += LINIA;
            }
        }
        if (!dades.avisos().isEmpty()) {
            y += 10;
        }
        int iniciFiles = y;
        for (Fila f : dades.files()) {
            if (f.peces().stream().allMatch(p -> p instanceof Text)) {
                // Una fila només de text: s'ajunta en un sol text i s'ajusta a l'amplada.
                net.minecraft.network.chat.MutableComponent tot = Component.empty();
                f.peces().forEach(p -> tot.append(((Text) p).text()));
                List<FormattedCharSequence> linies = font.split(tot, ample);
                for (FormattedCharSequence l : linies) {
                    textos.add(new Linia(l, x0, y));
                    y += LINIA;
                }
                y += linies.isEmpty() ? LINIA : 2;
                continue;
            }
            int px = x0;
            for (Peca peca : f.peces()) {
                int w = amplada(peca);
                if (px > x0 && px + w > x0 + ample) {
                    px = x0;
                    y += ALT_FILA;
                }
                switch (peca) {
                    case Text t -> textos.add(new Linia(t.text().getVisualOrderText(), px, y + 5));
                    case Boto b -> {
                        Button boto = Button.builder(b.text(), x -> clicar(b.accio())).bounds(px, y, w, 18)
                                .tooltip(b.ajuda().getString().isEmpty() ? null : Tooltip.create(b.ajuda())).build();
                        boto.active = !b.accio().isEmpty();
                        afegir(boto, y);
                    }
                    case Entrada e -> {
                        EditBox camp = new EditBox(font, px + 1, y + 1, w - 2, 16, Component.literal(e.id()));
                        camp.setMaxLength(Math.max(1, e.max()));
                        camp.setValue(escrit.getOrDefault(e.id(), e.valor()));
                        camp.setResponder(v -> escrit.put(e.id(), v));
                        if (!e.ajuda().getString().isEmpty()) {
                            camp.setTooltip(Tooltip.create(e.ajuda()));
                        }
                        entrades.put(e.id(), camp);
                        afegir(camp, y);
                    }
                }
                px += w + 3;
            }
            y += ALT_FILA + 2;
        }
        alturaContingut = y + scroll - dalt;
        if (dades.avisos().isEmpty() && iniciFiles == dalt - scroll) {
            alturaContingut = Math.max(alturaContingut, 0);
        }
    }

    /** Afegeix un botó o un camp, i l'amaga si queda fora de la zona visible. */
    private void afegir(AbstractWidget w, int y) {
        w.visible = y >= dalt && y + 18 <= baix;
        addRenderableWidget(w);
    }

    private int amplada(Peca peca) {
        return switch (peca) {
            case Text t -> font.width(t.text());
            case Boto b -> Math.max(20, font.width(b.text()) + 10);
            case Entrada e -> Math.max(24, e.ample());
        };
    }

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

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, dades.titol(), width / 2, 6, 0xFFFFFF);
        g.enableScissor(x0 - 2, dalt, x0 + ample + 2, baix);
        if (!dades.avisos().isEmpty()) {
            int alt = 0;
            for (Component avis : dades.avisos()) {
                alt += font.split(avis, ample - 8).size() * LINIA;
            }
            g.fill(x0, dalt - scroll, x0 + ample, dalt - scroll + alt + 6, 0x80000000);
        }
        for (Linia l : textos) {
            if (l.y() + LINIA >= dalt && l.y() <= baix) {
                g.drawString(font, l.text(), l.x(), l.y(), 0xFFFFFF, true);
            }
        }
        g.disableScissor();
        int visible = baix - dalt;
        if (alturaContingut > visible) {
            int barra = Math.max(12, visible * visible / alturaContingut);
            int pos = dalt + (visible - barra) * scroll / Math.max(1, alturaContingut - visible);
            g.fill(x0 + ample + 4, pos, x0 + ample + 6, pos + barra, 0xFFAAAAAA);
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
