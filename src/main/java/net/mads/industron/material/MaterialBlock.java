package net.mads.industron.material;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class MaterialBlock extends Block implements MaterialPartBlock {
    private final IndustrialMaterial material;
    private final MaterialPart part;

    public MaterialBlock(IndustrialMaterial material, MaterialPart part) {
        super(propertiesFor(material, part));
        this.material = material;
        this.part = part;
    }

    public IndustrialMaterial material() {
        return material;
    }

    public MaterialPart part() {
        return part;
    }

    static BlockBehaviour.Properties propertiesFor(IndustrialMaterial material, MaterialPart part) {
        if (part == MaterialPart.CLAY_BLOCK) {
            return BlockBehaviour.Properties.of()
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL);
        }

        if (part == MaterialPart.BRICKS
                || part == MaterialPart.FIREBOX
                || part == MaterialPart.BRICK_SLAB
                || part == MaterialPart.BRICK_STAIRS
                || part == MaterialPart.BRICK_WALL) {
            float hardness = ClayMaterialRules.brickDestroyTime(material);
            float blastResistance = Math.max(6.0F, Math.min(60.0F,
                    material.properties().structuralStrength() / 4.0F));
            return BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops()
                    .strength(hardness, blastResistance)
                    .sound(SoundType.DEEPSLATE_BRICKS);
        }

        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL);

        if (part == MaterialPart.FRAME) {
            return properties.noOcclusion();
        }

        return properties;
    }
}
