package net.mads.industron.material.structure;

import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A normal vanilla-behaviour chest whose material/texture is supplied by WoodMaterial. */
public final class StructureWoodChestBlock extends ChestBlock {
    private final WoodMaterial material;

    public StructureWoodChestBlock(WoodMaterial material, BlockBehaviour.Properties properties) {
        super(properties, () -> BlockEntityRegistry.STRUCTURE_WOOD_CHEST.get());
        this.material = material;
    }

    public WoodMaterial material() {
        return material;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructureWoodChestBlockEntity(pos, state);
    }
}
