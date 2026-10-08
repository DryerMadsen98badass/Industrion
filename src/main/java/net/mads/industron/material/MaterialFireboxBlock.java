package net.mads.industron.material;

import net.mads.industron.machine.FireboxBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Active firebox form generated for every registered clay material. */
public final class MaterialFireboxBlock extends FireboxBlock implements MaterialPartBlock {
    private final IndustrialMaterial material;

    public MaterialFireboxBlock(IndustrialMaterial material) {
        super(properties(material));
        if (material == null || !material.isClayMaterial()) {
            throw new IllegalArgumentException("Material firebox requires a clay material");
        }
        this.material = material;
    }

    private static BlockBehaviour.Properties properties(IndustrialMaterial material) {
        return MaterialBlock.propertiesFor(material, MaterialPart.BRICKS)
                .lightLevel(state -> state.getValue(ACTIVE) ? 12 : 0);
    }

    @Override
    public IndustrialMaterial material() {
        return material;
    }

    @Override
    public MaterialPart part() {
        return MaterialPart.FIREBOX;
    }
}
