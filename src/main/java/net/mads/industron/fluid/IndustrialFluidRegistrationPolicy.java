package net.mads.industron.fluid;

import java.util.List;

public final class IndustrialFluidRegistrationPolicy {
    private IndustrialFluidRegistrationPolicy() {
    }

    /** Only these definitions must create a FluidType, source, flowing fluid and bucket. */
    public static List<IndustrialFluid> generatedFluids() {
        return IndustrialFluids.ALL.stream()
                .filter(IndustrialFluidLookup::shouldRegister)
                .toList();
    }

    /** These definitions point at fluids and buckets already provided by Minecraft or another mod. */
    public static List<IndustrialFluid> existingFluids() {
        return IndustrialFluids.ALL.stream()
                .filter(IndustrialFluid::hasExistingFluid)
                .toList();
    }
}
