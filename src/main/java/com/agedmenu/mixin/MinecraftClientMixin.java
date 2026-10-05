package com.agedmenu.mixin;

import com.agedmenu.client.AgedTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    /** Sustituye el TitleScreen vanilla (al iniciar y al salir de un mundo) por el nuestro. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen agedmenu$replaceTitle(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class) {
            return new AgedTitleScreen();
        }
        return screen;
    }
}
