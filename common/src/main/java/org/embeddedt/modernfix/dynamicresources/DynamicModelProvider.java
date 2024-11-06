package org.embeddedt.modernfix.dynamicresources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.embeddedt.modernfix.ModernFix;
import org.embeddedt.modernfix.util.DynamicMap;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles loading models dynamically, rather than at startup time.
 */
public class DynamicModelProvider {
    /*
    private final LoadingCache<ResourceLocation, Optional<BlockStateModelLoader.LoadedModels>> loadedStateDefinitions =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(3, TimeUnit.MINUTES)
                    .maximumSize(1000)
                    .concurrencyLevel(8)
                    .softValues()
                    .build(new CacheLoader<>() {
                        @Override
                        public Optional<BlockStateModelLoader.LoadedModels> load(ResourceLocation key) {
                            return loadBlockStateDefinition(key);
                        }
                    });

    private final LoadingCache<ResourceLocation, Optional<UnbakedModel>> loadedBlockModels =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(3, TimeUnit.MINUTES)
                    .maximumSize(1000)
                    .concurrencyLevel(8)
                    .softValues()
                    .build(new CacheLoader<>() {
                        @Override
                        public Optional<UnbakedModel> load(ResourceLocation key) {
                            return loadBlockModel(key);
                        }
                    });

    private final LoadingCache<ModelResourceLocation, Optional<UnbakedModel>> loadedUnbakedModels =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(3, TimeUnit.MINUTES)
                    .maximumSize(1000)
                    .concurrencyLevel(8)
                    .softValues()
                    .build(new CacheLoader<>() {
                        @Override
                        public Optional<UnbakedModel> load(ModelResourceLocation key) {
                            return loadModel(key);
                        }
                    });

    private final LoadingCache<ModelResourceLocation, Optional<BakedModel>> loadedBakedModels =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(3, TimeUnit.MINUTES)
                    .maximumSize(1000)
                    .concurrencyLevel(8)
                    .softValues()
                    .build(new CacheLoader<>() {
                        @Override
                        public Optional<BakedModel> load(ModelResourceLocation key) {
                            return loadBakedModel(key);
                        }
                    });

    private final Map<ModelResourceLocation, BakedModel> initialBakedRegistry;
    private final BakedModel missingModel;
    private final UnbakedModel unbakedMissingModel;
    private final Function<ResourceLocation, StateDefinition<Block, BlockState>> stateMapper;
    private final ResourceManager resourceManager;
    private final BlockStateModelLoader blockStateModelLoader;
    private final ModelBakery.TextureGetter textureGetter;
    private final DynamicMap<ResourceLocation, UnbakedModel> fakeUnbakedModelMap;
    private final DynamicResolver resolver;

    public DynamicModelProvider(Map<ModelResourceLocation, BakedModel> initialBakedRegistry, BakedModel missingModel, ResourceManager resourceManager, Map<ResourceLocation, AtlasSet.StitchResult> atlasMap) {
        this.initialBakedRegistry = initialBakedRegistry;
        this.missingModel = missingModel;
        this.textureGetter = (mrl, material) -> {
            var atlas = atlasMap.get(material.atlasLocation());
            var sprite = atlas.getSprite(material.texture());
            if (sprite != null) {
                return sprite;
            } else {
                ModernFix.LOGGER.warn("Unable to find sprite '{}' referenced by model '{}'", material.texture(), mrl);
                return atlas.missing();
            }
        };
        this.stateMapper = BlockStateModelLoader.definitionLocationToBlockMapper();
        this.resourceManager = resourceManager;
        this.unbakedMissingModel = MissingBlockModel.missingModel();
        this.blockStateModelLoader = new BlockStateModelLoader(this.unbakedMissingModel);
        this.fakeUnbakedModelMap = new DynamicMap<>(ResourceLocation.class, key -> this.loadedBlockModels.getUnchecked(key).orElse(null));
        this.resolver = new DynamicResolver();
    }

    private Optional<BlockStateModelLoader.LoadedModels> loadBlockStateDefinition(ResourceLocation location) {
        StateDefinition<Block, BlockState> stateDefinition = this.stateMapper.apply(location);
        if(stateDefinition == null) {
            return Optional.empty();
        }
        List<Resource> resources = resourceManager.getResourceStack(ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "blockstates/" + location.getPath() + ".json"));
        List<BlockStateModelLoader.LoadedBlockModelDefinition> loadedDefinitions = new ArrayList<>(resources.size());
        for(Resource resource : resources) {
            try(Reader reader = resource.openAsReader()) {
                JsonObject jsonObject = GsonHelper.parse(reader);
                BlockModelDefinition blockModelDefinition = BlockModelDefinition.fromJsonElement(jsonObject);
                loadedDefinitions.add(new BlockStateModelLoader.LoadedBlockModelDefinition(resource.sourcePackId(), blockModelDefinition));
            } catch(Exception e) {
                ModernFix.LOGGER.error("Failed to load blockstate definition {} from pack '{}'", location, resource.sourcePackId(), e);
            }
        }
        return Optional.of(this.blockStateModelLoader.loadBlockStateDefinitionStack(location, stateDefinition, loadedDefinitions));
    }

    private BakedModel bakeModel(UnbakedModel model, ModelResourceLocation location) {
        synchronized (this) {
            this.resolver.clearResolver();
            model.resolveDependencies(this.resolver);
            var modelBakery = new ModelBakery(Map.of(location, model), this.fakeUnbakedModelMap, this.unbakedMissingModel);
            modelBakery.bakeModels(this.textureGetter);
            return modelBakery.getBakedTopLevelModels().get(location);
        }
    }

    private Optional<BakedModel> loadBakedModel(ModelResourceLocation location) {
        var unbakedModel = this.loadedUnbakedModels.getUnchecked(location);
        return unbakedModel.map(model -> this.bakeModel(model, location));
    }

    private Optional<UnbakedModel> loadBlockModel(ResourceLocation location) {
        if(location.equals(SpecialModels.BUILTIN_GENERATED)) {
            return Optional.of(SpecialModels.GENERATED_MARKER);
        } else if(location.equals(SpecialModels.BUILTIN_BLOCK_ENTITY)) {
            return Optional.of(SpecialModels.BLOCK_ENTITY_MARKER);
        }
        var resource = this.resourceManager.getResource(ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "models/" + location.getPath() + ".json"));
        if(resource.isPresent()) {
            try(Reader reader = resource.get().openAsReader()) {
                BlockModel blockModel = BlockModel.fromStream(reader);
                blockModel.name = location.toString();
                return Optional.of(blockModel);
            } catch(Exception e) {
                ModernFix.LOGGER.error("Failed to load block model {} from '{}'", location, resource.get().sourcePackId(), e);
                return Optional.empty();
            }
        } else {
            ModernFix.LOGGER.warn("Model '{}' does not exist in any resource packs", location);
            return Optional.empty();
        }
    }

    private Optional<UnbakedModel> loadModel(ModelResourceLocation location) {
        if (location.variant().equals(ModelResourceLocation.INVENTORY_VARIANT)) {
            return this.loadedBlockModels.getUnchecked(ResourceLocation.fromNamespaceAndPath(location.id().getNamespace(), "item/" + location.id().getPath()));
        } else if (location.variant().equals("standalone") || location.variant().equals("fabric_resource")) {
            return this.loadedBlockModels.getUnchecked(location.id());
        } else {
            var optLoadedModels = this.loadedStateDefinitions.getUnchecked(location.id());
            return optLoadedModels.map(loadedModels -> {
                var loadedModel = loadedModels.models().get(location);
                if(loadedModel != null) {
                    return loadedModel.model();
                } else {
                    return null;
                }
            });
        }
    }

    public BakedModel getModel(ModelResourceLocation location) {
        return this.loadedBakedModels.getUnchecked(location).orElse(this.missingModel);
    }

     */

