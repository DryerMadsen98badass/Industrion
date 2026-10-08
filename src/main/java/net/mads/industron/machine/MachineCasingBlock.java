package net.mads.industron.machine;

import net.mads.industron.block.loot.AssemblySalvageBlock;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MachineCasingBlock extends Block implements IWrenchable, AssemblySalvageBlock {
    private final MachineTier tier;

    public MachineCasingBlock(MachineTier tier) {
        super(BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL));
        this.tier = tier;
    }

    public MachineTier tier() {
        return tier;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return WrenchPickupHelper.pickup(this, state, context);
    }
}
