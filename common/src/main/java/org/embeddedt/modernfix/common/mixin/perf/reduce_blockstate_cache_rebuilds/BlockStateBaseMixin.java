package org.embeddedt.modernfix.common.mixin.perf.reduce_blockstate_cache_rebuilds;

import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.modernfix.duck.IBlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(BlockState.class)
public abstract class BlockStateBaseMixin implements IBlockState {
    @Shadow public abstract void initCache();

    @Shadow private BlockState.Cache cache;

    private volatile boolean cacheInvalid = false;
    private static boolean buildingCache = false;
    @Override
    public void clearCache() {
        cacheInvalid = true;
    }

    @Override
    public boolean isCacheInvalid() {
        return cacheInvalid;
    }

    private void mfix$generateCache() {
        if(cacheInvalid) {
            // Ensure that only one block's cache is built at a time
            synchronized (BlockState.class) {
                if(cacheInvalid) {
                    // Ensure that if we end up in here recursively, we just use the original cache
                    if(!buildingCache) {
                        buildingCache = true;
                        try {
                            this.initCache();
                            cacheInvalid = false;
                        } finally {
                            buildingCache = false;
                        }
                    }
                }

            }
        }
    }

    @Redirect(method = "*", at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/world/level/block/state/BlockState;cache:Lnet/minecraft/world/level/block/state/BlockState$Cache;",
            ordinal = 0
    ))
    private BlockState.Cache dynamicCacheGen(BlockState base) {
        mfix$generateCache();
        return this.cache;
    }
}
