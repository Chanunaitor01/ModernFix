package org.embeddedt.modernfix.common.mixin.perf.dynamic_resources;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.modernfix.annotation.ClientOnlyMixin;
import org.embeddedt.modernfix.dynamicresources.DynamicModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
@ClientOnlyMixin
public class ModelManagerMixin {
    @Shadow private BakedModel missingModel;
    @Shadow private ItemModel missingItemModel;
    @Shadow private EntityModelSet entityModelSet;
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

    @Inject(method = "apply", at = @At("RETURN"))
    private void createModelProvider(ModelManager.ReloadState reloadState, ProfilerFiller profiler, CallbackInfo ci) {
        this.mfix$modelProvider = new DynamicModelProvider(
                null, // TODO
                this.missingModel,
                this.missingItemModel,
                Minecraft.getInstance().getResourceManager(),
                this.entityModelSet,
                reloadState.atlasPreparations()
        );
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
}
