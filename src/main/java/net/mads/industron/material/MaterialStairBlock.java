package net.mads.industron.material;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;

public final class MaterialStairBlock extends StairBlock implements MaterialPartBlock {
    private final IndustrialMaterial material;
    private final MaterialPart part;

    public MaterialStairBlock(IndustrialMaterial material, MaterialPart part) {
        super(Blocks.BRICKS.defaultBlockState(), MaterialBlock.propertiesFor(material, part));
        this.material = material;
        this.part = part;
    }

    @Override public IndustrialMaterial material() { return material; }
    @Override public MaterialPart part() { return part; }
}
