package org.embeddedt.modernfix.common.mixin.perf.thread_priorities;

import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Mixin(Util.class)
public class UtilMixin {
    @ModifyArg(method = "makeBackgroundExecutor", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ForkJoinPool;<init>(ILjava/util/concurrent/ForkJoinPool$ForkJoinWorkerThreadFactory;Ljava/lang/Thread$UncaughtExceptionHandler;Z)V"), index = 1)
    private static ForkJoinPool.ForkJoinWorkerThreadFactory adjustPriorityOfThreadFactory(ForkJoinPool.ForkJoinWorkerThreadFactory factory) {
        return pool -> {
            ForkJoinWorkerThread thread = factory.newThread(pool);
            int pri = 4; // used to be configurable, but this causes classloading issues
            thread.setPriority(pri);
            return thread;
        };
    }
}
