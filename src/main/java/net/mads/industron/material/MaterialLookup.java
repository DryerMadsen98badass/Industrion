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
import net.mads.industron.runtime.BoundedIdentityCache;

import java.util.Optional;

public final class MaterialLookup {
    private static final BoundedIdentityCache<Item, Optional<MaterialTarget>> EXISTING_ITEM_CACHE = new BoundedIdentityCache<>(4096);
    private static final BoundedIdentityCache<Item, Optional<MaterialTarget>> BUCKET_CACHE = new BoundedIdentityCache<>(4096);
    private static final BoundedIdentityCache<net.minecraft.world.level.material.Fluid, Optional<MaterialTarget>> MATERIAL_FLUID_CACHE = new BoundedIdentityCache<>(4096);
    private static int itemMaterials = -1, bucketMaterials = -1, bucketFluids = -1, fluidMaterials = -1, fluidCount = -1;

    private MaterialLookup() {
    }

    public static synchronized void clearCaches() {
        EXISTING_ITEM_CACHE.clear(); BUCKET_CACHE.clear(); MATERIAL_FLUID_CACHE.clear();
        itemMaterials = bucketMaterials = bucketFluids = fluidMaterials = fluidCount = -1;
    }

    private static int registeredFluidCount() {
        return FluidRegistry.MATERIAL_FLUIDS.size() + FluidRegistry.CHEMICAL_FLUIDS.size()
                + FluidRegistry.PLANT_PROCESS_FLUIDS.size() + 2;
    }

    public static MaterialTarget find(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof MaterialItem materialItem) {
            return new MaterialTarget(materialItem.material(), materialItem.part(), materialItem.magnetic());
        }

        if (item instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof MaterialPartBlock materialBlock) {
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
    public static synchronized MaterialTarget find(FluidStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        int materials = IndustrialMaterials.ALL.size(), fluids = registeredFluidCount();
        if (fluidMaterials != materials || fluidCount != fluids) {
            MATERIAL_FLUID_CACHE.clear(); fluidMaterials = materials; fluidCount = fluids;
        }
        return MATERIAL_FLUID_CACHE.computeIfAbsent(stack.getFluid(), MaterialLookup::findMaterialFluid).orElse(null);
    }

    private static Optional<MaterialTarget> findMaterialFluid(net.minecraft.world.level.material.Fluid candidate) {
        for (FluidRegistry.RegisteredFluid fluid : FluidRegistry.allFluids()) {
            if (candidate != fluid.source().get() && candidate != fluid.flowing().get()) {
                continue;
            }

            IndustrialMaterial material = materialForFluid(fluid);
            if (material == null) {
                continue;
            }

            return Optional.of(new MaterialTarget(material, fluidPart(fluid), false));
        }

        return Optional.empty();
    }

    private static synchronized MaterialTarget findExistingMaterialPart(Item item) {
        int materials = IndustrialMaterials.ALL.size();
        if (itemMaterials != materials) {
            EXISTING_ITEM_CACHE.clear(); itemMaterials = materials;
        }
        return EXISTING_ITEM_CACHE.computeIfAbsent(item, MaterialLookup::existingMaterialPart).orElse(null);
    }

    private static Optional<MaterialTarget> existingMaterialPart(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            for (MaterialPart part : material.parts()) {
                if (itemId.equals(material.existingParts().get(part))) {
                    return Optional.of(new MaterialTarget(material, part, false));
                }
            }
        }

        return Optional.empty();
    }

    private static synchronized MaterialTarget findMoltenBucket(ItemStack stack) {
        int materials = IndustrialMaterials.ALL.size(), fluids = registeredFluidCount();
        if (bucketMaterials != materials || bucketFluids != fluids) {
            BUCKET_CACHE.clear(); bucketMaterials = materials; bucketFluids = fluids;
        }
        return BUCKET_CACHE.computeIfAbsent(stack.getItem(), MaterialLookup::materialBucket).orElse(null);
    }

    private static Optional<MaterialTarget> materialBucket(Item item) {
        for (FluidRegistry.RegisteredFluid fluid : FluidRegistry.allFluids()) {
            if (item != fluid.bucket().get()) {
                continue;
            }

            IndustrialMaterial material = materialForFluid(fluid);
            if (material != null) {
                return Optional.of(new MaterialTarget(material, fluidPart(fluid), false));
            }
        }

        return Optional.empty();
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
