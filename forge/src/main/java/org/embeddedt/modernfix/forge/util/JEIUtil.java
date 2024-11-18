package org.embeddedt.modernfix.forge.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Supplier;

public class JEIUtil {
    private static Supplier<Boolean> isLoading = null;

    public static void registerLoadingRenderer(Supplier<Boolean> isLoading) {
        JEIUtil.isLoading = isLoading;
        MinecraftForge.EVENT_BUS.register(JEIUtil.class);
    }

    @SubscribeEvent
    public static void renderLoad(GuiScreenEvent.DrawScreenEvent.Post event) {
        /* Don't show the JEI indicator on the level loading screen, that looks weird */
        if(isLoading.get()) {
            Minecraft.getInstance().font.drawShadow(new TranslatableComponent("modernfix.jei_load").getString(), 0, 0, 0xffffff);
        }
    }
}
