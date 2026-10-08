package net.mads.industron.mixin;
import net.mads.industron.climate.ClimateContext;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Seasonal thermal exposure replaces instant damage from a static hot-biome tag. */
@Mixin(SnowGolem.class)
public abstract class SnowGolemClimateMixin {
    @Inject(method="aiStep",at=@At("HEAD"))
    private void industron$begin(CallbackInfo ci){ClimateContext.push(((SnowGolem)(Object)this).level());}
    @Inject(method="aiStep",at=@At("RETURN"))
    private void industron$end(CallbackInfo ci){ClimateContext.pop();}
    @Redirect(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/core/Holder;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean industron$melting(Holder<Biome> biome,TagKey<Biome> tag) {
        return !tag.equals(BiomeTags.SNOW_GOLEM_MELTS)&&biome.is(tag);
    }
}
