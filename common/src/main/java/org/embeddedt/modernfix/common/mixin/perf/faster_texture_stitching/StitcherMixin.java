package org.embeddedt.modernfix.common.mixin.perf.faster_texture_stitching;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.StitcherException;
import org.embeddedt.modernfix.ModernFix;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.embeddedt.modernfix.platform.ModernFixPlatformHooks;
import org.embeddedt.modernfix.textures.StbStitcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Mixin(Stitcher.class)
@ClientOnlyMixin
public class StitcherMixin<T extends Stitcher.Entry> {
    @Shadow @Final private List<Stitcher.Holder<T>> texturesToBeStitched;

    @Shadow private int storageX;

    @Shadow private int storageY;

    @Shadow @Final private int maxWidth;
    @Shadow @Final private int maxHeight;

    @Shadow @Final private static Comparator<Stitcher.Holder<?>> HOLDER_COMPARATOR;
    private List<StbStitcher.LoadableSpriteInfo<T>> loadableSpriteInfos;

    /**
     * @author embeddedt, SuperCoder79
     * @reason Use improved STB stitcher instead of the vanilla implementation, for performance
     */
    @Inject(method = "stitch", at = @At("HEAD"), cancellable = true)
    private void stitchFast(CallbackInfo ci) {
        if(!ModernFixPlatformHooks.INSTANCE.isLoadingNormally()) {
            ModernFix.LOGGER.error("Using vanilla stitcher implementation due to invalid loading state");
            return;
        }
        ci.cancel();
        ObjectArrayList<Stitcher.Holder<T>> holderList = new ObjectArrayList<>(this.texturesToBeStitched);
        holderList.sort(HOLDER_COMPARATOR);
        Stitcher.Holder<T>[] aholder = holderList.toArray(new Stitcher.Holder[0]);

        Pair<Pair<Integer, Integer>, List<StbStitcher.LoadableSpriteInfo<T>>> packingInfo = StbStitcher.packRects(aholder);
        this.storageX = packingInfo.getFirst().getFirst();
        this.storageY = packingInfo.getFirst().getSecond();

        // Detect an oversized atlas generated in the previous step.
        if(this.storageX > this.maxWidth || this.storageY > this.maxHeight) {
            ModernFix.LOGGER.error("Requested atlas size {}x{} exceeds maximum of {}x{}", this.storageX, this.storageY, this.maxWidth, this.maxHeight);
            throw new StitcherException(aholder[0].entry(), Stream.of(aholder).map(arg -> arg.entry()).collect(ImmutableList.toImmutableList()));
        }

        this.loadableSpriteInfos = packingInfo.getSecond();
    }

    /**
     * @author embeddedt, SuperCoder79
     * @reason We setup the image ourselves in the StbStitcher, so we just feed this information back into the vanilla code
     */
    @Inject(method = "gatherSprites", at = @At("HEAD"), cancellable = true)
    private void gatherSpritesFast(Stitcher.SpriteLoader<T> spriteLoader, CallbackInfo ci) {
        if(!ModernFixPlatformHooks.INSTANCE.isLoadingNormally())
            return;
        ci.cancel();
        for(StbStitcher.LoadableSpriteInfo<T> info : loadableSpriteInfos) {
            spriteLoader.load(info.info, info.x, info.y);
        }
    }
}
