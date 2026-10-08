package net.mads.industron.machine.foundry;

import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.material.*;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.registry.FluidRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** Runtime metallurgy derived from material definitions; never decomposes bonded ore minerals. */
public final class FoundryMetallurgy {
    private static final Map<String, MoltenRatio> RATIOS = new HashMap<>();
    private static Map<MoltenRatio, List<IndustrialMaterial>> alloys;
    private FoundryMetallurgy() {}

    public static FluidStack molten(IndustrialMaterial material, int amount) {
        if (material == null || !material.has(MaterialPart.MOLTEN_FLUID) || amount <= 0) return FluidStack.EMPTY;
        return new FluidStack(BuiltInRegistries.FLUID.get(
                IndustrialFluidLookup.fluidId(material, MaterialPart.MOLTEN_FLUID)), amount);
    }

    public static boolean unidentified(FluidStack fluid) {
        return fluid.is(FluidRegistry.MOLTEN_MIXTURE.source().get());
    }

    public static IndustrialMaterial material(FluidStack fluid) {
        if (unidentified(fluid)) return null;
        var target = MaterialLookup.find(fluid);
        return target != null && target.part() == MaterialPart.MOLTEN_FLUID ? target.material() : null;
    }

    public static MoltenRatio ratio(IndustrialMaterial material) {
        MoltenRatio cached = RATIOS.get(material.id());
        if (cached != null) return cached;
        MoltenRatio result = MoltenRatio.pure(material.id());
        // Only actual metal-form materials may be unpacked for alloy matching, not ore chemistry.
        if (material.has(MaterialPart.INGOT) && !material.components().isEmpty()) {
            result = null;
            long total = 0;
            for (var component : material.components()) {
                IndustrialMaterial child = MaterialCatalog.find(component.substance().id());
                MoltenRatio next = child == null ? MoltenRatio.pure(component.substance().id()) : ratio(child);
                result = result == null ? next : result.mix(total, next, component.amount());
                total += component.amount();
            }
        }
        RATIOS.put(material.id(), result);
        return result;
    }

    public static MoltenRatio ratio(FluidStack fluid) {
        if (unidentified(fluid)) return fluid.get(FoundryComponents.COMPOSITION.get());
        IndustrialMaterial material = material(fluid);
        return material != null && material.has(MaterialPart.INGOT) ? ratio(material) : null;
    }

    private static Map<MoltenRatio, List<IndustrialMaterial>> alloys() {
        if (alloys == null) {
            Map<MoltenRatio, List<IndustrialMaterial>> result = new HashMap<>();
            for (IndustrialMaterial material : IndustrialMaterials.ALL) {
                if (!material.isOreMaterial() && !material.isClayMaterial() && material.has(MaterialPart.INGOT)
                        && material.has(MaterialPart.MOLTEN_FLUID)) {
                    result.computeIfAbsent(ratio(material), ignored -> new ArrayList<>()).add(material);
                }
            }
            alloys = result;
        }
        return alloys;
    }

    public static FluidStack resolve(MoltenRatio ratio, int amount, int temperature) {
        var matches = alloys().getOrDefault(ratio, List.of());
        if (matches.size() == 1 && temperature >= matches.getFirst().meltingPoint()) {
            return molten(matches.getFirst(), amount);
        }
        FluidStack result = new FluidStack(FluidRegistry.MOLTEN_MIXTURE.source().get(), amount);
        result.set(FoundryComponents.COMPOSITION.get(), ratio);
        return result;
    }

    public static int temperature(FluidStack fluid) {
        Integer actual = fluid.get(FoundryComponents.TEMPERATURE.get());
        if (actual != null) return actual;
        IndustrialMaterial material = material(fluid);
        return material == null ? liquidus(fluid) : material.castTemperature();
    }

    public static int liquidus(FluidStack fluid) {
        IndustrialMaterial material = material(fluid);
        if (material != null) return material.meltingPoint();
        MoltenRatio ratio = unidentified(fluid) ? fluid.get(FoundryComponents.COMPOSITION.get()) : null;
        if (ratio == null) return 0;
        int result = 0;
        for (String id : ratio.weights().keySet()) {
            IndustrialMaterial component = MaterialCatalog.find(id);
            if (component == null) return Integer.MAX_VALUE;
            result = Math.max(result, component.meltingPoint());
        }
        return result;
    }

    /** Metals share one bath; non-metallic molten minerals and other fluids remain separate phases. */
    public static List<FluidStack> equilibrate(List<FluidStack> input, int temperature) {
        List<FluidStack> result = new ArrayList<>();
        MoltenRatio mixture = null;
        int mixedAmount = 0;
        for (FluidStack original : input) {
            FluidStack fluid = original.copy();
            fluid.remove(FoundryComponents.TEMPERATURE.get());
            MoltenRatio next = ratio(fluid);
            if (next != null && temperature >= liquidus(fluid)) {
                mixture = mixture == null ? next : mixture.mix(mixedAmount, next, fluid.getAmount());
                mixedAmount = Math.addExact(mixedAmount, fluid.getAmount());
            } else addLayer(result, fluid);
        }
        if (mixture != null) addLayer(result, resolve(mixture, mixedAmount, temperature));
        result.sort(Comparator.comparingInt(FoundryMetallurgy::density).reversed());
        return result;
    }

    private static void addLayer(List<FluidStack> result, FluidStack fluid) {
        for (FluidStack present : result) {
            if (FluidStack.isSameFluidSameComponents(present, fluid)) {
                present.grow(fluid.getAmount());
                return;
            }
        }
        result.add(fluid);
    }

    public static int density(FluidStack fluid) {
        IndustrialMaterial material = material(fluid);
        if (material != null) return material.properties().density();
        MoltenRatio ratio = ratio(fluid);
        if (ratio == null) return 1000;
        double sum = 0;
        for (var entry : ratio.weights().entrySet()) {
            IndustrialMaterial component = MaterialCatalog.find(entry.getKey());
            if (component != null) sum += component.properties().density() *
                    ratio.share(entry.getKey());
        }
        return (int) sum;
    }

    public static int duration(IndustrialMaterial material, int mb, int temperature) {
        double heatCapacity = Math.max(1, material.properties().specificHeatCapacity());
        double conductivity = Math.max(1, material.properties().thermalConductivity());
        double ticks = 200.0 * mb / MaterialUnits.MILLIBUCKETS_PER_UNIT
                * Math.max(1, temperature - 20) / 500.0 * Math.sqrt(heatCapacity / conductivity);
        return (int) Math.max(20, Math.min(Integer.MAX_VALUE, Math.ceil(ticks)));
    }
}
