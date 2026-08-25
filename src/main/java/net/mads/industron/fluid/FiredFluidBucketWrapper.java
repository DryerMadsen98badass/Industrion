package net.mads.industron.fluid;

import net.mads.industron.registry.FluidRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

public class FiredFluidBucketWrapper extends FluidBucketWrapper {
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

        return new ItemStack(firedBucket, stack.getCount());
    }
}
