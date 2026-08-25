package net.mads.industron.mixin;

import com.simibubi.create.content.fluids.FluidNetwork;
import net.createmod.catnip.math.BlockFace;
import net.mads.industron.transport.FluidTransportRates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(FluidNetwork.class)
public abstract class FluidNetworkMixin {
    @Shadow Level world;
    @Shadow BlockFace start;
    @Shadow int transferSpeed;
    @Shadow Set<BlockPos> visited;
    @Shadow FluidStack fluid;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/content/fluids/FluidNetwork;transferSpeed:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int createExpansion$limitTransferSpeedToWeakestPipe(FluidNetwork network) {
        int pipeLimit = createExpansion$pipeRateAt(start == null ? null : start.getPos());

        if (start != null) {
            pipeLimit = Math.min(pipeLimit, createExpansion$pipeRateAt(start.getConnectedPos()));
        }

        if (visited != null) {
            for (BlockPos pos : visited) {
                pipeLimit = Math.min(pipeLimit, createExpansion$pipeRateAt(pos));
            }
        }

        if (fluid != null && !fluid.isEmpty() && !createExpansion$networkCanTransport(fluid)) {
            return 0;
        }

        return pipeLimit == Integer.MAX_VALUE
                ? transferSpeed
                : Math.min(transferSpeed, pipeLimit);
    }

    @Unique
    private int createExpansion$pipeRateAt(BlockPos pos) {
        if (world == null || pos == null || !world.isLoaded(pos)) {
            return Integer.MAX_VALUE;
        }

        BlockState state = world.getBlockState(pos);
        return FluidTransportRates.isPipe(state)
                ? FluidTransportRates.pipeRate(state)
                : Integer.MAX_VALUE;
    }
    @Unique
    private boolean createExpansion$networkCanTransport(FluidStack stack) {
        if (!createExpansion$canTransportAt(start == null ? null : start.getPos(), stack)) {
            return false;
        }
        if (start != null && !createExpansion$canTransportAt(start.getConnectedPos(), stack)) {
            return false;
        }
        if (visited != null) {
            for (BlockPos pos : visited) {
                if (!createExpansion$canTransportAt(pos, stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Unique
    private boolean createExpansion$canTransportAt(BlockPos pos, FluidStack stack) {
        if (world == null || pos == null || !world.isLoaded(pos)) {
            return true;
        }
        BlockState state = world.getBlockState(pos);
        return !FluidTransportRates.isPipe(state) || FluidTransportRates.canTransport(state, stack);
    }

}
