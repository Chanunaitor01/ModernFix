package org.embeddedt.modernfix.common.mixin.perf.cache_blockstate_cache_arrays;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockState.Cache.class)
public class AbstractBlockStateCacheMixin {
    private static final Direction.Axis[] DIRECTION_AXIS_VALUES = Direction.Axis.values();

    @Redirect(method = "<init>(Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction$Axis;values()[Lnet/minecraft/core/Direction$Axis;"))
    private Direction.Axis[] getDirectionAxisValues() {
        return DIRECTION_AXIS_VALUES;
    }
}
