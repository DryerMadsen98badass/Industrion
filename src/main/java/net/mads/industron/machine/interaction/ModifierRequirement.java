package net.mads.industron.machine.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.Industron;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

/** Requirement that must match before a machine modifier can apply. */
public record ModifierRequirement(
        Type type,
        Optional<ResourceLocation> id,
        int amount,
        Optional<BlockInteraction> blockInteraction,
        Optional<MachineCondition> condition
) {
    public static final Codec<ModifierRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Type::valueOf, Type::name).fieldOf("type").forGetter(ModifierRequirement::type),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(ModifierRequirement::id),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(ModifierRequirement::amount),
            BlockInteraction.CODEC.optionalFieldOf("block_interaction").forGetter(ModifierRequirement::blockInteraction),
            MachineCondition.CODEC.optionalFieldOf("condition").forGetter(ModifierRequirement::condition)
    ).apply(instance, ModifierRequirement::new));

    /** Requires an item in the machine's available item inputs. */
    public static ModifierRequirement item(String itemId, int amount) {
        return new ModifierRequirement(Type.ITEM, Optional.of(id(itemId)), Math.max(1, amount), Optional.empty(), Optional.empty());
    }

    /** Requires a fluid in the machine's available fluid inputs. */
    public static ModifierRequirement fluid(String fluidId, int amount) {
        return new ModifierRequirement(Type.FLUID, Optional.of(id(fluidId)), Math.max(1, amount), Optional.empty(), Optional.empty());
    }

    /** Requires a world block/fluid check. */
    public static ModifierRequirement block(BlockInteraction interaction) {
        return new ModifierRequirement(Type.BLOCK, Optional.empty(), 1, Optional.of(interaction), Optional.empty());
    }

    /** Requires a world condition. */
    public static ModifierRequirement condition(MachineCondition condition) {
        return new ModifierRequirement(Type.CONDITION, Optional.empty(), 1, Optional.empty(), Optional.of(condition));
    }

    public boolean matchesItems(List<ItemStack> stacks) {
        if (type != Type.ITEM || id.isEmpty()) {
            return true;
        }
        int found = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id.get())) {
                found += stack.getCount();
            }
        }
        return found >= amount;
    }

    public boolean matchesFluids(List<FluidStack> stacks) {
        if (type != Type.FLUID || id.isEmpty()) {
            return true;
        }
        int found = 0;
        for (FluidStack stack : stacks) {
            if (!stack.isEmpty() && BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(id.get())) {
                found += stack.getAmount();
            }
        }
        return found >= amount;
    }

    private static ResourceLocation id(String id) {
        return id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id);
    }

    public enum Type {
        ITEM,
        FLUID,
        BLOCK,
        CONDITION
    }
}
