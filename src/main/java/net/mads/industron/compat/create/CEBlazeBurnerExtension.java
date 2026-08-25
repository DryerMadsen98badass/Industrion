package net.mads.industron.compat.create;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

public interface CEBlazeBurnerExtension {
    int createExpansion$getHeatedAfterSuperheated();

    void createExpansion$setHeatedAfterSuperheated(int ticks);

    boolean createExpansion$hasPendingCustomFuelTransition();

    void createExpansion$setPendingCustomFuelTransition(boolean pending);

    FluidStack createExpansion$getFluidBuffer();

    void createExpansion$setFluidBuffer(FluidStack stack);

    void createExpansion$applyCustomFuel(int superheatedTicks, int heatedTicks);

    void createExpansion$addCustomFuel(int superheatedTicks, int heatedTicks);

    static CEBlazeBurnerExtension of(BlazeBurnerBlockEntity burner) {
        return (CEBlazeBurnerExtension) burner;
    }

    static CEBlazeBurnerExtension tryOf(BlazeBurnerBlockEntity burner) {
        return burner instanceof CEBlazeBurnerExtension extension ? extension : null;
    }
}
