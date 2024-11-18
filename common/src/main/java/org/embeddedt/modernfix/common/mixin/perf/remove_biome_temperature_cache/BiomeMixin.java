package org.embeddedt.modernfix.common.mixin.perf.remove_biome_temperature_cache;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/* Idea from Lithium for 1.19.3 */
@Mixin(Biome.class)
public abstract class BiomeMixin {
    @Shadow protected abstract float getTemperatureNoCache(BlockPos pos);

    /**
     * @author 2No2Name
     * @reason Remove caching, it's not effective
     * @param pos
     * @return
     */
    @Overwrite
    public final float getTemperature(BlockPos pos) {
        return this.getTemperatureNoCache(pos);
    }
}
