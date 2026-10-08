package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialTierResolver;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.tool.ToolMaterialRules;
import net.mads.industron.tool.ToolMaterialLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Central Assembly Workbench level definition.
 *
 * <p>Add or change workbenches only here. Recipes use {@code .level(x)} while runtime and JEI
 * resolve the actual block level from this definition.</p>
 */
public final class WorkbenchLevels {
    public static final Definition WORKBENCHES = Definition.builder()
            .workbench("industron:basic_assembly_workbench", 1)
            .workbench("industron:assembly_workbench", 2)
            .build();

    private WorkbenchLevels() {
    }

    /** ULV/LV/MV = 1, HV/EV/IV = 2, LuV/ZPM/UV = 3, and so on. */
    public static int forTier(MachineTier tier) {
        if (tier == null || tier == MachineTier.NONE) return 1;
        MachineTier normalized = tier.recipeTier();
        int index = MachineTier.ELECTRIC_TIERS.indexOf(normalized);
        if (index < 0) return 1;
        return index / 3 + 1;
    }

    /** Workbench level implied by a concrete material identity. */
    public static int forMaterial(IndustrialSubstance material) {
        if (material == null) return 1;
        if (material instanceof IndustrialMaterial industrial) {
            return forTier(industrial.tier());
        }

        MachineTier toolTier = ToolMaterialRules.tier(material);
        if (toolTier != MachineTier.NONE) {
            return forTier(toolTier);
        }

        return forTier(MaterialTierResolver.strongestComponentTier(material));
    }

    /** Material-derived level for a concrete stack. Non-material items stay at level 1. */
    public static int forStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1;
        ToolMaterialLookup.Target toolTarget = ToolMaterialLookup.find(stack);
        if (toolTarget != null) return forMaterial(toolTarget.material());
        MaterialLookup.MaterialTarget materialTarget = MaterialLookup.find(stack);
        if (materialTarget != null) return forMaterial(materialTarget.material());
        return 1;
    }

    public static int levelOf(BlockState state) {
        return state == null ? 0 : levelOf(state.getBlock());
    }

    public static int levelOf(Block block) {
        if (block == null) return 0;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return WORKBENCHES.levelOf(id);
    }

    public static List<ItemStack> itemStacksForLevel(int level) {
        List<ItemStack> result = new ArrayList<>();
        for (ResourceLocation id : WORKBENCHES.idsForLevel(level)) {
            BuiltInRegistries.BLOCK.getOptional(id).ifPresent(block -> {
                if (block.asItem() != net.minecraft.world.item.Items.AIR) {
                    result.add(new ItemStack(block));
                }
            });
        }
        return List.copyOf(result);
    }

    public static final class Definition {
        private final Map<ResourceLocation, Integer> levelsById;
        private final Map<Integer, List<ResourceLocation>> idsByLevel;

        private Definition(Map<ResourceLocation, Integer> levelsById) {
            this.levelsById = Map.copyOf(levelsById);
            Map<Integer, List<ResourceLocation>> grouped = new LinkedHashMap<>();
            levelsById.forEach((id, level) -> grouped.computeIfAbsent(level, ignored -> new ArrayList<>()).add(id));
            Map<Integer, List<ResourceLocation>> frozen = new LinkedHashMap<>();
            grouped.forEach((level, ids) -> frozen.put(level, List.copyOf(ids)));
            this.idsByLevel = Map.copyOf(frozen);
        }

        public static Builder builder() {
            return new Builder();
        }

        public int levelOf(ResourceLocation id) {
            if (id == null) return 0;
            return levelsById.getOrDefault(id, 0);
        }

        public List<ResourceLocation> idsForLevel(int level) {
            return idsByLevel.getOrDefault(level, List.of());
        }

        public Optional<ResourceLocation> primaryForLevel(int level) {
            List<ResourceLocation> ids = idsForLevel(level);
            return ids.isEmpty() ? Optional.empty() : Optional.of(ids.getFirst());
        }

        public int highestRegisteredLevel() {
            return idsByLevel.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        }

        public static final class Builder {
            private final Map<ResourceLocation, Integer> levelsById = new LinkedHashMap<>();

            private Builder() {
            }

            /** Definition syntax: .workbench("namespace:block_id", level). */
            public Builder workbench(String id, int level) {
                return workbench(ResourceLocation.parse(Objects.requireNonNull(id, "id")), level);
            }

            public Builder workbench(ResourceLocation id, int level) {
                Objects.requireNonNull(id, "id");
                if (level < 1) throw new IllegalArgumentException("Workbench level must be >= 1");
                Integer previous = levelsById.putIfAbsent(id, level);
                if (previous != null) {
                    throw new IllegalArgumentException("Workbench id is already defined: " + id);
                }
                return this;
            }

            public Definition build() {
                if (levelsById.isEmpty()) throw new IllegalStateException("At least one Assembly Workbench must be defined");
                return new Definition(levelsById);
            }
        }
    }
}
