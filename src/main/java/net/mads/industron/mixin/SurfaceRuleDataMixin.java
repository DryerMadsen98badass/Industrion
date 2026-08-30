package net.mads.industron.mixin;

import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Removes vanilla surface-rule geology stones that bypass placed-feature removal.
 *
 * <p>Calcite and sandstone layers become normal stone. Basalt-delta blackstone becomes basalt.
 * The blocks themselves remain registered and may still be placed by Industron geology or structures.</p>
 */
@Mixin(SurfaceRuleData.class)
public abstract class SurfaceRuleDataMixin {
    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/Blocks;CALCITE:Lnet/minecraft/world/level/block/Block;"
            )
    )
    private static Block industron$replaceNaturalCalcite() {
        return Blocks.STONE;
    }


    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/Blocks;SANDSTONE:Lnet/minecraft/world/level/block/Block;"
            )
    )
    private static Block industron$replaceNaturalSandstone() {
        return Blocks.STONE;
    }

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/Blocks;RED_SANDSTONE:Lnet/minecraft/world/level/block/Block;"
            )
    )
    private static Block industron$replaceNaturalRedSandstone() {
        return Blocks.STONE;
    }

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/Blocks;BLACKSTONE:Lnet/minecraft/world/level/block/Block;"
            )
    )
    private static Block industron$replaceNaturalBlackstone() {
        return Blocks.BASALT;
    }
}
