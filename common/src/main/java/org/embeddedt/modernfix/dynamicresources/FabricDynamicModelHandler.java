package org.embeddedt.modernfix.dynamicresources;

import net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.modernfix.ModernFix;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FabricDynamicModelHandler implements DynamicModelProvider.DynamicModelPlugin {
    private final List<ModelLoadingPlugin> pluginList;

    // Borrowed from Fabric API, this dispatching logic is extremely trivial

    private static final ResourceLocation[] MODEL_MODIFIER_PHASES = new ResourceLocation[] { ModelModifier.OVERRIDE_PHASE, ModelModifier.DEFAULT_PHASE, ModelModifier.WRAP_PHASE, ModelModifier.WRAP_LAST_PHASE };

    private final Event<ModelModifier.OnLoad> onLoadModifiers = EventFactory.createWithPhases(ModelModifier.OnLoad.class, modifiers -> (model, context) -> {
        for (ModelModifier.OnLoad modifier : modifiers) {
            try {
                model = modifier.modifyModelOnLoad(model, context);
            } catch (Exception exception) {
                ModernFix.LOGGER.error("Failed to modify unbaked model on load", exception);
            }
        }

        return model;
    }, MODEL_MODIFIER_PHASES);

    private final Event<ModelModifier.OnLoadBlock> onLoadBlockModifiers = EventFactory.createWithPhases(ModelModifier.OnLoadBlock.class, modifiers -> (model, context) -> {
        for (ModelModifier.OnLoadBlock modifier : modifiers) {
            try {
                model = modifier.modifyModelOnLoad(model, context);
            } catch (Exception exception) {
                ModernFix.LOGGER.error("Failed to modify unbaked block model on load", exception);
            }
        }

        return model;
    }, MODEL_MODIFIER_PHASES);

    public FabricDynamicModelHandler(DynamicModelProvider provider) {
        this.pluginList = ModelLoadingPlugin.getAll();
        var context = new PluginContext(provider);
        for (var plugin : this.pluginList) {
            plugin.initialize(context);
        }
        context.fireResolvers();
    }

    @Override
    public Optional<UnbakedModel> modifyModelOnLoad(Optional<UnbakedModel> model, ResourceLocation id) {
        return Optional.ofNullable(this.onLoadModifiers.invoker().modifyModelOnLoad(model.orElse(null), () -> id));
    }

    @Override
    public UnbakedBlockStateModel modifyBlockModelOnLoad(UnbakedBlockStateModel model, ModelResourceLocation id, BlockState state) {
        return this.onLoadBlockModifiers.invoker().modifyModelOnLoad(model, new ModelModifier.OnLoadBlock.Context() {
            @Override
            public ModelResourceLocation id() {
                return id;
            }

            @Override
            public BlockState state() {
                return state;
            }
        });
    }

    private class PluginContext implements ModelLoadingPlugin.Context {
        private final DynamicModelProvider provider;
        private final Map<Block, BlockStateResolver> resolvers = new HashMap<>();

        private PluginContext(DynamicModelProvider provider) {
            this.provider = provider;
        }

        @Override
        public void addModels(ResourceLocation... ids) {
            /* no-op on dynamic model loader */
        }

        @Override
        public void addModels(Collection<? extends ResourceLocation> ids) {
            /* no-op on dynamic model loader */
        }

        @Override
        public void registerBlockStateResolver(Block block, BlockStateResolver resolver) {
            resolvers.put(block, resolver);
        }

        public void fireResolvers() {
            resolvers.forEach((block, resolver) -> {
                resolver.resolveBlockStates(new BlockStateResolver.Context() {
                    @Override
                    public Block block() {
                        return block;
                    }

                    @Override
                    public void setModel(BlockState state, UnbakedBlockStateModel model) {
                        provider.addUnbakedBlockStateOverride(BlockModelShaper.stateToModelLocation(state), model);
                    }
                });
            });
        }

        @Override
        public Event<ModelModifier.OnLoad> modifyModelOnLoad() {
            return onLoadModifiers;
        }

        @Override
        public Event<ModelModifier.OnLoadBlock> modifyBlockModelOnLoad() {
            return onLoadBlockModifiers;
        }
    }
}
