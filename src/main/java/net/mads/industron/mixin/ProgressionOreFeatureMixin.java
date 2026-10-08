package net.mads.industron.mixin;

import net.mads.industron.progression.ProgressionMaterials;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Safety net for legacy ore features added by datapacks or to biomes outside vanilla tags. */
@Mixin(OreFeature.class)
public abstract class ProgressionOreFeatureMixin {
    @Inject(method="place", at=@At("HEAD"), cancellable=true)
    private void industron$noLegacyOre(FeaturePlaceContext<OreConfiguration> context,
            CallbackInfoReturnable<Boolean> callback) {
        if (context.config().targetStates.stream().anyMatch(target -> ProgressionMaterials.isLegacyOre(target.state)))
            callback.setReturnValue(false);
    }
}
