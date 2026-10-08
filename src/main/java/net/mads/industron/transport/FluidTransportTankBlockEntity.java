package net.mads.industron.transport;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.mads.industron.material.FluidTransportLimits;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class FluidTransportTankBlockEntity extends FluidTankBlockEntity {
    public FluidTransportTankBlockEntity(BlockPos pos, BlockState state) {
        this(blockEntityType(state), pos, state);
    }

    public FluidTransportTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidTransportTier transportTier() {
        if (getBlockState().getBlock() instanceof TieredFluidTank tank) {
            return tank.transportTier();
        }
        throw new IllegalStateException("Fluid transport tank block entity is attached to an invalid block");
    }

    @Override
    protected SmartFluidTank createInventory() {
        SmartFluidTank tank = new SmartFluidTank(tankCapacity(), this::onFluidStackChanged);
        tank.setValidator(stack -> FluidTransportRates.canTransport(transportTier(), stack));
        return tank;
    }

    @Override
    public void applyFluidTankSize(int blocks) {
        int capacity = FluidTransportLimits.tankCapacity(tankCapacity(), blocks);
        tankInventory.setCapacity(capacity);
        int overflow = tankInventory.getFluidAmount() - capacity;
        if (overflow > 0) {
            tankInventory.drain(overflow, FluidAction.EXECUTE);
        }
        forceFluidLevelUpdate = true;
    }

    @Override
    public int getTankSize(int tank) {
        return tankCapacity();
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyFluidTankSize(blocks);
    }

    public IFluidHandler fluidCapability() {
        if (fluidCapability != null) {
            return fluidCapability;
        }
        FluidTankBlockEntity controller = getControllerBE();
        return controller == null ? getTankInventory() : controller.getTankInventory();
    }

    /** Breaking any connected tank block discards the fluid stored by that tank multiblock. */
    public void discardStoredFluid() {
        if (level == null || level.isClientSide()) return;
        IFluidHandler storage = fluidCapability();
        if (storage != null) {
            storage.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        }
        setChanged();
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        CompoundTag savedTankContent = compound.contains("TankContent")
                ? compound.getCompound("TankContent").copy()
                : null;

        super.read(compound, registries, clientPacket);

        if (!isController()) {
            return;
        }

        int capacity = FluidTransportLimits.tankCapacity(tankCapacity(), getTotalTankSize());
        tankInventory.setCapacity(capacity);
        if (savedTankContent != null) {
            tankInventory.readFromNBT(registries, savedTankContent);
            if (!FluidTransportRates.canTransport(transportTier(), tankInventory.getFluid())) {
                tankInventory.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
            }
            int overflow = tankInventory.getFluidAmount() - capacity;
            if (overflow > 0) {
                tankInventory.drain(overflow, FluidAction.EXECUTE);
            }
        }
        if (clientPacket) {
            setFluidLevel(LerpedFloat.linear().startWithValue(getFillState()));
        }
    }

    private int tankCapacity() {
        if (getBlockState().getBlock() instanceof TieredFluidTank tank) {
            return tank.transportTier().tankCapacity();
        }
        throw new IllegalStateException("Fluid transport tank block entity is attached to an invalid block");
    }

    private static BlockEntityType<?> blockEntityType(BlockState state) {
        if (state.getBlock() instanceof FluidTransportTankBlock tank) {
            return tank.getBlockEntityType();
        }
        throw new IllegalArgumentException("Fluid transport tank block entity received an invalid block state: " + state);
    }
}
