package net.mads.industron.compat.create;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class BlazeBurnerFuelHandler {
    public static final int FLUID_CAPACITY = 4000;

    private BlazeBurnerFuelHandler() {
    }

    public static boolean tryConsumeItemFuel(BlazeBurnerBlockEntity burner, ItemStack stack, boolean simulate) {
        return false;
    }

    public static void tryConsumeBufferedFluid(BlazeBurnerBlockEntity burner) {
    }

    public static IFluidHandler fluidCapability(BlazeBurnerBlockEntity burner) {
        return new FluidInputHandler();
    }

    public static FluidStack getFluidBuffer(BlazeBurnerBlockEntity burner) {
        CEBlazeBurnerExtension extension = CEBlazeBurnerExtension.tryOf(burner);
        return extension == null ? FluidStack.EMPTY : extension.createExpansion$getFluidBuffer();
    }

    public static void setFluidBuffer(BlazeBurnerBlockEntity burner, FluidStack stack) {
        CEBlazeBurnerExtension extension = CEBlazeBurnerExtension.tryOf(burner);
        if (extension != null) {
            extension.createExpansion$setFluidBuffer(stack);
        }
    }

    private static final class FluidInputHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? FLUID_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}