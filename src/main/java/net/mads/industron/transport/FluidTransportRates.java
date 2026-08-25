package net.mads.industron.transport;

import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.material.MaterialLookup;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidTransportRates {
    /** Create's unmodified formula is RPM / 2, or 0.5 mB per RPM per tick. */
    private static final double VANILLA_CREATE_PUMP_RATE = 0.5D;

    private FluidTransportRates() {
    }

    public static double pumpRate(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof TieredFluidPump pump) {
            return pump.transportTier().pumpRate();
        }
        if (block instanceof PumpBlock) {
            return FluidTransportTiers.CREATE_PUMP_RATE;
        }
        return 0.0D;
    }

    public static float scalePumpPressure(PumpBlockEntity pump, float vanillaPressure) {
        double configuredRate = pumpRate(pump.getBlockState());
        if (configuredRate <= 0.0D) {
            return vanillaPressure;
        }

        double scaled = vanillaPressure * configuredRate / VANILLA_CREATE_PUMP_RATE;
        if (!Double.isFinite(scaled)) {
            return Float.MAX_VALUE;
        }
        return (float) Math.min(Float.MAX_VALUE, scaled);
    }

    public static int pipeRate(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof TieredFluidPipe pipe) {
            return pipe.transportTier().maximumPipeRate();
        }
        if (block instanceof FluidPipeBlock || block instanceof GlassFluidPipeBlock || block instanceof EncasedPipeBlock) {
            return FluidTransportTiers.CREATE_PIPE_RATE;
        }
        return Integer.MAX_VALUE;
    }

    public static int maxFluidTemperature(BlockState state) {
        if (state.getBlock() instanceof TieredFluidPipe pipe) {
            return pipe.transportTier().maxFluidTemperature();
        }
        return Integer.MAX_VALUE;
    }

    public static int maxPressure(BlockState state) {
        if (state.getBlock() instanceof TieredFluidPipe pipe) {
            return pipe.transportTier().maxPressure();
        }
        return Integer.MAX_VALUE;
    }

    public static int minChemicalRange(BlockState state) {
        if (state.getBlock() instanceof TieredFluidPipe pipe) {
            return pipe.transportTier().minChemicalRange();
        }
        return Integer.MIN_VALUE;
    }

    public static int maxChemicalRange(BlockState state) {
        if (state.getBlock() instanceof TieredFluidPipe pipe) {
            return pipe.transportTier().maxChemicalRange();
        }
        return Integer.MAX_VALUE;
    }

    public static boolean canTransport(BlockState state, FluidStack stack) {
        if (stack == null || stack.isEmpty() || !(state.getBlock() instanceof TieredFluidPipe pipe)) {
            return true;
        }
        return canTransport(pipe.transportTier(), stack);
    }

    /** Shared material compatibility rule for both tiered pipes and tiered tanks. */
    public static boolean canTransport(FluidTransportTier tier, FluidStack stack) {
        if (tier == null || stack == null || stack.isEmpty()) {
            return true;
        }

        int temperature = fluidTemperatureC(stack);
        if (temperature > tier.maxFluidTemperature()) {
            return false;
        }

        Integer chemicalBalance = fluidChemicalBalance(stack);
        return chemicalBalance == null
                || chemicalBalance >= tier.minChemicalRange() && chemicalBalance <= tier.maxChemicalRange();
    }

    private static int fluidTemperatureC(FluidStack stack) {
        IndustrialFluid industrial = IndustrialFluidLookup.find(stack);
        if (industrial != null) {
            return industrial.temperature();
        }
        if (stack.getFluid() == Fluids.LAVA || stack.getFluid() == Fluids.FLOWING_LAVA) {
            return 1300;
        }
        if (stack.getFluid() == Fluids.WATER || stack.getFluid() == Fluids.FLOWING_WATER) {
            return 20;
        }
        // Unknown third-party fluids are allowed until they expose an Industron fluid definition.
        return 20;
    }

    private static Integer fluidChemicalBalance(FluidStack stack) {
        MaterialLookup.MaterialTarget materialTarget = MaterialLookup.find(stack);
        if (materialTarget != null) {
            return materialTarget.material().properties().acidity();
        }

        IndustrialFluid industrial = IndustrialFluidLookup.find(stack);
        if (industrial == null || !industrial.hasChemicalBalance()) {
            return null;
        }

        return (int) Math.round(industrial.chemicalBalance());
    }

    public static boolean isOpenAt(BlockState state, Direction direction) {
        Block block = state.getBlock();
        if (block instanceof FluidPipeBlock) {
            return FluidPipeBlock.isOpenAt(state, direction);
        }
        if (block instanceof GlassFluidPipeBlock) {
            return AxisPipeBlock.isOpenAt(state, direction);
        }
        if (block instanceof EncasedPipeBlock) {
            return state.getValue(EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(direction));
        }
        if (block instanceof PumpBlock) {
            return PumpBlock.isOpenAt(state, direction);
        }
        return false;
    }

    public static boolean isPipe(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FluidPipeBlock
                || block instanceof GlassFluidPipeBlock
                || block instanceof EncasedPipeBlock;
    }

    public static boolean isPump(BlockState state) {
        return state.getBlock() instanceof PumpBlock;
    }

    public static boolean isPipeOrPump(BlockState state) {
        return isPipe(state) || isPump(state);
    }
}
