package org.embeddedt.modernfix.forge.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.embeddedt.modernfix.ModernFixClient;
import org.embeddedt.modernfix.screen.ModernFixConfigScreen;

import java.util.ArrayList;
import java.util.List;

public class ModernFixClientForge {
    private static ModernFixClient commonMod;

    public ModernFixClientForge() {
        commonMod = new ModernFixClient();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::keyBindRegister);
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () ->  new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new ModernFixConfigScreen(screen))
        );
    }

    private KeyMapping configKey;

    private void keyBindRegister(RegisterKeyMappingsEvent event) {
        configKey = new KeyMapping("key.modernfix.config", KeyConflictContext.UNIVERSAL, InputConstants.UNKNOWN, "key.modernfix");
        event.register(configKey);
    }

    @SubscribeEvent
    public void onConfigKey(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START && configKey.consumeClick()) {
            Minecraft.getInstance().setScreen(new ModernFixConfigScreen(Minecraft.getInstance().screen));
        }
    }

    private static final List<String> brandingList = new ArrayList<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderOverlay(CustomizeGuiOverlayEvent.DebugText event) {
        if(commonMod.brandingString != null && Minecraft.getInstance().options.renderDebug) {
            if(brandingList.size() == 0) {
                brandingList.add("");
                brandingList.add(commonMod.brandingString);
            }
            int targetIdx = 0, numSeenBlanks = 0;
            List<String> right = event.getRight();
            while(targetIdx < right.size()) {
                String s = right.get(targetIdx);
                if(s == null || s.length() == 0) {
                    numSeenBlanks++;
                }
                if(numSeenBlanks == 3)
                    break;
                targetIdx++;
            }
            right.addAll(targetIdx, brandingList);
        }
    }

    @SubscribeEvent
    public void onDisconnect(LevelEvent.Unload event) {
        if(event.getLevel().isClientSide()) {
            DebugScreenOverlay overlay = ObfuscationReflectionHelper.getPrivateValue(ForgeGui.class, (ForgeGui)Minecraft.getInstance().gui, "debugOverlay");
            if(overlay != null) {
                Minecraft.getInstance().tell(overlay::clearChunkCache);
            }
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartedEvent event) {
        commonMod.onServerStarted(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if(event.phase == TickEvent.Phase.END)
            commonMod.onRenderTickEnd();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRecipes(RecipesUpdatedEvent e) {
        commonMod.onRecipesUpdated();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTags(TagsUpdatedEvent e) {
        commonMod.onTagsUpdated();
    }
}
