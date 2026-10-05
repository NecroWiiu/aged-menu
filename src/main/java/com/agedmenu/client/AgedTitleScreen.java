package com.agedmenu.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class AgedTitleScreen extends Screen {
    private static final Identifier BACKGROUND = new Identifier("agedmenu", "textures/gui/background.png");
    private static final int BG_W = 2214, BG_H = 1242;
    private static final long LEAVE_MS = 170L;

    private final List<MenuButton> animated = new ArrayList<>();
    private long openedAt = Util.getMeasuringTimeMs();
    private long lastFrame = 0L;
    private long leaveAt = -1L;
    private Runnable pending = null;
    private boolean resizing = false;
    private float px = 0f, py = 0f;
    private int menuX, titleY;

    public AgedTitleScreen() {
        super(Text.translatable("narrator.screen.title"));
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        resizing = true;
        super.resize(client, width, height);
        resizing = false;
    }

    @Override
    protected void init() {
        clearChildren();
        animated.clear();
        if (!resizing) {
            openedAt = Util.getMeasuringTimeMs();
            leaveAt = -1L;
            pending = null;
        }

        menuX = Math.max(24, (int) (width * 0.09f));
        final int bw = 150, bh = 20, gap = 4;
        int contentH = 62 + 14 + 5 * (bh + gap);
        titleY = Math.max(16, (height - contentH) / 2 - 8);
        int y = titleY + 62 + 14;

        boolean modMenu = FabricLoader.getInstance().isModLoaded("modmenu");

        MenuButton single = add(new MenuButton(Text.translatable("menu.singleplayer"), MenuButton.Style.LARGE,
                menuX, y, bw, bh, () -> go(() -> client.setScreen(new SelectWorldScreen(this)))));
        y += bh + gap;

        MenuButton multi = add(new MenuButton(Text.translatable("menu.multiplayer"), MenuButton.Style.LARGE,
                menuX, y, bw, bh, () -> go(this::openMultiplayer)));
        multi.active = client.isMultiplayerEnabled();
        y += bh + gap;

        MenuButton mods = add(new MenuButton(Text.translatableWithFallback("modmenu.title", "Mods"),
                MenuButton.Style.LARGE, menuX, y, bw, bh, () -> go(this::openMods)));
        if (!modMenu) {
            mods.active = false;
            mods.setTooltip(Tooltip.of(Text.translatable("agedmenu.modmenu_missing")));
        }
        y += bh + gap;

        add(new MenuButton(Text.translatable("menu.options"), MenuButton.Style.LARGE,
                menuX, y, bw, bh, () -> go(() -> client.setScreen(new OptionsScreen(this, client.options)))));
        y += bh + gap;

        add(new MenuButton(Text.translatable("menu.quit"), MenuButton.Style.LARGE,
                menuX, y, bw, bh, () -> go(client::scheduleStop)));

        // fila inferior: idioma + accesibilidad
        Text langT = Text.translatable("options.language").copy().append("...");
        Text accT = Text.translatable("options.accessibility.title").copy().append("...");
        int sy = height - 36, sx = menuX;
        int lw = textRenderer.getWidth(langT) + 12;
        add(new MenuButton(langT, MenuButton.Style.SMALL, sx, sy, lw, 14, () -> go(() ->
                client.setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager())))));
        sx += lw + 6;
        int aw = textRenderer.getWidth(accT) + 12;
        add(new MenuButton(accT, MenuButton.Style.SMALL, sx, sy, aw, 14, () -> go(() ->
                client.setScreen(new AccessibilityOptionsScreen(this, client.options)))));
    }

    private MenuButton add(MenuButton b) {
        animated.add(b);
        return addDrawableChild(b);
    }

    private void openMultiplayer() {
        Screen next = client.options.skipMultiplayerWarning
                ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this);
        client.setScreen(next);
    }

    /** Abre la pantalla de Mod Menu por reflexión (sin dependencia en compilación). */
    private void openMods() {
        try {
            Class<?> c = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            client.setScreen((Screen) c.getConstructor(Screen.class).newInstance(this));
        } catch (Throwable t) {
            leaveAt = -1L; // si falla, se queda en el menú
        }
    }

    /** Inicia el fundido de salida y ejecuta la acción cuando termina. */
    private void go(Runnable r) {
        if (pending != null) return;
        pending = r;
        leaveAt = Util.getMeasuringTimeMs();
    }

    private static float ease(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        float i = 1f - x;
        return 1f - i * i * i;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        float dt = lastFrame == 0L ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float t = now - openedAt;

        // parallax suave siguiendo el ratón
        float tx = (0.5f - mouseX / (float) Math.max(1, width)) * 14f;
        float ty = (0.5f - mouseY / (float) Math.max(1, height)) * 8f;
        float k = 1f - (float) Math.exp(-dt * 4f);
        px += (tx - px) * k;
        py += (ty - py) * k;

        drawBackground(ctx, now);
        drawScrim(ctx);
        drawTitle(ctx, t);

        // entrada escalonada de los botones
        for (int i = 0; i < animated.size(); i++) {
            MenuButton b = animated.get(i);
            float e = ease((t - 150f - i * 70f) / 450f);
            b.appear = e;
            b.setX(b.baseX - (int) ((1f - e) * 24f));
        }
        super.render(ctx, mouseX, mouseY, delta);
        drawFooter(ctx, t);

        // fundido de entrada / salida
        float veil = 1f - ease(t / 700f);
        if (leaveAt >= 0L) {
            float lp = MathHelper.clamp((now - leaveAt) / (float) LEAVE_MS, 0f, 1f);
            veil = Math.max(veil, lp * 0.65f);
        }
        int va = (int) (veil * 255f);
        if (va > 3) ctx.fill(0, 0, width, height, (va << 24) | 0x050A0E);

        if (pending != null && now - leaveAt >= LEAVE_MS) {
            Runnable r = pending;
            pending = null;
            r.run();
        }
    }

    private void drawBackground(DrawContext ctx, long now) {
        float scale = Math.max(width / (float) BG_W, height / (float) BG_H) * 1.07f;
        float drift = (float) Math.sin(now / 5200.0) * 4f;
        float x = (width - BG_W * scale) / 2f + px + drift;
        float y = (height - BG_H * scale) / 2f + py;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawTexture(BACKGROUND, 0, 0, BG_W, BG_H, 0f, 0f, BG_W, BG_H, BG_W, BG_H);
        ctx.getMatrices().pop();
    }

    private void drawScrim(DrawContext ctx) {
        int strips = 48;
        int total = (int) (width * 0.55f);
        int sw = Math.max(1, (int) Math.ceil(total / (double) strips));
        for (int i = 0; i < strips; i++) {
            float f = 1f - i / (float) strips;
            int a = (int) (0xA0 * f * f);
            if (a > 0) ctx.fill(i * sw, 0, (i + 1) * sw, height, (a << 24) | 0x07131A);
        }
        ctx.fillGradient(0, height - 70, width, height, 0x00000000, 0x99000000);
    }

    private void drawTitle(DrawContext ctx, float t) {
        float a = ease(t / 600f);
        if (a <= 0.02f) return;
        int color = MenuButton.argb(a, 0xFFFFFF);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(menuX + 2, titleY - (1f - a) * 6f, 0);
        ctx.getMatrices().scale(5f, 5f, 1f);
        ctx.drawText(textRenderer, "Aged", 0, 0, color, true);
        ctx.getMatrices().pop();
        int lineW = (int) (46 * ease((t - 200f) / 500f));
        if (lineW > 0) ctx.fill(menuX + 3, titleY + 46, menuX + 3 + lineW, titleY + 48, MenuButton.argb(a, MenuButton.ACCENT));
        int sub = MenuButton.argb(ease((t - 250f) / 500f) * 0.85f, 0xB9CBD3);
        if ((sub >>> 24) > 3) {
            ctx.drawText(textRenderer, Text.literal("Minecraft ").append(Text.translatable("agedmenu.edition")),
                    menuX + 3, titleY + 54, sub, true);
        }
    }

    private void drawFooter(DrawContext ctx, float t) {
        float a = ease((t - 500f) / 500f) * 0.7f;
        int col = MenuButton.argb(a, 0xD5E0E6);
        if ((col >>> 24) <= 3) return;
        int mods = FabricLoader.getInstance().getAllMods().size();
        String ver = "Minecraft " + SharedConstants.getGameVersion().getName() + " (Fabric)";
        ctx.drawText(textRenderer, Text.literal(ver + "  \u00B7  ").append(Text.translatable("agedmenu.mods_loaded", mods)),
                menuX, height - 16, col, true);
        Text c = Text.literal("Copyright Mojang AB. Do not distribute!");
        ctx.drawText(textRenderer, c, width - textRenderer.getWidth(c) - 8, height - 16, col, true);
    }
}
