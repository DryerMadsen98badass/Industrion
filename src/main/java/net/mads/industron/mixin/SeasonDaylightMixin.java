package net.mads.industron.mixin;
import net.mads.industron.climate.ClimateMath;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Adjust the solar clock only. Fixed-time dimensions and the world's calendar are unchanged. */
@Mixin(DimensionType.class)
public abstract class SeasonDaylightMixin {
    @ModifyVariable(method="timeOfDay",at=@At("HEAD"),argsOnly=true)
    private long industron$seasonSun(long time) {
        DimensionType type=(DimensionType)(Object)this;
        return !type.hasFixedTime() && type.effectsLocation().equals(ResourceLocation.withDefaultNamespace("overworld"))
            ?ClimateMath.solarTicks(time):time;
    }
}
