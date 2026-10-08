package net.mads.industron.material.organism.cooking;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Extra fuel state attached to vanilla campfire block entities by the existing campfire mixin. */
public interface CampfireFuelAccess {
    NonNullList<ItemStack> industron$fuelItems();

    int industron$fuelTicks();

    void industron$setFuelTicks(int ticks);

    int industron$activeFuelSlot();

    void industron$setActiveFuelSlot(int slot);
}
