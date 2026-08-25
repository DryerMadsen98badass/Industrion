package net.mads.industron.client;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.mads.industron.Industron;
import net.mads.industron.transport.FluidTransportTier;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public final class IndustronSpriteShifts {
    private static final Map<String, TankSpriteShifts> FLUID_TANKS = new LinkedHashMap<>();

    static {
        for (FluidTransportTier tier : FluidTransportTier.all()) {
            FLUID_TANKS.put(
                    tier.id(),
                    new TankSpriteShifts(
                            tankShift(tier.id() + "_fluid_tank"),
                            tankShift(tier.id() + "_fluid_tank_top"),
                            tankShift(tier.id() + "_fluid_tank_inner")
                    )
            );
        }
    }

    private IndustronSpriteShifts() {
    }

    public static TankSpriteShifts fluidTank(FluidTransportTier tier) {
        TankSpriteShifts shifts = FLUID_TANKS.get(tier.id());
        if (shifts == null) {
            throw new IllegalArgumentException("Missing fluid tank sprite shifts for tier " + tier.id());
        }
        return shifts;
    }

    private static CTSpriteShiftEntry tankShift(String texture) {
        return CTSpriteShifter.getCT(
                AllCTTypes.RECTANGLE,
                blockTexture(texture),
                blockTexture(texture + "_connected")
        );
    }

    private static ResourceLocation blockTexture(String texture) {
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "block/" + texture);
    }

    public static void init() {
        // Forces all tier sprite shifts to be registered before model baking.
    }

    public record TankSpriteShifts(
            CTSpriteShiftEntry side,
            CTSpriteShiftEntry top,
            CTSpriteShiftEntry inner
    ) {
    }
}
