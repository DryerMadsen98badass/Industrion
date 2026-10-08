package net.mads.industron.mixin;

import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Noise veins bypass placed features; disable their construction rather than scan generated chunks. */
@Mixin(NoiseChunk.class)
public abstract class ProgressionNoiseVeinsMixin {
    @Redirect(method="<init>", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;oreVeinsEnabled()Z"))
    private boolean industron$noLegacyNoiseVeins(NoiseGeneratorSettings settings) {
        return false;
    }
}
