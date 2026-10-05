package com.agedmenu.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/** Botón minimalista: solo texto, con barra de acento y panel que aparecen suavemente al pasar el ratón. */
public class MenuButton extends ClickableWidget {
    public enum Style { LARGE, SMALL }

    static final int ACCENT = 0xF2B8A8;
    private static final int TEXT_IDLE = 0xCFDCE3;
    private static final int TEXT_HOVER = 0xFFFFFF;
    private static final int TEXT_DISABLED = 0x7A858C;

    private final Style style;
    private final Runnable action;
    int baseX;
    float appear = 1f;
    private float hover = 0f;
    private long lastMs = 0L;

    public MenuButton(Text label, Style style, int x, int y, int w, int h, Runnable action) {
        super(x, y, w, h, label);
        this.baseX = x;
        this.style = style;
        this.action = action;
    }

    @Override
    protected void renderButton(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        float dt = lastMs == 0L ? 0.016f : Math.min(0.1f, (now - lastMs) / 1000f);
        lastMs = now;

        float target = (isHovered() || isFocused()) && active ? 1f : 0f;
        hover += (target - hover) * (1f - (float) Math.exp(-dt * 16f));
        float h = hover * hover * (3f - 2f * hover);
        float a = appear;
        if (a <= 0.01f) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int x = getX(), y = getY(), w = getWidth(), ht = getHeight();
        int textColor = active ? lerpColor(TEXT_IDLE, TEXT_HOVER, h) : TEXT_DISABLED;

        if (style == Style.LARGE) {
            // panel translúcido
            int panelA = (int) (0x5A * h * a);
            if (panelA > 3) ctx.fill(x, y, x + w, y + ht, (panelA << 24) | 0x08141B);
            // barra de acento que crece desde el centro
            int barH = (int) (ht * h);
            if (barH > 0) {
                int by = y + (ht - barH) / 2;
                ctx.fill(x, by, x + 2, by + barH, argb(a, ACCENT));
            }
            int tx = x + 10 + (int) (5 * h);
            int ty = y + (ht - 8) / 2;
            drawText(ctx, mc, getMessage(), tx, ty, argb(a, textColor));
        } else {
            int tw = mc.textRenderer.getWidth(getMessage());
            int tx = x + (w - tw) / 2;
            int ty = y + (ht - 8) / 2 - 1;
            drawText(ctx, mc, getMessage(), tx, ty, argb(a, textColor));
            int ul = (int) (tw * h);
            if (ul > 0) ctx.fill(tx, ty + 10, tx + ul, ty + 11, argb(a, ACCENT));
        }
    }

    private static void drawText(DrawContext ctx, MinecraftClient mc, Text t, int x, int y, int color) {
        if ((color >>> 24) > 3) ctx.drawText(mc.textRenderer, t, x, y, color, true);
    }

    static int argb(float alpha, int rgb) {
        int a = MathHelper.clamp((int) (alpha * 255f), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    static int lerpColor(int c1, int c2, float t) {
        int r = (int) MathHelper.lerp(t, (c1 >> 16) & 255, (c2 >> 16) & 255);
        int g = (int) MathHelper.lerp(t, (c1 >> 8) & 255, (c2 >> 8) & 255);
        int b = (int) MathHelper.lerp(t, c1 & 255, c2 & 255);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        action.run();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, getMessage());
    }
}
