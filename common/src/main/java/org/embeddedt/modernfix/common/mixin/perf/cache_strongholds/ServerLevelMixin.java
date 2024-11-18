package org.embeddedt.modernfix.common.mixin.perf.cache_strongholds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.Dimension;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelData;
import org.embeddedt.modernfix.duck.IServerLevel;
import org.embeddedt.modernfix.world.StrongholdLocationCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements IServerLevel {

    protected ServerLevelMixin(LevelData levelData, DimensionType dimensionType, BiFunction<Level, Dimension, ChunkSource> biFunction, ProfilerFiller profilerFiller, boolean bl) {
        super(levelData, dimensionType, biFunction, profilerFiller, bl);
    }

    @Shadow public abstract DimensionDataStorage getDataStorage();

    private StrongholdLocationCache mfix$strongholdCache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addStrongholdCache(CallbackInfo ci) {
        mfix$strongholdCache = this.getDataStorage().computeIfAbsent(() -> new StrongholdLocationCache((ServerLevel)(Object)this), StrongholdLocationCache.getFileId(this.dimension.getType()));
    }

    @Override
    public StrongholdLocationCache mfix$getStrongholdCache() {
        return mfix$strongholdCache;
    }
}
