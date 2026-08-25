package net.mads.industron.machine.runtime;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public record CERecipeExecution(
        ResourceLocation recipeId,
        ResourceLocation recipeType,
        int duration,
        int resourcePerTick,
        int parallel,
        List<ItemStack> itemInputs,
        List<FluidStack> fluidInputs,
        List<ItemStack> itemOutputs,
        List<FluidStack> fluidOutputs,
        CompoundTag machineData
) {
    public CERecipeExecution {
        duration = Math.max(1, duration);
        parallel = Math.max(1, parallel);
        itemInputs = copyItems(itemInputs);
        fluidInputs = copyFluids(fluidInputs);
        itemOutputs = copyItems(itemOutputs);
        fluidOutputs = copyFluids(fluidOutputs);
        machineData = machineData == null ? new CompoundTag() : machineData.copy();
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("RecipeId", recipeId.toString());
        tag.putString("RecipeType", recipeType.toString());
        tag.putInt("Duration", duration);
        tag.putInt("ResourcePerTick", resourcePerTick);
        tag.putInt("Parallel", parallel);
        tag.put("ItemInputs", saveItems(itemInputs, registries));
        tag.put("FluidInputs", saveFluids(fluidInputs, registries));
        tag.put("ItemOutputs", saveItems(itemOutputs, registries));
        tag.put("FluidOutputs", saveFluids(fluidOutputs, registries));
        tag.put("MachineData", machineData.copy());
        return tag;
    }

    public static CERecipeExecution load(CompoundTag tag, HolderLookup.Provider registries) {
        return new CERecipeExecution(
                ResourceLocation.parse(tag.getString("RecipeId")),
                ResourceLocation.parse(tag.getString("RecipeType")),
                tag.getInt("Duration"),
                tag.getInt("ResourcePerTick"),
                tag.getInt("Parallel"),
                loadItems(tag.getList("ItemInputs", Tag.TAG_COMPOUND), registries),
                loadFluids(tag.getList("FluidInputs", Tag.TAG_COMPOUND), registries),
                loadItems(tag.getList("ItemOutputs", Tag.TAG_COMPOUND), registries),
                loadFluids(tag.getList("FluidOutputs", Tag.TAG_COMPOUND), registries),
                tag.getCompound("MachineData")
        );
    }

    private static List<ItemStack> copyItems(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private static List<FluidStack> copyFluids(List<FluidStack> stacks) {
        return stacks.stream().map(FluidStack::copy).toList();
    }

    private static ListTag saveItems(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            list.add(stack.saveOptional(registries));
        }
        return list;
    }

    private static List<ItemStack> loadItems(ListTag list, HolderLookup.Provider registries) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static ListTag saveFluids(List<FluidStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (FluidStack stack : stacks) {
            list.add(stack.saveOptional(registries));
        }
        return list;
    }

    private static List<FluidStack> loadFluids(ListTag list, HolderLookup.Provider registries) {
        List<FluidStack> stacks = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            FluidStack stack = FluidStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }
}
