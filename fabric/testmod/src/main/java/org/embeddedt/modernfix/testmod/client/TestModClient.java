package org.embeddedt.modernfix.testmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import org.embeddedt.modernfix.fabric.api.dynresources.ModelScanController;
import org.embeddedt.modernfix.testmod.TestMod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestModClient implements ClientModInitializer {
    private static final Pattern RGB_PATTERN = Pattern.compile("^wool_([0-9]+)_([0-9]+)_([0-9]+)$");
    @Override
    public void onInitializeClient() {
        ModelScanController.SCAN_PREDICATES.add(rl -> !rl.getNamespace().equals(TestMod.ID));
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(resourceManager -> (modelId, context) -> {
            if(modelId.getNamespace().equals(TestMod.ID)) {
                Matcher matcher = RGB_PATTERN.matcher(modelId.getPath());
                if(matcher.matches()) {
                    int r = Integer.parseInt(matcher.group(1));
                    int g = Integer.parseInt(matcher.group(2));
                    int b = Integer.parseInt(matcher.group(3));
                    return new TestModBlockModel(r, g, b);
                }
            }
            return null;
        });
    }
}
