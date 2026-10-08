package net.mads.industron.fluid;

import net.mads.industron.registry.FluidRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

public class FiredFluidBucketWrapper extends FoundryBucketWrapper {
    public FiredFluidBucketWrapper(ItemStack container) {
        super(container);
    }

    @Override
    protected void setFluid(FluidStack fluidStack) {
        super.setFluid(fluidStack);
        container = convertNormalBucketToFired(container);
    }

    private static ItemStack convertNormalBucketToFired(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }

        FluidRegistry.buildFiredBucketMaps();

        Item firedBucket = FluidRegistry.FIRED_BUCKET_BY_NORMAL_BUCKET.get(stack.getItem());
        if (firedBucket == null) {
            return stack;
        }

        ItemStack result = new ItemStack(firedBucket, stack.getCount());
        var ratio = stack.get(net.mads.industron.machine.foundry.FoundryComponents.COMPOSITION.get());
        var temperature = stack.get(net.mads.industron.machine.foundry.FoundryComponents.TEMPERATURE.get());
        if (ratio != null) result.set(net.mads.industron.machine.foundry.FoundryComponents.COMPOSITION.get(), ratio);
        if (temperature != null) result.set(net.mads.industron.machine.foundry.FoundryComponents.TEMPERATURE.get(), temperature);
        return result;
    }
}
