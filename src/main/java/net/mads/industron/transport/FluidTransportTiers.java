package net.mads.industron.transport;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialProperties;

import static net.mads.industron.transport.FluidTransportTier.transportTier;

public final class FluidTransportTiers {
    /** Create's configured baseline pump rate in mB per RPM, per tick. */
    public static final double CREATE_PUMP_RATE = 4.0D;

    /** Create pipe limit, derived from Create's pump at the maximum 256 RPM. */
    public static final int CREATE_PIPE_RATE = Math.max(1, (int) Math.floor(CREATE_PUMP_RATE * 256.0D));

    private static boolean bootstrapped;

    private FluidTransportTiers() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            // Fluid transport structures are a metal-only system. Gems and
            // non-metals never receive pipes, pumps or tanks. The metal must
            // also still be solid at the 20 C reference temperature.
            if (!material.properties().metal()
                    || material.properties().meltingPoint() <= 20
                    || material.properties().state() != MaterialProperties.PhysicalState.SOLID) {
                continue;
            }

            transportTier(material.id(), material.displayName())
                    .material(material)
                    .build();
        }
    }
}
