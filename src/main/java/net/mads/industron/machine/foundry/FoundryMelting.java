package net.mads.industron.machine.foundry;

import net.mads.industron.material.*;
import net.mads.industron.recipe.*;
import net.mads.industron.registry.FluidRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.*;

/** Per-slot time/temperature processing; paused or blocked work never consumes inputs. */
public final class FoundryMelting {
    private final Map<Integer, Work> work = new HashMap<>();
    private int cooldown;
    private long workRevision = Long.MIN_VALUE;
    private static final class Work {
        final ItemStack input;
        final Plan plan;
        int ticks;
        Work(ItemStack input, Plan plan) { this.input = input.copyWithCount(1); this.plan = plan; }
    }
    public record Plan(int count, int temperature, int duration, List<FluidStack> outputs) {}

    public int displayValue(FoundryBlockEntity foundry, int slot, int field) {
        Work current = work.get(slot);
        if (!foundry.isFormed() || current == null) return 0;
        ItemStack input = foundry.itemStorage().getStackInSlot(slot);
        if (input.isEmpty() || !ItemStack.isSameItemSameComponents(input, current.input)) return 0;
        return switch (field) {
            case 0 -> current.ticks;
            case 1 -> Math.max(1, current.plan.duration());
            case 2 -> current.plan.temperature();
            case 3 -> input.getCount() < current.plan.count() ? 4
                    : foundry.temperature() < current.plan.temperature() ? 1
                    : current.ticks >= current.plan.duration() ? 3 : 2;
            default -> 0;
        };
    }

    public void tick(FoundryBlockEntity foundry) {
        if (++cooldown < 10) return;
        if (workRevision != CERecipeLookup.revision()) { work.clear(); workRevision = CERecipeLookup.revision(); }
        cooldown = 0;
        foundry.fluidStorage().equilibrate();
        var slots = foundry.itemStorage().activeSlots();
        work.keySet().retainAll(slots);
        for (int slot : slots) {
            ItemStack input = foundry.itemStorage().getStackInSlot(slot);
            Work current = work.get(slot);
            if (current == null || !ItemStack.isSameItemSameComponents(input, current.input)) {
                Plan plan = plan(foundry, input);
                if (plan == null) { work.remove(slot); continue; }
                current = new Work(input, plan);
                work.put(slot, current);
            }
            Plan plan = current.plan;
            if (input.getCount() < plan.count() || foundry.temperature() < plan.temperature()) {
                current.ticks = 0;
                continue;
            }
            current.ticks = (int) Math.min(plan.duration(), (long) current.ticks + 10);
            if (current.ticks < plan.duration()) continue;
            if (!foundry.fluidStorage().addBatch(plan.outputs(), FluidAction.SIMULATE)) continue;
            if (foundry.fluidStorage().addBatch(plan.outputs(), FluidAction.EXECUTE)) {
                foundry.itemStorage().extractItem(slot, plan.count(), false);
                current.ticks = 0;
            }
        }
    }

    public static int volume(MaterialPart part) {
        return MaterialFormAmounts.millibuckets(part);
    }

    private record Key(net.minecraft.world.item.Item item, net.minecraft.core.component.DataComponentPatch components, int count) {}
    private static final Map<Key, Optional<Plan>> PLANS = new LinkedHashMap<>(256, 0.75F, true);
    private static net.minecraft.world.item.crafting.RecipeManager planManager;
    private static long planRevision = Long.MIN_VALUE;
    public static void clearCache() { PLANS.clear(); planManager = null; }

