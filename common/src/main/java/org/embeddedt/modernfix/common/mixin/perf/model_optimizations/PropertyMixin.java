package org.embeddedt.modernfix.common.mixin.perf.model_optimizations;

import net.minecraft.world.level.block.state.properties.AbstractProperty;
import org.embeddedt.modernfix.dedup.IdentifierCaches;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractProperty.class)
public class PropertyMixin {

    @Shadow @Mutable
    @Final private String name;

    @Shadow private Integer hashCode;

    @Shadow @Final private Class clazz;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/state/properties/AbstractProperty;name:Ljava/lang/String;"))
    private void internName(AbstractProperty<?> instance, String name) {
        this.name = IdentifierCaches.PROPERTY.deduplicate(name);
    }
    /**
     * @author embeddedt
     * @reason compare hashcodes if generated, use reference equality for speed
     */
    @Overwrite
    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (!(p_equals_1_ instanceof AbstractProperty<?>)) {
            return false;
        } else {
            AbstractProperty<?> property = (AbstractProperty<?>)p_equals_1_;
            /* reference equality is safe here because of deduplication */
            return this.clazz == property.getValueClass() && this.name == property.getName();
        }
    }
}
