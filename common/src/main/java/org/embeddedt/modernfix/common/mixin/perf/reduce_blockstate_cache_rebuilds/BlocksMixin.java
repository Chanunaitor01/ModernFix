package org.embeddedt.modernfix.common.mixin.perf.reduce_blockstate_cache_rebuilds;

import net.minecraft.server.MinecraftServer;
import org.embeddedt.modernfix.blockstate.BlockStateCacheHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

@Mixin(value = MinecraftServer.class, priority = 1100)
public class BlocksMixin {
    @ModifyArg(method = "refreshRegistries", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/IdMapper;forEach(Ljava/util/function/Consumer;)V"), index = 0)
    private Consumer getEmptyConsumer(Consumer original) {
        BlockStateCacheHandler.rebuildParallel(true);
        return o -> {};
    }
}
