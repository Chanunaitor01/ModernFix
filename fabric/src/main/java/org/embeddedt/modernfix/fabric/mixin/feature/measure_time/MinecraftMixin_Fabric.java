package org.embeddedt.modernfix.fabric.mixin.feature.measure_time;

import net.minecraft.client.Minecraft;
import org.embeddedt.modernfix.ModernFixClient;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
@ClientOnlyMixin
public class MinecraftMixin_Fabric {
    @Inject(method = "selectLevel", at = @At("HEAD"))
    private void recordWorldLoadStart(CallbackInfo ci) {
        ModernFixClient.worldLoadStartTime = System.nanoTime();
    }
}
