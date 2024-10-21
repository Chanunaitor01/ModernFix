package org.embeddedt.modernfix.common.mixin.perf.fix_loop_spin_waiting;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

// This fixes https://bugs.mojang.com/browse/MC-183518
@Mixin(value = MinecraftServer.class, priority = 500)
public abstract class MinecraftServerMixin {

    @Shadow private long nextTickTimeNanos;
    @Unique
    private boolean mfix$isWaitingForNextTick = false;

    @WrapWithCondition(
            method = "waitUntilNextTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;managedBlock(Ljava/util/function/BooleanSupplier;)V")
    )
    private boolean manageBlockWithCondition(MinecraftServer instance, BooleanSupplier booleanSupplier) {
        try {
            this.mfix$isWaitingForNextTick = true;
            return true;
        } finally {
            this.mfix$isWaitingForNextTick = false;
        }
    }

    @Redirect(
            method = "waitForTasks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ReentrantBlockableEventLoop;waitForTasks()V")
    )
    private void waitForTasks(ReentrantBlockableEventLoop instance) {
        long i = this.mfix$isWaitingForNextTick ? this.nextTickTimeNanos - Util.getNanos() : 100_000L;
        LockSupport.parkNanos("waiting for tasks", i);
    }
}
