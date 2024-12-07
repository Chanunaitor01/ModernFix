package org.embeddedt.modernfix.dynamicresources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SpriteGetter;
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

    private final LoadingCache<ResourceLocation, Optional<ClientItem>> loadedClientItemProperties =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(3, TimeUnit.MINUTES)
                    .maximumSize(1000)
                    .concurrencyLevel(8)
                    .softValues()
                    .build(new CacheLoader<>() {
                        @Override
                        public Optional<ClientItem> load(ResourceLocation key) {
                            return loadClientItemProperties(key);
                        }
                    });

    private final LoadingCache<ResourceLocation, Optional<ItemModel>> loadedItemModels =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(3, TimeUnit.MINUTES)
                    .maximumSize(1000)
                    .concurrencyLevel(8)
                    .softValues()
                    .build(new CacheLoader<>() {
                        @Override
                        public Optional<ItemModel> load(ResourceLocation key) {
                            return loadItemModel(key);
                        }
                    });

    private final BakedModel missingModel;
    private final ItemModel missingItemModel;
    private final UnbakedModel unbakedMissingModel;
    private final Function<ResourceLocation, StateDefinition<Block, BlockState>> stateMapper;
    private final ResourceManager resourceManager;
    private final ModelBakery.TextureGetter textureGetter;
    private final DynamicResolver resolver;
    private final EntityModelSet entityModelSet;
    private final ItemModelGenerator itemModelGenerator;

    public DynamicModelProvider(Map<ModelResourceLocation, BakedModel> initialBakedRegistry, BakedModel missingModel,
                                ItemModel missingItemModel, ResourceManager resourceManager, EntityModelSet entityModelSet,
                                Map<ResourceLocation, AtlasSet.StitchResult> atlasMap) {
        this.missingModel = missingModel;
        this.missingItemModel = missingItemModel;
        this.entityModelSet = entityModelSet;
        var missing = atlasMap.get(TextureAtlas.LOCATION_BLOCKS).missing();
        this.textureGetter = new ModelBakery.TextureGetter() {
            @Override
            public TextureAtlasSprite get(ModelDebugName modelDebugName, Material material) {
                var atlas = atlasMap.get(material.atlasLocation());
                var sprite = atlas.getSprite(material.texture());
                if (sprite != null) {
                    return sprite;
                } else {
                    ModernFix.LOGGER.warn("Unable to find sprite '{}' referenced by model '{}'", material.texture(), modelDebugName.get());
                    return missing;
                }
            }

            @Override
            public TextureAtlasSprite reportMissingReference(ModelDebugName modelDebugName, String string) {
                return missing;
            }
        };
        this.stateMapper = BlockStateModelLoader.definitionLocationToBlockMapper();
        this.resourceManager = resourceManager;
        this.unbakedMissingModel = MissingBlockModel.missingModel();
        this.resolver = new DynamicResolver();
        this.itemModelGenerator = new ItemModelGenerator();
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
        return Optional.of(BlockStateModelLoader.loadBlockStateDefinitionStack(location, stateDefinition, loadedDefinitions, this.unbakedMissingModel));
    }

    private BakedModel bakeModel(UnbakedModel model, ModelResourceLocation location) {
        synchronized (this) {
            this.resolver.clearResolver();
            model.resolveDependencies(this.resolver);
            var modelBaker = new DynamicBaker(location::toString);
            return UnbakedModel.bakeWithTopModelValues(model, modelBaker, BlockModelRotation.X0_Y0);
        }
    }

    private BakedModel bakeModel(UnbakedBlockStateModel model, ModelResourceLocation location) {
        synchronized (this) {
            this.resolver.clearResolver();
            model.resolveDependencies(this.resolver);
            var modelBaker = new DynamicBaker(location::toString);
            return model.bake(modelBaker);
        }
    }

    private Optional<BakedModel> loadBakedModel(ModelResourceLocation location) {
        if (location.variant().equals("standalone") || location.variant().equals("fabric_resource")) {
            return this.loadedBlockModels.getUnchecked(location.id()).map(unbakedModel -> {
                return this.bakeModel(unbakedModel, location);
            });
        } else {
            var optLoadedModels = this.loadedStateDefinitions.getUnchecked(location.id());
            Optional<UnbakedBlockStateModel> unbakedModelOpt = optLoadedModels.map(loadedModels -> {
                var loadedModel = loadedModels.models().get(location);
                if(loadedModel != null) {
                    return loadedModel.model();
                } else {
                    return null;
                }
            });
            return unbakedModelOpt.map(unbakedModel -> {
                return this.bakeModel(unbakedModel, location);
            });
        }
    }

    private Optional<UnbakedModel> loadBlockModel(ResourceLocation location) {
        if (location.equals(ItemModelGenerator.GENERATED_ITEM_MODEL_ID)) {
            return Optional.of(this.itemModelGenerator);
        }
        var resource = this.resourceManager.getResource(ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "models/" + location.getPath() + ".json"));
        if(resource.isPresent()) {
            try(Reader reader = resource.get().openAsReader()) {
                BlockModel blockModel = BlockModel.fromStream(reader);
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

    private Optional<ClientItem> loadClientItemProperties(ResourceLocation location) {
        var resource = this.resourceManager.getResource(ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "items/" + location.getPath() + ".json"));
        if(resource.isPresent()) {
            try(Reader reader = resource.get().openAsReader()) {
                ClientItem clientItem = ClientItem.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
                return Optional.of(clientItem);
            } catch(Exception e) {
                ModernFix.LOGGER.error("Failed to load block model {} from '{}'", location, resource.get().sourcePackId(), e);
                return Optional.empty();
            }
        } else {
            ModernFix.LOGGER.warn("Client item '{}' does not exist in any resource packs", location);
            return Optional.empty();
        }
    }

    private Optional<ItemModel> loadItemModel(ResourceLocation location) {
        return this.loadedClientItemProperties.getUnchecked(location).map(clientItem -> {
            var bakingContext = new ItemModel.BakingContext(new DynamicBaker(location::toString), this.entityModelSet, this.missingItemModel);
            return clientItem.model().bake(bakingContext);
        });
    }

    public BakedModel getModel(ModelResourceLocation location) {
        return this.loadedBakedModels.getUnchecked(location).orElse(this.missingModel);
    }

    public ClientItem.Properties getClientItemProperties(ResourceLocation location) {
        return this.loadedClientItemProperties.getUnchecked(location).map(ClientItem::properties).orElse(ClientItem.Properties.DEFAULT);
    }

    public ItemModel getItemModel(ResourceLocation location) {
        return this.loadedItemModels.getUnchecked(location).orElse(this.missingItemModel);
    }

    private class DynamicBaker implements ModelBaker {
        private final ModelDebugName modelDebugName;

        private DynamicBaker(ModelDebugName modelDebugName) {
            this.modelDebugName = modelDebugName;
        }

        @Override
        public BakedModel bake(ResourceLocation location, ModelState transform) {
            return DynamicModelProvider.this.loadBlockModel(location).map(unbakedModel -> {
                DynamicModelProvider.this.resolver.clearResolver();
                unbakedModel.resolveDependencies(DynamicModelProvider.this.resolver);
                return UnbakedModel.bakeWithTopModelValues(unbakedModel, this, transform);
            }).orElse(DynamicModelProvider.this.missingModel);
        }

        @Override
        public SpriteGetter sprites() {
            return DynamicModelProvider.this.textureGetter.bind(this.modelDebugName);
        }

        @Override
        public ModelDebugName rootName() {
            return this.modelDebugName;
        }
    }

    /**
     * Based on the Mojang impl but with some changes to make it slightly more efficient.
     */
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
}
