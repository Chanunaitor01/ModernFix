package org.embeddedt.modernfix.common.mixin.perf.thread_priorities;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.players.GameProfileCache;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.net.Proxy;

@Mixin(IntegratedServer.class)
@ClientOnlyMixin
public abstract class IntegratedServerMixin extends MinecraftServer {
    public IntegratedServerMixin(File file, Proxy proxy, DataFixer dataFixer, Commands commands, YggdrasilAuthenticationService yggdrasilAuthenticationService, MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository, GameProfileCache gameProfileCache, ChunkProgressListenerFactory chunkProgressListenerFactory, String string) {
        super(file, proxy, dataFixer, commands, yggdrasilAuthenticationService, minecraftSessionService, gameProfileRepository, gameProfileCache, chunkProgressListenerFactory, string);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void adjustServerPriority(CallbackInfo ci) {
        int pri = 4; //ModernFixConfig.INTEGRATED_SERVER_PRIORITY.get();
        this.serverThread.setPriority(pri);
    }
}
