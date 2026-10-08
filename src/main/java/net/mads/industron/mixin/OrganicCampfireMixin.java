package net.mads.industron.mixin;

import net.mads.industron.material.organism.cooking.CampfireFuelAccess;
import net.mads.industron.material.organism.cooking.CampfireFuelRuntime;
import net.mads.industron.material.organism.cooking.OrganicCampfireAccess;
import net.mads.industron.material.organism.cooking.OrganicCampfireRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public abstract class OrganicCampfireMixin implements OrganicCampfireAccess, CampfireFuelAccess {
    @Shadow @Final private int[] cookingProgress;
    @Shadow @Final private int[] cookingTime;

    @Unique private final NonNullList<ItemStack> industron$fuelItems = NonNullList.withSize(4, ItemStack.EMPTY);
    @Unique private int industron$fuelTicks;
    @Unique private int industron$activeFuelSlot = -1;

    @Override
    public int[] industron$cookingProgress() {
        return cookingProgress;
    }

    @Override
    public int[] industron$cookingTime() {
        return cookingTime;
    }

    @Override
    public NonNullList<ItemStack> industron$fuelItems() {
        return industron$fuelItems;
    }

    @Override
    public int industron$fuelTicks() {
        return industron$fuelTicks;
    }

    @Override
    public void industron$setFuelTicks(int ticks) {
        industron$fuelTicks = Math.max(0, ticks);
    }

    @Override
    public int industron$activeFuelSlot() {
        return industron$activeFuelSlot;
    }

    @Override
    public void industron$setActiveFuelSlot(int slot) {
        industron$activeFuelSlot = slot >= 0 && slot < industron$fuelItems.size() ? slot : -1;
    }

    @Inject(method = "cookTick", at = @At("HEAD"), cancellable = true)
    private static void industron$cook(
            Level level,
            BlockPos pos,
            BlockState state,
            CampfireBlockEntity fire,
            CallbackInfo ci
    ) {
        if (!CampfireFuelRuntime.prepareBurn(level, pos, state, fire)) {
            ci.cancel();
            return;
        }
        if (!OrganicCampfireRuntime.containsOrganic(fire)) return;

        OrganicCampfireRuntime.tick(level, pos, state, fire);
        CampfireFuelRuntime.finishBurn(level, pos, state, fire);
        ci.cancel();
    }

    /** Vanilla cooking reaches this only when the HEAD injection did not replace it. */
    @Inject(method = "cookTick", at = @At("TAIL"))
    private static void industron$finishVanillaCook(
            Level level,
            BlockPos pos,
            BlockState state,
            CampfireBlockEntity fire,
            CallbackInfo ci
    ) {
        CampfireFuelRuntime.finishBurn(level, pos, state, fire);
    }

    @Inject(method = "cooldownTick", at = @At("HEAD"), cancellable = true)
    private static void industron$cool(
            Level level,
            BlockPos pos,
            BlockState state,
            CampfireBlockEntity fire,
            CallbackInfo ci
    ) {
        if (!OrganicCampfireRuntime.containsOrganic(fire)) return;
        var access = (OrganicCampfireAccess) fire;
        for (int i = 0; i < fire.getItems().size(); i++) {
            if (OrganicCampfireRuntime.definition(fire.getItems().get(i)) == null) {
                access.industron$cookingProgress()[i] = Math.max(0, access.industron$cookingProgress()[i] - 2);
            }
        }
        fire.setChanged();
        ci.cancel();
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void industron$saveFuel(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        ListTag fuels = new ListTag();
        for (int slot = 0; slot < industron$fuelItems.size(); slot++) {
            ItemStack stack = industron$fuelItems.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            entry.put("Stack", stack.copyWithCount(1).saveOptional(registries));
            fuels.add(entry);
        }
        if (!fuels.isEmpty()) tag.put("IndustronFuelItems", fuels);
        if (industron$fuelTicks > 0) {
            tag.putInt("IndustronFuelTicks", industron$fuelTicks);
            tag.putInt("IndustronActiveFuelSlot", industron$activeFuelSlot);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void industron$loadFuel(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        for (int i = 0; i < industron$fuelItems.size(); i++) industron$fuelItems.set(i, ItemStack.EMPTY);
        ListTag fuels = tag.getList("IndustronFuelItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < fuels.size(); i++) {
            CompoundTag entry = fuels.getCompound(i);
            int slot = entry.getByte("Slot");
            if (slot < 0 || slot >= industron$fuelItems.size()) continue;
            ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("Stack"));
            if (!stack.isEmpty()) industron$fuelItems.set(slot, stack.copyWithCount(1));
        }
        industron$fuelTicks = Math.max(0, tag.getInt("IndustronFuelTicks"));
        industron$activeFuelSlot = industron$fuelTicks > 0
                ? Math.max(-1, Math.min(industron$fuelItems.size() - 1, tag.getInt("IndustronActiveFuelSlot")))
                : -1;
    }
}
