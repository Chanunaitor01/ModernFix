package org.embeddedt.modernfix.common.mixin.perf.deduplicate_wall_shapes;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Most wall blocks use the default set of vanilla properties, and the default sizes for their shapes. This means
 * there is no need to reconstruct a separate VoxelShape instance for each wall, we can just repurpose the
 * same shape instances. To do this we can cache a mapping between a state (represented only as its prop->value map)
 * and the desired shape, and generate the BlockState->VoxelShape map from this for each block.
 */
@Mixin(CrossCollisionBlock.class)
public abstract class WallBlockMixin extends Block {
    private static Map<ImmutableList<Float>, Pair<VoxelShape[], StateDefinition<Block, BlockState>>> CACHE_BY_SHAPE_VALS = new HashMap<>();

    public WallBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "makeShapes", at = @At("HEAD"), cancellable = true)
    private synchronized void useCachedShapeMap(float f1, float f2, float f3, float f4, float f5, CallbackInfoReturnable<VoxelShape[]> cir) {
        ImmutableList<Float> key = ImmutableList.of(f1, f2, f3, f4, f5);
        Pair<VoxelShape[], StateDefinition<Block, BlockState>> cache = CACHE_BY_SHAPE_VALS.get(key);
        // require the properties to be identical
        if(cache == null || !cache.getSecond().getProperties().equals(this.stateDefinition.getProperties()))
            return;
        cir.setReturnValue(cache.getFirst());
    }

    @Inject(method = "makeShapes", at = @At("RETURN"))
    private synchronized void storeCachedShapesByProperty(float f1, float f2, float f3, float f4, float f5, CallbackInfoReturnable<VoxelShape[]> cir) {
        // never populate cache as a non-vanilla block
        //if((Class<?>)this.getClass() != WallBlock.class)
        //    return;
        ImmutableList<Float> key = ImmutableList.of(f1, f2, f3, f4, f5);
        if(!CACHE_BY_SHAPE_VALS.containsKey(key)) {
            VoxelShape[] shapes = cir.getReturnValue();
            CACHE_BY_SHAPE_VALS.put(key, Pair.of(shapes, this.stateDefinition));
        }
    }
}
