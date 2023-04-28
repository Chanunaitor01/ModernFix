package org.embeddedt.modernfix.dynamicresources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.IRegistryDelegate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ModelLocationCache {
    private static final LoadingCache<BlockState, ModelResourceLocation> blockLocationCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .build(new CacheLoader<BlockState, ModelResourceLocation>() {
                @Override
                public ModelResourceLocation load(BlockState key) throws Exception {
                    return BlockModelShaper.stateToModelLocation(key);
                }
            });

    private static final LoadingCache<Item, ModelResourceLocation> itemLocationCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .build(new CacheLoader<Item, ModelResourceLocation>() {
                @Override
                public ModelResourceLocation load(Item key) throws Exception {
                    return new ModelResourceLocation(Registry.ITEM.getKey(key), "inventory");
                }
            });

    public static ModelResourceLocation get(BlockState state) {
        try {
            return blockLocationCache.get(state);
        } catch(ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    public static ModelResourceLocation get(Item item) {
        try {
            return itemLocationCache.get(item);
        } catch(ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
