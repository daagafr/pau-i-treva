package cat.mineuab.pauitreva.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Una bandera amb marc que es pot clicar (a la galeria per triar la del país). Sense acció, només es mira. */
public class BotoBandera extends Button {

    private final String id;
    private final int marge;

    public BotoBandera(int x, int y, int w, int h, String id, int marge, OnPress accio) {
        super(x, y, w, h, Component.literal(id), accio, DEFAULT_NARRATION);
        this.id = id;
        this.marge = marge;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        boolean sobre = active && isHoveredOrFocused();
        Estil.bandera(g, id, getX() + marge, getY() + marge, getWidth() - 2 * marge, getHeight() - 2 * marge, sobre);
        if (sobre) {
            g.fill(getX() + marge, getY() + marge, getX() + getWidth() - marge, getY() + getHeight() - marge, 0x30FFFFFF);
        }
    }
}
