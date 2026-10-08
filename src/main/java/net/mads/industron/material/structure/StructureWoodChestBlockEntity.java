package net.mads.industron.material.structure;

import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One shared block-entity type supports every generated WoodMaterial chest. */
public final class StructureWoodChestBlockEntity extends ChestBlockEntity {
    public StructureWoodChestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.STRUCTURE_WOOD_CHEST.get(), pos, state);
    }

    public WoodMaterial material() {
        return getBlockState().getBlock() instanceof StructureWoodChestBlock chest
                ? chest.material()
                : null;
    }
}
