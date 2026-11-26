package cat.mineuab.pauitreva.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Un botó amb l'estil de la pantalla: fusta fosca amb vora d'or, que s'il·lumina en passar-hi el ratolí. Si és
 * una pestanya, porta una icona, i l'activa s'enganxa al contingut de sota.
 */
public class BotoPau extends Button {

    private final String icona;
    private final boolean pestanya;

    public BotoPau(int x, int y, int w, int h, Component text, OnPress accio) {
        this(x, y, w, h, text, "", false, accio);
    }

    public BotoPau(int x, int y, int w, int h, Component text, String icona, boolean pestanya, OnPress accio) {
        super(x, y, w, h, text, accio, DEFAULT_NARRATION);
        this.icona = icona;
        this.pestanya = pestanya;
    }

    /** Amplada que necessita un botó amb aquest text (i icona). */
    public static int amplada(Font f, Component text, boolean ambIcona) {
        return f.width(text) + (ambIcona ? 26 : 12);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean sobre = isHoveredOrFocused() && active;
        boolean triada = pestanya && !active;
        int vora = triada ? Estil.OR : !active ? Estil.OR_APAGAT : sobre ? Estil.OR_CLAR : Estil.OR_FOSC;
        g.fill(x, y, x + w, y + h, Estil.NEGRE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - (triada ? 0 : 1), vora);
        int dalt;
        int baix;
        if (triada) {
            dalt = 0xFF2A231C;
            baix = 0xFF241E18;
        } else if (!active) {
            dalt = 0xFF241F1A;
            baix = 0xFF1C1814;
        } else if (sobre) {
            dalt = 0xFF54452F;
            baix = 0xFF3A2F22;
        } else {
            dalt = 0xFF3E3327;
            baix = 0xFF29211A;
        }
        g.fillGradient(x + 2, y + 2, x + w - 2, y + h - (triada ? 0 : 2), dalt, baix);
        int tx = x + w / 2 - f.width(getMessage()) / 2;
        if (!icona.isEmpty()) {
            int total = 16 + 4 + f.width(getMessage());
            int ix = x + (w - total) / 2;
            Estil.icona(g, icona, ix, y + (h - 16) / 2);
            tx = ix + 20;
        }
        int color = triada ? Estil.OR_CLAR : active ? Estil.TEXT : Estil.TEXT_APAGAT;
        g.drawString(f, getMessage(), tx, y + (h - 8) / 2, color, true);
    }
}
