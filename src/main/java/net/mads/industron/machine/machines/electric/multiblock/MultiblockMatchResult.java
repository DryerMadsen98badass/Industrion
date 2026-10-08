package net.mads.industron.machine.machines.electric.multiblock;

import net.mads.industron.machine.MachineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record MultiblockMatchResult(
        boolean matched,
        String variant,
        int variantLevel,
        MachineTier tier,
        int coilHeat,
        int coilCount,
        ResourceLocation casingModel,
        List<BlockPos> positions,
        Map<MultiblockAbility, List<BlockPos>> abilityPositions,
        Map<Integer, BlockPos> sequentialInputPositions,
        Map<Integer, BlockPos> sequentialOutputPositions,
        Map<BlockPos, ResourceLocation> overlayModels
) {
    public static MultiblockMatchResult failed() {
        return new MultiblockMatchResult(
                false,
                "",
                0,
                null,
                0,
                0,
                null,
                List.<BlockPos>of(),
                Map.<MultiblockAbility, List<BlockPos>>of(),
                Map.<Integer, BlockPos>of(),
                Map.<Integer, BlockPos>of(),
                Map.<BlockPos, ResourceLocation>of()
        );
    }
}
