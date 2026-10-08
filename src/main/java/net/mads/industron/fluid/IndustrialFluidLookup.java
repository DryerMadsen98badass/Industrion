package net.mads.industron.fluid;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.ChemicalBalanceRange;
import net.mads.industron.registry.FluidRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.mads.industron.runtime.BoundedIdentityCache;

import java.util.Optional;

public final class IndustrialFluidLookup {
    private static final BoundedIdentityCache<Fluid, Optional<IndustrialFluid>> CACHE = new BoundedIdentityCache<>(4096);
    private static int registeredCount = -1, definitionCount = -1;
    private IndustrialFluidLookup() {
    }

    public static synchronized void clearCache() {
        CACHE.clear(); registeredCount = definitionCount = -1;
    }

    public static boolean shouldRegister(IndustrialFluid definition) {
        return !definition.hasExistingFluid();
    }

    public static ResourceLocation fluidId(IndustrialFluid definition) {
        if (definition.hasExistingFluid()) return definition.existingFluidId();
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, definition.registryName());
    }

    public static ResourceLocation fluidId(IndustrialMaterial material, MaterialPart part) {
        requireFluidPart(material, part);
        if (material.hasExistingPart(part)) return material.existingPart(part);
        return fluidId(materialFluid(material, part));
    }

    /** Compatibility: returns the ambient state fluid when present, otherwise molten. */
    public static ResourceLocation fluidId(IndustrialMaterial material) {
        return fluidId(material, primaryFluidPart(material));
    }

    public static IndustrialFluid materialFluid(IndustrialMaterial material, MaterialPart part) {
        requireFluidPart(material, part);
        return switch (part) {
            case GAS -> materialFluid(material, IndustrialFluid.Kind.GAS,
                    material.properties().ambientTemperature(), -100, 100);
            case LIQUID -> materialFluid(material, IndustrialFluid.Kind.LIQUID,
                    material.properties().ambientTemperature(), Math.max(100, material.properties().density()), 1000);
            case MOLTEN_FLUID -> materialFluid(material, IndustrialFluid.Kind.MOLTEN,
                    material.properties().castTemperature(), Math.max(100, material.properties().density()), 6000);
            default -> throw new IllegalArgumentException("Not a material fluid part: " + part);
        };
    }

    /** Compatibility helper. */
    public static IndustrialFluid materialFluid(IndustrialMaterial material) {
        return materialFluid(material, primaryFluidPart(material));
    }

    public static Fluid fluid(IndustrialFluid definition) {
        return BuiltInRegistries.FLUID.get(fluidId(definition));
    }

    public static synchronized IndustrialFluid find(Fluid fluid) {
        if (fluid == null) return null;
        int registered = FluidRegistry.MATERIAL_FLUIDS.size() + FluidRegistry.CHEMICAL_FLUIDS.size()
                + FluidRegistry.PLANT_PROCESS_FLUIDS.size() + 2;
        int definitions = IndustrialFluids.ALL.size();
        if (registered != registeredCount || definitions != definitionCount) {
            CACHE.clear(); registeredCount = registered; definitionCount = definitions;
        }
        return CACHE.computeIfAbsent(fluid, value -> Optional.ofNullable(findUncached(value))).orElse(null);
    }

    private static IndustrialFluid findUncached(Fluid fluid) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        for (FluidRegistry.RegisteredFluid registered : FluidRegistry.allFluids()) {
            if (registered.source().get() == fluid || registered.flowing().get() == fluid) {
                return registered.definition();
            }
            if (fluidId(registered.definition()).equals(id)) return registered.definition();
        }
        for (IndustrialFluid definition : IndustrialFluids.ALL) {
            if (fluidId(definition).equals(id)) return definition;
        }
        return null;
    }

    public static IndustrialFluid find(FluidStack stack) {
        return stack == null || stack.isEmpty() ? null : find(stack.getFluid());
    }

    public static IndustrialFluid find(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Optional<FluidStack> contained = FluidUtil.getFluidContained(stack);
        return contained.map(IndustrialFluidLookup::find).orElse(null);
    }

    private static MaterialPart primaryFluidPart(IndustrialMaterial material) {
        if (material.has(MaterialPart.GAS)) return MaterialPart.GAS;
        if (material.has(MaterialPart.LIQUID)) return MaterialPart.LIQUID;
        if (material.has(MaterialPart.MOLTEN_FLUID)) return MaterialPart.MOLTEN_FLUID;
        throw new IllegalArgumentException("Material does not define a fluid part: " + material.id());
    }

    private static void requireFluidPart(IndustrialMaterial material, MaterialPart part) {
        if (!part.isFluid() || !material.has(part)) {
            throw new IllegalArgumentException("Material does not define fluid part " + part + ": " + material.id());
        }
    }

    private static IndustrialFluid materialFluid(
            IndustrialMaterial material,
            IndustrialFluid.Kind kind,
            int temperature,
            int density,
            int viscosity
    ) {
        int acidity = Math.max(-100, Math.min(100, material.properties().acidity()));
        Optional<Integer> chemicalBalance = acidity == 0
                ? Optional.empty()
                : Optional.of(ChemicalBalanceRange.toHundredths(acidity));
        int cbDrainPerTickMb = chemicalBalance.isPresent() ? 1 : 0;

        return new IndustrialFluid(
                material.id(), material.displayName(), material.color(), kind,
                temperature, density, viscosity, 0,
                chemicalBalance, cbDrainPerTickMb, material.components(), Optional.empty()
        );
    }
}
