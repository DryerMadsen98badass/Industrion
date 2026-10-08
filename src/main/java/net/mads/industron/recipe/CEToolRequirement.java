package net.mads.industron.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyToolType;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Optional;

/** A non-consumed manual tool action declared by a normal CE recipe. */
public record CEToolRequirement(String toolId, int amount) {
    public static final Codec<CEToolRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_EMPTY_STRING.fieldOf("tool").forGetter(CEToolRequirement::toolId),
            ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(CEToolRequirement::amount)
    ).apply(instance, CEToolRequirement::new));

    public CEToolRequirement {
        if (toolId == null || toolId.isBlank()) throw new IllegalArgumentException("Tool id cannot be blank");
        if (amount < 1) throw new IllegalArgumentException("Tool amount must be at least 1");
    }

    public static CEToolRequirement of(AssemblyToolType tool, int amount) {
        if (tool == null) throw new IllegalArgumentException("Tool cannot be null");
        return new CEToolRequirement(tool.id(), amount);
    }

    public static CEToolRequirement of(ToolDefinition tool, int amount) {
        if (tool == null) throw new IllegalArgumentException("Tool cannot be null");
        return new CEToolRequirement(tool.id(), amount);
    }

    public Optional<AssemblyToolType> registeredType() {
        return AssemblyTools.findType(toolId);
    }

    public boolean matches(ItemStack stack) {
        return AssemblyTools.find(toolId, stack) != null;
    }

    public String displayName() {
        return registeredType().map(AssemblyToolType::displayName).orElseGet(() -> {
            String readable = toolId.replace('_', ' ').trim();
            if (readable.isEmpty()) return toolId;
            return readable.substring(0, 1).toUpperCase(Locale.ROOT) + readable.substring(1);
        });
    }
}
