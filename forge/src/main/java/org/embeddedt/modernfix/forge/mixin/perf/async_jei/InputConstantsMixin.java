package org.embeddedt.modernfix.forge.mixin.perf.async_jei;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.embeddedt.modernfix.annotation.RequiresMod;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InputConstants.class)
@ClientOnlyMixin
@RequiresMod("jei")
public class InputConstantsMixin {
    @Redirect(method = "isKeyDown", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwGetKey(JI)I", remap = false))
    private static int offThreadKeyFetch(long win, int k) {
        if(RenderSystem.isOnRenderThreadOrInit())
            return GLFW.glfwGetKey(win, k);
        else
            return 0;
    }
}
