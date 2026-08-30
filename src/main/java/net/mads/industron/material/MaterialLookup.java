package net.mads.industron.material;

import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.registry.FluidRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class MaterialLookup {
    private static final Map<Item, MaterialTarget> EXISTING_ITEM_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<net.minecraft.world.level.material.Fluid, MaterialTarget> MATERIAL_FLUID_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private MaterialLookup() {
    }

    public static MaterialTarget find(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof MaterialItem materialItem) {
            return new MaterialTarget(materialItem.material(), materialItem.part(), materialItem.magnetic());
        }

        if (item instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof MaterialBlock materialBlock) {
            return new MaterialTarget(materialBlock.material(), materialBlock.part(), false);
        }

        if (item instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof EnergyWireBlock wire) {
            return new MaterialTarget(wire.material(), wire.thickness().materialPart(), false);
        }

        MaterialTarget existingTarget = findExistingMaterialPart(item);
        if (existingTarget != null) {
            return existingTarget;
        }

        return findMoltenBucket(stack);
    }

    /** Lookup for normal/gas IndustrialFluid definitions, including existing fluids such as minecraft:water. */
    public static IndustrialFluid findIndustrialFluid(ItemStack stack) {
        return IndustrialFluidLookup.find(stack);
    }

    /** Lookup for normal/gas IndustrialFluid definitions, including existing fluids such as minecraft:water. */
    public static IndustrialFluid findIndustrialFluid(FluidStack stack) {
        return IndustrialFluidLookup.find(stack);
    }

    /** Existing API for molten fluids generated from or mapped onto IndustrialMaterial definitions. */
    public static MaterialTarget find(FluidStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        MaterialTarget cached = MATERIAL_FLUID_CACHE.get(stack.getFluid());
        if (cached != null) {
            return cached;
        }

        for (FluidRegistry.RegisteredFluid fluid : FluidRegistry.allFluids()) {
            if (!stack.is(fluid.source().get()) && !stack.is(fluid.flowing().get())) {
                continue;
            }

            IndustrialMaterial material = materialForFluid(fluid);
            if (material == null) {
                continue;
            }

            MaterialTarget target = new MaterialTarget(material, fluidPart(fluid), false);
            MATERIAL_FLUID_CACHE.put(fluid.source().get(), target);
            MATERIAL_FLUID_CACHE.put(fluid.flowing().get(), target);
            return target;
        }

        return null;
    }

    private static MaterialTarget findExistingMaterialPart(Item item) {
        MaterialTarget cached = EXISTING_ITEM_CACHE.get(item);
        if (cached != null) {
            return cached;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            for (MaterialPart part : material.parts()) {
                if (itemId.equals(material.existingParts().get(part))) {
                    MaterialTarget target = new MaterialTarget(material, part, false);
                    EXISTING_ITEM_CACHE.put(item, target);
                    return target;
                }
            }
        }

        return null;
    }

    private static MaterialTarget findMoltenBucket(ItemStack stack) {
        for (FluidRegistry.RegisteredFluid fluid : FluidRegistry.allFluids()) {
            if (!stack.is(fluid.bucket().get())) {
                continue;
            }

            IndustrialMaterial material = materialForFluid(fluid);
            if (material != null) {
                return new MaterialTarget(material, fluidPart(fluid), false);
            }
        }

        return null;
    }

    private static IndustrialMaterial materialForFluid(FluidRegistry.RegisteredFluid fluid) {
        ResourceLocation sourceId = fluid.source().getId();
        MaterialPart part = fluidPart(fluid);
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (!material.has(part)) continue;
            if (material.hasExistingPart(part)) {
                if (sourceId.equals(material.existingPart(part))) return material;
                continue;
            }
            if (material.id().equals(fluid.definition().id())) return material;
        }
        return null;
    }

    private static MaterialPart fluidPart(FluidRegistry.RegisteredFluid fluid) {
        return switch (fluid.definition().kind()) {
            case LIQUID -> MaterialPart.LIQUID;
            case GAS -> MaterialPart.GAS;
            case MOLTEN -> MaterialPart.MOLTEN_FLUID;
        };
    }

    public record MaterialTarget(IndustrialMaterial material, MaterialPart part, boolean magnetic) {
        public MaterialTarget(IndustrialMaterial material, MaterialPart part) {
            this(material, part, false);
        }
    }
}
