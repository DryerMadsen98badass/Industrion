package net.mads.industron.material.structure;

import net.mads.industron.kinetics.shaft.AbstractMaterialShaftBlock;
import net.mads.industron.kinetics.shaft.ShaftLimits;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Wood shaft: intentionally early-game limited and incompatible with Steam Engines. */
public final class StructureWoodShaftBlock extends AbstractMaterialShaftBlock {
    private final WoodMaterial material;

    public StructureWoodShaftBlock(WoodMaterial material, BlockBehaviour.Properties properties) {
        super(properties, ShaftLimits.WOOD, false);
        this.material = material;
    }

    public WoodMaterial material() {
        return material;
    }
}
