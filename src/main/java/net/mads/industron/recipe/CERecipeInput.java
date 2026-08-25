package net.mads.industron.recipe;

import net.mads.industron.machine.MachineDrive;
import net.mads.industron.machine.MachineTier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record CERecipeInput(
        List<ItemStack> items,
        List<FluidStack> fluids,
        Optional<Integer> circuit,
        Set<ResourceLocation> availableLogic,
        Optional<MachineTier> machineTier,
        Optional<MachineTier> kineticTier,
        Optional<MachineTier> energyTier,
        MachineDrive drive,
        int rpm,
        int coilHeat
) implements RecipeInput {
    public CERecipeInput {
        items = List.copyOf(items);
        fluids = List.copyOf(fluids);
        circuit = circuit == null ? Optional.empty() : circuit;
        availableLogic = Set.copyOf(availableLogic);
        machineTier = machineTier == null ? Optional.empty() : machineTier;
        kineticTier = kineticTier == null ? Optional.empty() : kineticTier;
        energyTier = energyTier == null ? Optional.empty() : energyTier;
        drive = Objects.requireNonNullElse(drive, MachineDrive.NONE);
    }

    public static CERecipeInput of(List<ItemStack> items, List<FluidStack> fluids) {
        return new CERecipeInput(
                items,
                fluids,
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                MachineDrive.NONE,
                0,
                0
        );
    }

    /**
     * Tier supplied by the machine's actual runtime power path.
     */
    public Optional<MachineTier> processingTier() {
        Optional<MachineTier> selected = switch (drive) {
            case ELECTRIC -> energyTier.or(() -> machineTier);
            case KINETIC -> kineticTier.or(() -> machineTier);
            case KINETIC_OUTPUT, STEAM, NONE -> machineTier;
        };
        return selected.filter(tier -> tier != MachineTier.NONE);
    }

    public boolean usesKinetic() {
        return drive.usesKinetic();
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty) && fluids.stream().allMatch(FluidStack::isEmpty);
    }
}
