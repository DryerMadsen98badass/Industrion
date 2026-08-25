package net.mads.industron.fluid;

import java.util.List;
import java.util.Optional;

public final class IndustrialFluids {
    public static final IndustrialFluid STEAM = new IndustrialFluid(
            "steam",
            "Steam",
            0xD9D9D9,
            IndustrialFluid.Kind.GAS,
            373,
            1,
            1,
            0,
            Optional.empty(),
            0,
            List.of(),
            Optional.empty()
    );

    public static final List<IndustrialFluid> ALL = List.of(STEAM);

    private IndustrialFluids() {
    }
}
