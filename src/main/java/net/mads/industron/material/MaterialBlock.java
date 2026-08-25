package net.mads.industron.material;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class MaterialBlock extends Block {
    private final IndustrialMaterial material;
    private final MaterialPart part;

    public MaterialBlock(IndustrialMaterial material, MaterialPart part) {
        super(properties(part));
        this.material = material;
        this.part = part;
    }

    public IndustrialMaterial material() {
        return material;
    }

    public MaterialPart part() {
        return part;
    }

    private static BlockBehaviour.Properties properties(MaterialPart part) {
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
