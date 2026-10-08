package net.mads.industron.machine.machines.electric.multiblock;

import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.block.coils.CoilDefinitions;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record MultiblockVisualization(List<MachineTier> tiers, Map<Character, SymbolInfo> symbols) {
    public static MultiblockVisualization empty() {
        return new MultiblockVisualization(MachineTier.ALL, Map.of());
    }

    public List<ItemStack> validStacks(char symbol, MachineTier tier, MultiblockControllerDefinition controller, char controllerSymbol) {
        if (symbol == controllerSymbol) {
            return stackFor(controller.id());
        }

        SymbolInfo info = symbols.get(symbol);
        if (info == null) {
            return List.of();
        }

        return info.validStacks(tier);
    }

    private static List<ItemStack> stackFor(ResourceLocation blockId) {
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        if (block == null || block.asItem() == null) {
            return List.of();
        }

        ItemStack stack = new ItemStack(block);
        return stack.isEmpty() ? List.of() : List.of(stack);
    }

    public static Builder builder() {
        return new Builder();
    }

    public MultiblockVisualization withSymbol(char symbol, SymbolInfo info) {
        Map<Character, SymbolInfo> merged = new LinkedHashMap<>(symbols);
        merged.put(symbol, info);
        return new MultiblockVisualization(tiers, Map.copyOf(merged));
    }

    public MultiblockVisualization merge(MultiblockVisualization other) {
        Map<Character, SymbolInfo> merged = new LinkedHashMap<>(symbols);
        merged.putAll(other.symbols);
        List<MachineTier> mergedTiers = other.tiers.isEmpty() ? tiers : other.tiers;
        return new MultiblockVisualization(mergedTiers, Map.copyOf(merged));
    }

    public record SymbolInfo(
            List<ResourceLocation> blockIds,
            List<MultiblockPredicates.TieredBlock> tieredBlocks,
            Set<MultiblockAbility> requiredAbilities,
            Set<MultiblockAbility> anyAbilities,
            boolean tieredMachineCasing,
            Set<String> materialCasingDefinitions,
            boolean coilBlocks
    ) {
        public static SymbolInfo block(ResourceLocation blockId) {
            return new SymbolInfo(List.of(blockId), List.of(), Set.of(), Set.of(), false, Set.of(), false);
        }

        public static SymbolInfo tieredBlock(MultiblockPredicates.TieredBlock block) {
            return new SymbolInfo(List.of(), List.of(block), Set.of(), Set.of(), false, Set.of(), false);
        }

        public static SymbolInfo tieredBlocks(List<MultiblockPredicates.TieredBlock> blocks) {
            return new SymbolInfo(List.of(), List.copyOf(blocks), Set.of(), Set.of(), false, Set.of(), false);
        }

        public static SymbolInfo requiredAbility(Set<MultiblockAbility> abilities) {
            return new SymbolInfo(List.of(), List.of(), Set.copyOf(abilities), Set.of(), false, Set.of(), false);
        }

        public static SymbolInfo anyAbility(Set<MultiblockAbility> abilities) {
            return new SymbolInfo(List.of(), List.of(), Set.of(), Set.copyOf(abilities), false, Set.of(), false);
        }

        public static SymbolInfo machineCasings() {
            return new SymbolInfo(List.of(), List.of(), Set.of(), Set.of(), true, Set.of(), false);
        }

        public static SymbolInfo materialMachineCasings(String definitionId) {
            return new SymbolInfo(List.of(), List.of(), Set.of(), Set.of(), false, Set.of(definitionId), false);
        }

        public static SymbolInfo coils() {
            return new SymbolInfo(List.of(), List.of(), Set.of(), Set.of(), false, Set.of(), true);
        }

        public SymbolInfo merge(SymbolInfo other) {
            List<ResourceLocation> mergedBlockIds = mergeLists(blockIds, other.blockIds);
            List<MultiblockPredicates.TieredBlock> mergedTieredBlocks = mergeLists(tieredBlocks, other.tieredBlocks);
            Set<MultiblockAbility> mergedRequired = mergeSets(requiredAbilities, other.requiredAbilities);
            Set<MultiblockAbility> mergedAny = mergeSets(anyAbilities, other.anyAbilities);
            Set<String> mergedMaterialCasings = mergeSets(materialCasingDefinitions, other.materialCasingDefinitions);
            return new SymbolInfo(
                    mergedBlockIds,
                    mergedTieredBlocks,
                    mergedRequired,
                    mergedAny,
                    tieredMachineCasing || other.tieredMachineCasing,
                    mergedMaterialCasings,
                    coilBlocks || other.coilBlocks
            );
        }

        public List<ItemStack> validStacks(MachineTier tier) {
            List<ItemStack> stacks = new ArrayList<>();

            if (tieredMachineCasing) {
                BlockRegistry.MACHINE_CASINGS.values().forEach(block -> {
                    if (block.get().tier().equals(tier)) {
                        stacks.add(new ItemStack(block.get()));
                    }
                });
            }

            if (!materialCasingDefinitions.isEmpty()) {
                BlockRegistry.MATERIAL_MACHINE_CASINGS.values().forEach(block -> {
                    if (block.get().tier().equals(tier)
                            && materialCasingDefinitions.contains(block.get().definition().id())) {
                        stacks.add(new ItemStack(block.get()));
                    }
                });
            }

            if (coilBlocks) {
                CoilDefinitions.ALL.stream()
                        .map(definition -> BlockRegistry.getCoil(definition.id()))
                        .filter(java.util.Objects::nonNull)
                        .forEach(block -> stacks.add(new ItemStack(block.get())));
            }

            for (MultiblockPredicates.TieredBlock entry : tieredBlocks) {
                if (entry.tier().equals(tier)) {
                    stacks.addAll(stackFor(entry.blockId()));
                }
            }

            for (ResourceLocation blockId : blockIds) {
                stacks.addAll(stackFor(blockId));
            }

            if (!requiredAbilities.isEmpty() || !anyAbilities.isEmpty()) {
                BlockRegistry.getAllMachinePorts().forEach(block -> {
                    MachinePortBlock port = block.get();
                    if (matchesPort(port, tier)) {
                        stacks.add(new ItemStack(port));
                    }
                });
                BlockRegistry.getAllStaticMachinePorts().forEach(block -> {
                    MachinePortBlock port = block.get();
                    if (matchesPort(port, tier)) {
                        stacks.add(new ItemStack(port));
                    }
                });
            }

            return stacks;
        }

        private boolean matchesPort(MachinePortBlock port, MachineTier tier) {
            if (port.hasTier() && !port.effectiveTier().equals(tier)) {
                return false;
            }
            if (!port.abilities().containsAll(requiredAbilities)) {
                return false;
            }
            return anyAbilities.isEmpty() || anyAbilities.stream().anyMatch(port.abilities()::contains);
        }

        private static <T> List<T> mergeLists(List<T> first, List<T> second) {
            List<T> merged = new ArrayList<>(first);
            for (T value : second) {
                if (!merged.contains(value)) {
                    merged.add(value);
                }
            }
            return List.copyOf(merged);
        }

        private static <T> Set<T> mergeSets(Set<T> first, Set<T> second) {
            Set<T> merged = new java.util.LinkedHashSet<>(first);
            merged.addAll(second);
            return Set.copyOf(merged);
        }
    }

    public static final class Builder {
        private final List<MachineTier> tiers = new ArrayList<>(MachineTier.ALL);
        private final Map<Character, SymbolInfo> symbols = new LinkedHashMap<>();

        public Builder tiers(MachineTier... tiers) {
            this.tiers.clear();
            this.tiers.addAll(List.of(tiers));
            return this;
        }

        public Builder block(char symbol, String blockId) {
            symbols.put(symbol, SymbolInfo.block(MultiblockRegistry.id(blockId)));
            return this;
        }

        public Builder tieredMachineCasing(char symbol) {
            symbols.put(symbol, SymbolInfo.machineCasings());
            return this;
        }

        public Builder ability(char symbol, MultiblockAbility firstAbility, MultiblockAbility... extraAbilities) {
            EnumSet<MultiblockAbility> abilities = EnumSet.of(firstAbility, extraAbilities);
            symbols.put(symbol, SymbolInfo.requiredAbility(abilities));
            return this;
        }

        public MultiblockVisualization build() {
            return new MultiblockVisualization(List.copyOf(tiers), Map.copyOf(symbols));
        }
    }
}
