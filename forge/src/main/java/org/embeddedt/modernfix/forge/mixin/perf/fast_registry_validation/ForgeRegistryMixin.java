package org.embeddedt.modernfix.forge.mixin.perf.fast_registry_validation;

import net.minecraftforge.registries.ForgeRegistry;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;

@Mixin(value = ForgeRegistry.class, remap = false)
public class ForgeRegistryMixin<V> {
    //private static Method bitSetTrimMethod = null;
    //private static boolean bitSetTrimMethodRetrieved = false;

    ///**
    // * Cache the result of findMethod instead of running it multiple times.
    // * Null checks are not required as the surrounding code handles it already.
    // */
    //@Redirect(method = "validateContent", at = @At(value = "HEAD"))
    //private Method skipMultipleRemap(Class<?> clz, String methodName, Class<?>[] params) {
    //    if(!bitSetTrimMethodRetrieved) {
    //        bitSetTrimMethodRetrieved = true;
    //        bitSetTrimMethod = ObfuscationReflectionHelper.findMethod(BitSet.class, methodName, params);
    //    }
    //    return bitSetTrimMethod;
    //}

    private int expectedNextBit = -1;

    /**
     * Avoid calling nextClearBit and scanning the whole registry for every block registration.
     */
    @Redirect(method = "add(ILnet/minecraft/resources/ResourceLocation;Ljava/lang/Object;Ljava/lang/String;)I", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;nextClearBit(I)I"))
    private int useCachedBit(BitSet availabilityMap, int minimum) {
        int bit = availabilityMap.nextClearBit(expectedNextBit != -1 ? expectedNextBit : minimum);
        expectedNextBit = bit + 1;
        return bit;
    }

    @Inject(method = { "sync", "clear", "block" }, at = @At("HEAD"))
    private void clearBitCache(CallbackInfo ci) {
        expectedNextBit = -1;
    }

    @Redirect(method = "add(ILnet/minecraft/resources/ResourceLocation;Ljava/lang/Object;Ljava/lang/String;)I", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;trace(Lorg/apache/logging/log4j/Marker;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void skipTrace(Logger logger, Marker marker, String s, Object o, Object o1, Object o2, Object o3, Object o4) {

    }
}
