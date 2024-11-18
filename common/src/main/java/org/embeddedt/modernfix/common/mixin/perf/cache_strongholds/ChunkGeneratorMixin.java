package org.embeddedt.modernfix.common.mixin.perf.cache_strongholds;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.StrongholdFeature;
import org.embeddedt.modernfix.ModernFix;
import org.embeddedt.modernfix.duck.IServerLevel;
import org.embeddedt.modernfix.platform.ModernFixPlatformHooks;
import org.embeddedt.modernfix.world.StrongholdLocationCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(StrongholdFeature.class)
public class ChunkGeneratorMixin {
    @Shadow @Final @Mutable private ChunkPos[] strongholdPos;

    @Inject(method = "generatePositions", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", ordinal = 0, remap = false), cancellable = true)
    private void useCachedDataIfAvailable(ChunkGenerator<?> generator, CallbackInfo ci) {
        ServerLevel level = searchLevel(generator);
        if(level == null) {
            ModernFix.LOGGER.error("Can't find server level for " + this);
            return;
        }
        StrongholdLocationCache cache = ((IServerLevel)level).mfix$getStrongholdCache();
        List<ChunkPos> positions = cache.getChunkPosList();
        if(positions.isEmpty())
            return;
        ModernFix.LOGGER.debug("Loaded stronghold cache for dimension {} with {} positions", level.dimension.getType().toString(), positions.size());
        this.strongholdPos = positions.toArray(new ChunkPos[0]);
        ci.cancel();
    }

    private ServerLevel searchLevel(ChunkGenerator<?> generator) {
        MinecraftServer server = ModernFixPlatformHooks.INSTANCE.getCurrentServer();
        if(server != null) {
            ServerLevel ourLevel = null;
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getChunkSource().getGenerator() == generator) {
                    ourLevel = level;
                    break;
                }
            }
            return ourLevel;
        } else
            return null;
    }

    @Inject(method = "generatePositions", at = @At("TAIL"))
    private void saveCachedData(ChunkGenerator<?> generator, CallbackInfo ci) {
        if(this.strongholdPos.length > 0) {
            ServerLevel level = searchLevel(generator);
            if(level != null) {
                StrongholdLocationCache cache = ((IServerLevel)level).mfix$getStrongholdCache();
                cache.setChunkPosList(Arrays.asList(this.strongholdPos));
                ModernFix.LOGGER.debug("Saved stronghold cache for dimension {}", level.dimension.getType().toString());
            }
        }
    }
}