    private static Plan plan(FoundryBlockEntity foundry, ItemStack input) {
        if (input.isEmpty() || foundry.getLevel() == null) return null;
        var manager = foundry.getLevel().getRecipeManager();
        long revision = CERecipeLookup.revision();
        if (planManager != manager || planRevision != revision) {
            PLANS.clear(); planManager = manager; planRevision = revision;
        }
        Key key = new Key(input.getItem(), input.getComponentsPatch(), input.getCount());
        Optional<Plan> cached = PLANS.get(key);
        if (cached == null) {
            cached = Optional.ofNullable(computePlan(foundry, input));
            if (PLANS.size() >= 256) PLANS.remove(PLANS.keySet().iterator().next());
            PLANS.put(key, cached);
        }
        // Cached fluids never escape to a potentially mutating storage implementation.
        return cached.map(p -> new Plan(p.count(), p.temperature(), p.duration(),
                p.outputs().stream().map(FluidStack::copy).toList())).orElse(null);
    }

    private static Plan computePlan(FoundryBlockEntity foundry, ItemStack input) {
        var target = MaterialLookup.find(input);
        if (target == null || target.part().isFluid()) return null;
        IndustrialMaterial source = target.material();
        int amount = volume(target.part());
        if (amount == 0 || source.isClayMaterial()) return null;
        boolean raw = switch (target.part()) {
            case RAW_ORE, RAW_BLOCK, CRUSHED_ORE, WASHED_CRUSHED_ORE, REFINED_ORE, IMPURE_DUST -> true;
            default -> false;
        };
        // Composite powders only enter the molten chemistry route already selected by Phase 06.
        if (!raw && !source.components().isEmpty() && !source.has(MaterialPart.INGOT)) {
            if (foundry.getLevel() == null) return null;
            for (var holder : CERecipeLookup.byType(foundry.getLevel().getRecipeManager(), CERecipeTypes.MELTING)) {
                CERecipe recipe = holder.value();
                if (recipe.itemInputs().size() != 1 || !recipe.itemInputs().getFirst().ingredient().test(input)
                        || !recipe.fluidInputs().isEmpty() || !recipe.itemOutputs().isEmpty()
                        || !recipe.chancedItemInputs().isEmpty() || !recipe.chancedFluidInputs().isEmpty()
                        || !recipe.chancedFluidOutputs().isEmpty() || !recipe.notConsumableItems().isEmpty()
                        || !recipe.notConsumableFluids().isEmpty() || !recipe.tools().isEmpty()
                        || !recipe.conditions().isEmpty() || !recipe.modifiers().isEmpty()
                        || recipe.requiredLogic().stream().anyMatch(logic -> !logic.equals(CERecipeLogics.COIL_TEMP.id()))
                        || recipe.circuit().isPresent()
                        || !recipe.blockInteractions().isEmpty() || recipe.fluidOutputs().isEmpty()) continue;
                return new Plan(recipe.itemInputs().getFirst().count(),
                        Math.max(meltingTemperature(source), recipe.requiredTemp().orElse(0)),
                        recipe.duration().orElse(200), recipe.fluidOutputs());
            }
            return null;
        }
        IndustrialMaterial output = source.smeltingResult().orElse(source);
        if (!output.has(MaterialPart.MOLTEN_FLUID)) return null;
        int yield = raw ? switch (target.part()) {
            case RAW_ORE, RAW_BLOCK -> 108;
            case CRUSHED_ORE, IMPURE_DUST -> 120;
            case WASHED_CRUSHED_ORE -> 132;
            case REFINED_ORE -> 140;
            default -> 144;
        } : 144;
        var amounts = FoundryOutputRules.amounts(amount, yield);
        List<FluidStack> outputs = new ArrayList<>();
        outputs.add(FoundryMetallurgy.molten(output, amounts.metalMb()));
        if (amounts.slagMb() > 0) outputs.add(new FluidStack(FluidRegistry.MOLTEN_SLAG.source().get(), amounts.slagMb()));
        int heat = Math.max(meltingTemperature(source), output.castTemperature());
        return new Plan(1, heat, net.mads.industron.material.recipes.OreWorkRules.meltingDuration(
                FoundryMetallurgy.duration(source, amount, heat), target.part()), List.copyOf(outputs));
    }

    private static int meltingTemperature(IndustrialMaterial material) {
        return material.properties().hasProperty("meltingPoint")
                ? Math.max(1, material.meltingPoint())
                : Math.max(1, material.castTemperature());
    }
}
