package org.embeddedt.modernfix.fabric.bridge;

import net.fabricmc.fabric.impl.client.model.ModelLoadingRegistryImpl;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.function.Consumer;

public class ModelV0Bridge {
    public static void populate(Consumer<ModelResourceLocation> modelConsumer, ModelBakery bakery, ResourceManager manager) {
        ModelLoadingRegistryImpl.LoaderInstance instance = ModelLoadingRegistryImpl.begin(bakery, manager);
        instance.onModelPopulation(modelConsumer);
        instance.finish();
    }
}
