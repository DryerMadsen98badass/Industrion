package net.mads.industron.machine.foundry;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.*;
import net.mads.industron.recipe.*;
import net.mads.industron.registry.FluidRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

import java.math.BigInteger;
import java.util.*;

/**
 * Runtime centrifuge recipes for unidentified molten mixtures.
 *
 * <p>These recipes are never registered/datagen recipes. They are derived from the exact
 * composition stored on a {@code MOLTEN_MIXTURE} stack and then executed by the normal
 * CERecipe runtime. A pass aims for roughly one second of work, keeps at most three fluid
 * outputs, and recursively separates larger mixtures into density fractions.</p>
 */
public final class MixtureCentrifuging {
    private static final String PREFIX = "runtime_mixture/";
    public static final int TARGET_BATCH_MB = 20;

    private MixtureCentrifuging() {}

    public static List<RecipeHolder<CERecipe>> candidates(List<ResourceLocation> types, CERecipeInput input) {
        if (!types.contains(CERecipeTypes.CENTRIFUGING.id())) return List.of();

        List<RecipeHolder<CERecipe>> result = new ArrayList<>();
        for (FluidStack fluid : input.fluids()) {
            if (!FoundryMetallurgy.unidentified(fluid)) continue;

            MoltenRatio ratio = fluid.get(FoundryComponents.COMPOSITION.get());
            if (ratio == null || ratio.weights().size() < 2 || ratio.total().bitLength() > 30) continue;
            if (fluid.getAmount() <= 0) continue;

            int temperature = FoundryMetallurgy.temperature(fluid);
            if (temperature < FoundryMetallurgy.liquidus(fluid)) continue;

            int amount = Math.min(fluid.getAmount(), preferredBatchAmount(ratio));
            ResourceLocation id = runtimeId(temperature, amount, ratio);
            if (id != null) byId(id).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Preferred pass size. If the normalized ratio can fit exactly at or below 20 mB,
     * use the largest exact multiple. Extremely wide ratios still use the 20 mB target
     * and rely on deterministic largest-remainder apportionment.
     */
    public static int preferredBatchAmount(MoltenRatio ratio) {
        if (ratio == null || ratio.total().signum() <= 0 || ratio.total().bitLength() > 30) {
            return TARGET_BATCH_MB;
        }
        int unit = ratio.total().intValueExact();
        if (unit <= TARGET_BATCH_MB) {
            return unit * Math.max(1, TARGET_BATCH_MB / unit);
        }
        return TARGET_BATCH_MB;
    }

    public static boolean isRuntimeRecipe(ResourceLocation id) {
        return id != null
                && id.getNamespace().equals(Industron.MOD_ID)
                && id.getPath().startsWith(PREFIX);
    }

    /** Parsed identity used by the singleblock centrifuge only for its small-mixture wait rule. */
    public static Optional<RuntimeKey> runtimeKey(ResourceLocation id) {
        if (!isRuntimeRecipe(id) || id.getPath().length() > 8192) return Optional.empty();
        try {
            String[] parts = id.getPath().substring(PREFIX.length()).split("/");
            if (parts.length < 6 || parts.length > 130 || parts.length % 2 != 0) return Optional.empty();

            int temperature = Integer.parseInt(parts[0]);
            int amount = Integer.parseInt(parts[1]);
            if (temperature < 0 || temperature > 1_000_000 || amount <= 0) return Optional.empty();

            Map<String, BigInteger> weights = new TreeMap<>();
            for (int i = 2; i < parts.length; i += 2) {
                if (weights.put(parts[i], new BigInteger(parts[i + 1])) != null) return Optional.empty();
            }
            return Optional.of(new RuntimeKey(temperature, amount, new MoltenRatio(weights)));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return Optional.empty();
        }
    }

    public static Optional<RecipeHolder<CERecipe>> byId(ResourceLocation id) {
        Optional<RuntimeKey> parsed = runtimeKey(id);
        if (parsed.isEmpty()) return Optional.empty();

        try {
            RuntimeKey key = parsed.get();
            int temperature = key.temperature();
            int amount = key.amount();
            MoltenRatio ratio = key.ratio();

            Map<String, Integer> apportioned = splitLargestRemainder(ratio, amount);
            if (apportioned.isEmpty()) return Optional.empty();

            List<ComponentSlice> components = new ArrayList<>();
            int tierIndex = 0;
            for (var entry : ratio.weights().entrySet()) {
                IndustrialMaterial material = MaterialCatalog.find(entry.getKey());
                if (material == null || !material.has(MaterialPart.INGOT)
                        || !material.has(MaterialPart.MOLTEN_FLUID)
                        || temperature < material.meltingPoint()) {
                    return Optional.empty();
                }

                int materialTier = MachineTier.ALL.indexOf(material.tier());
                if (materialTier < 0) return Optional.empty();
                tierIndex = Math.max(tierIndex, materialTier);

                components.add(new ComponentSlice(
                        entry.getKey(),
                        entry.getValue(),
                        apportioned.getOrDefault(entry.getKey(), 0),
                        material.properties().density(),
                        material
                ));
            }

            List<FluidStack> outputs = buildOutputs(components, temperature);
            if (outputs.isEmpty() || outputs.size() > 3) return Optional.empty();
            int outputAmount = outputs.stream().mapToInt(FluidStack::getAmount).sum();
            if (outputAmount != amount) return Optional.empty();

            FluidStack source = new FluidStack(FluidRegistry.MOLTEN_MIXTURE.source().get(), amount);
            source.set(FoundryComponents.COMPOSITION.get(), ratio);
            source.set(FoundryComponents.TEMPERATURE.get(), temperature);

            var exactInput = new net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient(
                    net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient.of(true, source.copy()),
                    amount
            );

            // Runtime mixture rule: 1 tick per mB actually consumed.
            int duration = amount;

            CERecipe runtimeRecipe = new CERecipe(
                    CERecipeTypes.CENTRIFUGING.id(),
                    List.of(),
                    List.of(),
                    List.of(exactInput),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    outputs,
                    List.of(),
                    Optional.empty(),
                    Optional.of(duration),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(MachineTier.ALL.get(tierIndex).id()),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    false
            );
            return Optional.of(new RecipeHolder<>(id, runtimeRecipe));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return Optional.empty();
        }
    }

    /**
     * At most three outputs. More complex mixtures are split into contiguous density groups
     * at the two largest density gaps. Each multi-material group is resolved back to a known
     * molten material/alloy when its normalized composition exists; otherwise it remains an
     * unidentified molten mixture for another centrifuge pass.
     */
    private static List<FluidStack> buildOutputs(List<ComponentSlice> input, int temperature) {
        List<ComponentSlice> sorted = new ArrayList<>(input);
        sorted.sort(Comparator
                .comparingInt(ComponentSlice::density)
                .thenComparing(ComponentSlice::id));

        List<List<ComponentSlice>> groups = densityGroups(sorted);
        List<FluidStack> result = new ArrayList<>();

        for (List<ComponentSlice> group : groups) {
            int groupAmount = group.stream().mapToInt(ComponentSlice::amount).sum();
            if (groupAmount <= 0) continue;

            FluidStack output;
            if (group.size() == 1) {
                output = FoundryMetallurgy.molten(group.getFirst().material(), groupAmount);
            } else {
                Map<String, BigInteger> groupWeights = new TreeMap<>();
                for (ComponentSlice component : group) {
                    groupWeights.put(component.id(), component.weight());
                }
                output = FoundryMetallurgy.resolve(new MoltenRatio(groupWeights), groupAmount, temperature);
            }

            if (output.isEmpty()) return List.of();
            output.set(FoundryComponents.TEMPERATURE.get(), temperature);
            result.add(output);
        }

        return List.copyOf(result);
    }

    private static List<List<ComponentSlice>> densityGroups(List<ComponentSlice> sorted) {
        if (sorted.size() <= 3) {
            List<List<ComponentSlice>> direct = new ArrayList<>();
            for (ComponentSlice component : sorted) direct.add(List.of(component));
            return direct;
        }

        List<DensityGap> gaps = new ArrayList<>();
        for (int i = 0; i < sorted.size() - 1; i++) {
            long gap = (long) sorted.get(i + 1).density() - sorted.get(i).density();
            gaps.add(new DensityGap(i + 1, gap));
        }

        gaps.sort(Comparator
                .comparingLong(DensityGap::gap).reversed()
                .thenComparingInt(DensityGap::splitIndex));

        int first = gaps.get(0).splitIndex();
        int second = gaps.get(1).splitIndex();
        if (first > second) {
            int swap = first;
            first = second;
            second = swap;
        }

        return List.of(
                List.copyOf(sorted.subList(0, first)),
                List.copyOf(sorted.subList(first, second)),
                List.copyOf(sorted.subList(second, sorted.size()))
        );
    }

    /**
     * Hamilton/largest-remainder allocation. It consumes the complete final remainder even
     * when its mB count is not divisible by the composition unit, while always preserving
     * total fluid volume and remaining deterministic.
     */
    private static Map<String, Integer> splitLargestRemainder(MoltenRatio ratio, int amount) {
        BigInteger total = ratio.total();
        BigInteger amountBig = BigInteger.valueOf(amount);
        Map<String, Integer> result = new TreeMap<>();
        List<Remainder> remainders = new ArrayList<>();
        int allocated = 0;

        for (var entry : ratio.weights().entrySet()) {
            BigInteger numerator = entry.getValue().multiply(amountBig);
            BigInteger[] division = numerator.divideAndRemainder(total);
            int floor = division[0].intValueExact();
            result.put(entry.getKey(), floor);
            allocated = Math.addExact(allocated, floor);
            remainders.add(new Remainder(entry.getKey(), division[1]));
        }

        int left = amount - allocated;
        remainders.sort(Comparator
                .comparing(Remainder::remainder, Comparator.reverseOrder())
                .thenComparing(Remainder::id));
        for (int i = 0; i < left; i++) {
            String id = remainders.get(i % remainders.size()).id();
            result.put(id, result.get(id) + 1);
        }
        return Map.copyOf(result);
    }

    private static ResourceLocation runtimeId(int temperature, int amount, MoltenRatio ratio) {
        StringBuilder path = new StringBuilder(PREFIX).append(temperature).append('/').append(amount);
        ratio.weights().forEach((materialId, weight) -> path.append('/').append(materialId).append('/').append(weight));
        if (path.length() > 8192) return null;
        return ResourceLocation.tryBuild(Industron.MOD_ID, path.toString());
    }

    public record RuntimeKey(int temperature, int amount, MoltenRatio ratio) {}

    private record ComponentSlice(
            String id,
            BigInteger weight,
            int amount,
            int density,
            IndustrialMaterial material
    ) {}

    private record DensityGap(int splitIndex, long gap) {}
    private record Remainder(String id, BigInteger remainder) {}
}