    /**
     * Based on the Mojang impl but with some changes to make it slightly more efficient.
     */
    /*
    private class DynamicResolver implements UnbakedModel.Resolver {
        private final Set<ResourceLocation> stack = new ObjectOpenHashSet<>(4);
        private final Set<ResourceLocation> resolvedModels = new ObjectOpenHashSet<>();

        @Override
        public UnbakedModel resolve(ResourceLocation resourceLocation) {
            if (this.stack.contains(resourceLocation)) {
                ModernFix.LOGGER.warn("Detected model loading loop: {}->{}", this.stacktraceToString(), resourceLocation);
                return DynamicModelProvider.this.unbakedMissingModel;
            } else {
                UnbakedModel unbakedModel = DynamicModelProvider.this.loadedBlockModels.getUnchecked(resourceLocation).orElse(DynamicModelProvider.this.unbakedMissingModel);
                if (this.resolvedModels.add(resourceLocation)) {
                    this.stack.add(resourceLocation);
                    unbakedModel.resolveDependencies(this);
                    this.stack.remove(resourceLocation);
                }

                return unbakedModel;
            }
        }

        private String stacktraceToString() {
            return this.stack.stream().map(ResourceLocation::toString).collect(Collectors.joining("->"));
        }

        public void clearResolver() {
            this.stack.clear();
            this.resolvedModels.clear();
        }
    }

     */
}
