package net.mads.industron.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.mads.industron.compat.create.BlazeBurnerFuelHandler;
import net.mads.industron.compat.create.CEBlazeBurnerExtension;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlazeBurnerBlockEntity.class)
public abstract class BlazeBurnerBlockEntityMixin implements CEBlazeBurnerExtension {
    @Shadow protected BlazeBurnerBlockEntity.FuelType activeFuel;
    @Shadow protected int remainingBurnTime;
    @Shadow public abstract void updateBlockState();
    @Shadow protected abstract void playSound();

    @Unique private static final int CREATE_EXPANSION_MAX_BURN_TIME = 10000;

    @Unique private int createExpansion$heatedAfterSuperheated;
    @Unique private boolean createExpansion$pendingCustomFuelTransition;
    @Unique private FluidStack createExpansion$fluidBuffer = FluidStack.EMPTY;

    // Create returns from tick() early while fuel is still burning, so buffered fuel must be
    // checked at HEAD. A TAIL injection only runs after the current fuel has already expired.
    @Inject(method = "tick", at = @At("HEAD"))
    private void createExpansion$refuelCustomFluid(CallbackInfo ci) {
        BlazeBurnerFuelHandler.tryConsumeBufferedFluid((BlazeBurnerBlockEntity) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void createExpansion$finishCustomFuelTransition(CallbackInfo ci) {
        if (createExpansion$pendingCustomFuelTransition
                && activeFuel == BlazeBurnerBlockEntity.FuelType.NORMAL
                && remainingBurnTime == 5000) {
            createExpansion$pendingCustomFuelTransition = false;
            if (createExpansion$heatedAfterSuperheated > 0) {
                remainingBurnTime = createExpansion$heatedAfterSuperheated;
            } else {
                activeFuel = BlazeBurnerBlockEntity.FuelType.NONE;
                remainingBurnTime = 0;
            }
            createExpansion$heatedAfterSuperheated = 0;
            updateBlockState();
        }
    }

    @Inject(method = "tryUpdateFuel", at = @At("HEAD"), cancellable = true)
    private void createExpansion$tryCustomItemFuel(
            ItemStack stack,
            boolean forceOverflow,
            boolean simulate,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (BlazeBurnerFuelHandler.tryConsumeItemFuel((BlazeBurnerBlockEntity) (Object) this, stack, simulate)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void createExpansion$write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        tag.putInt("CEHeatedAfterSuperheated", createExpansion$heatedAfterSuperheated);
        tag.putBoolean("CEPendingCustomFuelTransition", createExpansion$pendingCustomFuelTransition);
        if (!createExpansion$fluidBuffer.isEmpty()) {
            tag.put("CEFluidBuffer", createExpansion$fluidBuffer.save(registries));
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void createExpansion$read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        createExpansion$heatedAfterSuperheated = tag.getInt("CEHeatedAfterSuperheated");
        createExpansion$pendingCustomFuelTransition = tag.getBoolean("CEPendingCustomFuelTransition");
        createExpansion$fluidBuffer = tag.contains("CEFluidBuffer")
                ? FluidStack.parseOptional(registries, tag.getCompound("CEFluidBuffer"))
                : FluidStack.EMPTY;
    }

    @Override
    public int createExpansion$getHeatedAfterSuperheated() {
        return createExpansion$heatedAfterSuperheated;
    }

    @Override
    public void createExpansion$setHeatedAfterSuperheated(int ticks) {
        createExpansion$heatedAfterSuperheated = Math.max(0, ticks);
    }

    @Override
    public boolean createExpansion$hasPendingCustomFuelTransition() {
        return createExpansion$pendingCustomFuelTransition;
    }

    @Override
    public void createExpansion$setPendingCustomFuelTransition(boolean pending) {
        createExpansion$pendingCustomFuelTransition = pending;
    }

    @Override
    public FluidStack createExpansion$getFluidBuffer() {
        return createExpansion$fluidBuffer.copy();
    }

    @Override
    public void createExpansion$setFluidBuffer(FluidStack stack) {
        createExpansion$fluidBuffer = stack.isEmpty() ? FluidStack.EMPTY : stack.copy();
    }

    @Override
    public void createExpansion$applyCustomFuel(int superheatedTicks, int heatedTicks) {
        createExpansion$heatedAfterSuperheated = Math.max(0, heatedTicks);
        createExpansion$pendingCustomFuelTransition = superheatedTicks > 0;
        if (superheatedTicks > 0) {
            activeFuel = BlazeBurnerBlockEntity.FuelType.SPECIAL;
            remainingBurnTime = superheatedTicks;
        } else {
            activeFuel = BlazeBurnerBlockEntity.FuelType.NORMAL;
            remainingBurnTime = Math.max(0, heatedTicks);
        }
        updateBlockState();
        playSound();
    }

    @Override
    public void createExpansion$addCustomFuel(int superheatedTicks, int heatedTicks) {
        if (superheatedTicks > 0) {
            int existingSuperheatedTicks = activeFuel == BlazeBurnerBlockEntity.FuelType.SPECIAL
                    ? Math.max(0, remainingBurnTime)
                    : 0;

            createExpansion$heatedAfterSuperheated = Math.max(0, heatedTicks);
            createExpansion$pendingCustomFuelTransition = true;
            activeFuel = BlazeBurnerBlockEntity.FuelType.SPECIAL;
            remainingBurnTime = existingSuperheatedTicks + Math.max(0, superheatedTicks);
        } else {
            int existingHeatedTicks = activeFuel == BlazeBurnerBlockEntity.FuelType.NORMAL
                    ? Math.max(0, remainingBurnTime)
                    : 0;
            int combinedHeatedTicks = Math.min(
                    CREATE_EXPANSION_MAX_BURN_TIME,
                    existingHeatedTicks + Math.max(0, heatedTicks)
            );

            activeFuel = BlazeBurnerBlockEntity.FuelType.NORMAL;
            remainingBurnTime = combinedHeatedTicks;
            createExpansion$heatedAfterSuperheated = 0;
            createExpansion$pendingCustomFuelTransition = false;
        }
        updateBlockState();
        playSound();
    }
}
