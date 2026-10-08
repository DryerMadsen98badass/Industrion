package net.mads.industron.material.plant;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;

/** Material-specific fertilizer produced by the vanilla Composter. */
public final class PlantFertilizerItem extends PlantProcessIntermediateItem {
    public PlantFertilizerItem(PlantProcessIntermediate intermediate) {
        super(intermediate);
        if (!PlantProcessingPlanner.FERTILIZER.equals(intermediate.suffix())) {
            throw new IllegalArgumentException("PlantFertilizerItem requires a fertilizer intermediate");
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (BoneMealItem.applyBonemeal(
                context.getItemInHand(),
                context.getLevel(),
                context.getClickedPos(),
                context.getPlayer()
        )) {
            if (!context.getLevel().isClientSide()) {
                context.getLevel().levelEvent(1505, context.getClickedPos(), 0);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        return InteractionResult.PASS;
    }
}
