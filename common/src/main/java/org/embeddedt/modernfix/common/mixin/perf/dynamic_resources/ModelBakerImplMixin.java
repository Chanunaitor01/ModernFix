package org.embeddedt.modernfix.common.mixin.perf.dynamic_resources;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.modernfix.ModernFix;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelBakery.ModelBakerImpl.class)
@ClientOnlyMixin
public abstract class ModelBakerImplMixin {
    @Shadow public abstract UnbakedModel getModel(ResourceLocation location);

    @Unique
    private int mfix$getDepth = 0;

    /**
     * @author embeddedt
     * @reason force parent resolution to happen before model gets baked
     */
    @ModifyReturnValue(method = "getModel", at = @At("RETURN"))
    private UnbakedModel resolveParents(UnbakedModel model) {
        mfix$getDepth++;
        if(mfix$getDepth == 1) {
            try {
                model.resolveParents(this::getModel);
            } catch(Exception e) {
                ModernFix.LOGGER.warn("Exception encountered resolving parents", e);
            }
        }
        mfix$getDepth--;
        return model;
    }
}
