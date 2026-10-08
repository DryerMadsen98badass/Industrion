package net.mads.industron.material;

import net.minecraft.world.level.block.SlabBlock;

public final class MaterialSlabBlock extends SlabBlock implements MaterialPartBlock {
    private final IndustrialMaterial material;
    private final MaterialPart part;

    public MaterialSlabBlock(IndustrialMaterial material, MaterialPart part) {
        super(MaterialBlock.propertiesFor(material, part));
        this.material = material;
        this.part = part;
    }

    @Override public IndustrialMaterial material() { return material; }
    @Override public MaterialPart part() { return part; }
}
