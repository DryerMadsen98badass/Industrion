package net.mads.industron.fluid;

import net.mads.industron.machine.foundry.FoundryComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

/** Bucket item identity alone cannot retain the composition of a runtime mixture. */
public class FoundryBucketWrapper extends FluidBucketWrapper {
    public FoundryBucketWrapper(ItemStack stack) { super(stack); }
    @Override public FluidStack getFluid() {
        FluidStack result = super.getFluid();
        if (!result.isEmpty()) {
            var ratio = container.get(FoundryComponents.COMPOSITION.get());
            var temperature = container.get(FoundryComponents.TEMPERATURE.get());
            if (ratio != null) result.set(FoundryComponents.COMPOSITION.get(), ratio);
            if (temperature != null) result.set(FoundryComponents.TEMPERATURE.get(), temperature);
        }
        return result;
    }
    @Override protected void setFluid(FluidStack fluid) {
        super.setFluid(fluid);
        container.remove(FoundryComponents.COMPOSITION.get());
        container.remove(FoundryComponents.TEMPERATURE.get());
        if (!fluid.isEmpty()) {
            var ratio = fluid.get(FoundryComponents.COMPOSITION.get());
            var temperature = fluid.get(FoundryComponents.TEMPERATURE.get());
            if (ratio != null) container.set(FoundryComponents.COMPOSITION.get(), ratio);
            if (temperature != null) container.set(FoundryComponents.TEMPERATURE.get(), temperature);
        }
    }
}
