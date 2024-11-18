package org.embeddedt.modernfix.forge.mixin.perf.kubejs;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import dev.latvian.kubejs.recipe.RecipeEventJS;
import dev.latvian.kubejs.recipe.RecipeJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.embeddedt.modernfix.ModernFix;
import org.embeddedt.modernfix.annotation.RequiresMod;
import org.embeddedt.modernfix.forge.util.KubeUtil;
import org.embeddedt.modernfix.forge.util.ModUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mixin(RecipeEventJS.class)
@RequiresMod("kubejs")
public class RecipeEventJSMixin {
    @Shadow(remap = false) @Final private List<RecipeJS> originalRecipes;

    /**
     * @author embeddedt
     * @reason parallelize filtering, then run the consumer on one thread
     */
    @Inject(method = "forEachRecipe", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"), remap = false)
    public void forEachRecipe(Object o, Consumer<RecipeJS> consumer, CallbackInfo ci, @Local(ordinal = 0) Predicate<RecipeJS> filter) {
        List<RecipeJS> filtered = LamdbaExceptionUtils.uncheck(() -> ModUtil.commonPool.submit(() -> this.originalRecipes.parallelStream().filter(filter).collect(Collectors.toList())).get());
        filtered.forEach(consumer);
    }

    @Inject(method = "post(Lnet/minecraft/world/item/crafting/RecipeManager;Ljava/util/Map;)V", at = @At(value = "INVOKE", target = "Ldev/latvian/kubejs/recipe/RecipeEventJS;post(Ldev/latvian/kubejs/script/ScriptType;Ljava/lang/String;)Z", remap = false))
    private void buildRecipeRegistry(RecipeManager manager, Map<ResourceLocation, JsonObject> jsonMap, CallbackInfo ci) {
        for(RecipeJS recipe : this.originalRecipes) {
            KubeUtil.originalRecipesByHash.put(recipe.id, recipe);
        }
    }

    @Inject(method = "post(Lnet/minecraft/world/item/crafting/RecipeManager;Ljava/util/Map;)V", at = @At("RETURN"))
    private void clearRecipeRegistry(RecipeManager manager, Map<ResourceLocation, JsonObject> jsonMap, CallbackInfo ci) {
        KubeUtil.originalRecipesByHash.clear();
    }

    /**
     * The recipe event object can be leaked in scripts and this wastes 40MB of memory.
     */
    @Inject(method = "post", at = @At("RETURN"))
    private void clearRecipeLists(CallbackInfo ci) {
        ModernFix.LOGGER.info("Clearing KubeJS recipe lists...");
        // Even though we are a mixin class, use reflection so this works across a variety of versions
        Field[] fields = RecipeEventJS.class.getDeclaredFields();
        for(Field f : fields) {
            try {
                if(!Modifier.isStatic(f.getModifiers())
                        && (Collection.class.isAssignableFrom(f.getType())
                            || Map.class.isAssignableFrom(f.getType()))
                ) {
                    f.setAccessible(true);
                    Object collection = f.get(this);
                    int size;
                    if(collection instanceof Map) {
                        size = ((Map<?, ?>)collection).size();
                        ((Map<?, ?>)collection).clear();
                    } else if(collection instanceof Collection) {
                        size = ((Collection<?>)collection).size();
                        ((Collection<?>)collection).clear();
                    } else
                        throw new IllegalStateException();
                    ModernFix.LOGGER.debug("Cleared {} with {} entries", f.getName(), size);
                }
            } catch(RuntimeException | ReflectiveOperationException e) {
                ModernFix.LOGGER.debug("Uh oh, couldn't clear field", e);
            }
        }
    }
}
