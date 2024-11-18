package org.embeddedt.modernfix.common.mixin.perf.dynamic_structure_manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(StructureManager.class)
public class StructureManagerMixin {
    @Shadow @Final @Mutable
    private Map<ResourceLocation, StructureTemplate> structureRepository;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void makeStructuresSafe(CallbackInfo ci) {
        /* Structures needing to be reloaded is not a huge issue since we optimize loading them already */
        Cache<ResourceLocation, StructureTemplate> structureCache = CacheBuilder.newBuilder()
                .softValues()
                .build();
        this.structureRepository = structureCache.asMap();
    }
}
