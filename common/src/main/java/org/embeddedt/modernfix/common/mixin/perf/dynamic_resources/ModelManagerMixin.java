package org.embeddedt.modernfix.common.mixin.perf.dynamic_resources;

import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.embeddedt.modernfix.dynamicresources.DynamicModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
@ClientOnlyMixin
public class ModelManagerMixin implements DynamicModelProvider.ModelManagerExtension {
    @Shadow private BakedModel missingModel;
    @Unique
    private DynamicModelProvider mfix$modelProvider;

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelManager;loadBlockModels(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Map<ResourceLocation, BlockModel>> deferBlockModelLoad(ResourceManager manager, Executor executor) {
        return CompletableFuture.completedFuture(Map.of());
    }

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BlockStateModelLoader;loadBlockStates(Lnet/minecraft/client/resources/model/UnbakedModel;Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<BlockStateModelLoader.LoadedModels> deferBlockStateLoad(UnbakedModel unbakedModel, ResourceManager resourceManager, Executor executor) {
        return CompletableFuture.completedFuture(new BlockStateModelLoader.LoadedModels(Map.of()));
    }

    /**
     * @author embeddedt
     * @reason disable map creation
     */
    @Overwrite
    private static Map<BlockState, BakedModel> createBlockStateToModelDispatch(Map<ModelResourceLocation, BakedModel> map, BakedModel bakedModel) {
        return Map.of();
    }

    @ModifyArg(method = "reload", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;allOf([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;", ordinal = 1))
    private CompletableFuture<?>[] createModelProvider(CompletableFuture<?>[] cfs, @Local(ordinal = 0) CompletableFuture<EntityModelSet> entityModelFuture, @Local(ordinal = 0, argsOnly = true) Executor executor, @Local(ordinal = 0) Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> atlasPreparations) {
        CompletableFuture<Void> makeModelProviderFuture = CompletableFuture.supplyAsync(() -> {
            return Map.copyOf(Maps.transformValues(atlasPreparations, CompletableFuture::join));
        }, executor).thenAcceptBoth(entityModelFuture, (stitchResults, entityModelSet) -> {
            this.mfix$modelProvider = new DynamicModelProvider(
                    Minecraft.getInstance().getResourceManager(),
                    entityModelSet,
                    stitchResults
            );
            DynamicModelProvider.currentReloadingModelProvider = new WeakReference<>(this.mfix$modelProvider);
        });
        return ArrayUtils.add(cfs, makeModelProviderFuture);
    }

    /**
     * @author embeddedt
     * @reason use dynamic model system
     */
    @Overwrite
    public BakedModel getModel(ModelResourceLocation modelLocation) {
        if(this.mfix$modelProvider != null) {
            return this.mfix$modelProvider.getModel(modelLocation);
        } else {
            return this.missingModel;
        }
    }

    /**
     * @author embeddedt
     * @reason use dynamic model system
     */
    @Overwrite
    public ItemModel getItemModel(ResourceLocation resourceLocation) {
        return this.mfix$modelProvider.getItemModel(resourceLocation);
    }

    /**
     * @author embeddedt
     * @reason use dynamic model system
     */
    @Overwrite
    public ClientItem.Properties getItemProperties(ResourceLocation resourceLocation) {
        return this.mfix$modelProvider.getClientItemProperties(resourceLocation);
    }

    @Override
    public DynamicModelProvider mfix$getModelProvider() {
        return this.mfix$modelProvider;
    }
}
