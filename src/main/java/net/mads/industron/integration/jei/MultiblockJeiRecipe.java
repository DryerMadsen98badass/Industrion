package net.mads.industron.integration.jei;

import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.registry.BlockRegistry;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockPattern;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicate;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockVisualization;
import net.mads.industron.machine.machines.electric.multiblock.PatternVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiblockJeiRecipe {
    private final MultiblockDefinition definition;
    private int variantIndex;
    private int layerIndex = -1;
    private int tierIndex;
    private float viewYaw = 225.0F;
    private float viewPitch = 30.0F;
    private float zoom = 1.0F;
    private SelectedBlock selectedBlock;
    private int selectedStackPage;
    private int materialPage;

    public MultiblockJeiRecipe(MultiblockDefinition definition) {
        this.definition = definition;
        this.variantIndex = 0;
    }

    public MultiblockDefinition definition() {
        return definition;
    }

    public PatternVariant variant() {
        List<PatternVariant> variants = variantsForJei();
        return variants.get(Math.floorMod(variantIndex, variants.size()));
    }

    public MachineTier tier() {
        List<MachineTier> tiers = tiers();
        return tiers.get(Math.floorMod(tierIndex, tiers.size()));
    }

    public List<MachineTier> tiers() {
        List<MachineTier> tiers = definition.visualization().tiers();
        return tiers.isEmpty() ? MachineTier.ALL : tiers;
    }

    public int layerIndex() {
        return Math.min(Math.max(layerIndex, 0), Math.max(variant().width() - 1, 0));
    }

    public boolean allLayers() {
        return layerIndex < 0;
    }

    public boolean isLayerVisible(int x) {
        return allLayers() || x == layerIndex();
    }

    public String layerDisplay() {
        return allLayers() ? "All layers" : "Layer " + (layerIndex() + 1) + "/" + Math.max(variant().width(), 1);
    }

    public float viewYaw() {
        return viewYaw;
    }

    public float viewPitch() {
        return viewPitch;
    }

    public float zoom() {
        return zoom;
    }

    public SelectedBlock selectedBlock() {
        return selectedBlock;
    }

    public Character selectedSymbol() {
        return selectedBlock == null ? null : selectedBlock.symbol();
    }

    public int selectedStackPage() {
        return selectedStackPage;
    }

    public void nextLayer() {
        int width = variant().width();
        if (width <= 0) {
            layerIndex = -1;
            return;
        }

        layerIndex++;
        if (layerIndex >= width) {
            layerIndex = -1;
        }
        selectedBlock = null;
        selectedStackPage = 0;
        materialPage = 0;
    }

    public void nextVariant() {
        variantIndex = Math.floorMod(variantIndex + 1, variantsForJei().size());
        if (layerIndex >= variant().width()) {
            layerIndex = -1;
        }
        selectedBlock = null;
        selectedStackPage = 0;
        materialPage = 0;
    }

    public void nextTier() {
        tierIndex = Math.floorMod(tierIndex + 1, tiers().size());
        selectedStackPage = 0;
        materialPage = 0;
    }

    public void resetView() {
        viewYaw = 225.0F;
        viewPitch = 30.0F;
        zoom = 1.0F;
    }

    public void dragView(double dragX, double dragY) {
        viewYaw += (float) dragX * 0.7F;
        viewPitch = clamp(viewPitch + (float) dragY * 0.7F, -65.0F, 65.0F);
    }

    public void zoom(double scrollDeltaY) {
        float factor = scrollDeltaY > 0.0D ? 1.12F : 0.88F;
        zoom = clamp(zoom * factor, 0.45F, 2.5F);
    }

    public void selectBlock(int x, int y, int z, char symbol) {
        selectedBlock = symbol == MultiblockPattern.air || symbol == MultiblockPattern.ignore ? null : new SelectedBlock(x, y, z, symbol);
        selectedStackPage = 0;
    }

    public void nextSelectedStackPage(int pageSize) {
        if (selectedBlock == null || pageSize <= 0) {
            selectedStackPage = 0;
            return;
        }

        int pages = Math.max(1, (validBlockEntries(selectedBlock.symbol()).size() + pageSize - 1) / pageSize);
        selectedStackPage = Math.floorMod(selectedStackPage + 1, pages);
    }

    public void nextMaterialPage(int pageSize) {
        if (selectedBlock != null || pageSize <= 0) {
            return;
        }
        int pages = Math.max(1, (materialEntries().size() + pageSize - 1) / pageSize);
        materialPage = Math.floorMod(materialPage + 1, pages);
    }

    public int materialPage(int pageSize) {
        if (pageSize <= 0) {
            return 0;
        }
        int pages = Math.max(1, (materialEntries().size() + pageSize - 1) / pageSize);
        return Math.min(materialPage, pages - 1);
    }

    public List<ItemStack> validStacks(char symbol) {
        Set<String> seen = new LinkedHashSet<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (ValidBlockEntry entry : validBlockEntries(symbol)) {
            for (ItemStack stack : entry.stacks()) {
                if (!stack.isEmpty() && seen.add(itemKey(stack))) {
                    stacks.add(stack);
                }
            }
        }
        return List.copyOf(stacks);
    }

    public List<ValidBlockEntry> validBlockEntries(char symbol) {
        List<MultiblockPredicate.DisplayOption> options = definition.displayOptions(symbol, tier());
        if (options.isEmpty()) {
            List<ItemStack> stacks = uniqueStacks(definition.visualization().validStacks(
                    symbol,
                    tier(),
                    definition.controller(),
                    definition.controllerSymbol()
            ));
            return stacks.isEmpty() ? List.of() : List.of(new ValidBlockEntry(stacks, List.of()));
        }

        List<ValidBlockEntry> entries = new ArrayList<>();
        for (MultiblockPredicate.DisplayOption option : options) {
            List<ItemStack> stacks = validStacksForOption(option);
            if (!stacks.isEmpty()) {
                entries.add(new ValidBlockEntry(stacks, displayOptionTooltip(option)));
            }
        }
        return List.copyOf(entries);
    }

    public List<String> optionSummaryLines(char symbol) {
        List<MultiblockPredicate.DisplayOption> options = definition.displayOptions(symbol, tier());
        if (options.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        for (MultiblockPredicate.DisplayOption option : options) {
            lines.add(displayOptionLabel(option) + displayOptionConstraintSuffix(option));
        }
        return List.copyOf(lines);
    }

    public ItemStack previewStack(int x, int y, int z, char symbol) {
        List<ItemStack> stacks = previewPlan().get(new PreviewPos(x, y, z));
        if (stacks == null || stacks.isEmpty()) {
            return previewRepresentative(validStacks(symbol));
        }
        return previewRepresentative(stacks);
    }

    private ItemStack previewRepresentative(List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof MachinePortBlock port
                    && port.effectiveTier() == tier()) {
                return stack;
            }
        }
        return stacks.getFirst();
    }

    private Map<PreviewPos, List<ItemStack>> previewPlan() {
        PatternVariant variant = variant();
        Map<Character, List<PreviewPos>> positionsBySymbol = new LinkedHashMap<>();
        for (int x = 0; x < variant.width(); x++) {
            for (int y = 0; y < variant.height(); y++) {
                for (int z = 0; z < variant.length(); z++) {
                    char symbol = variant.symbolAt(x, y, z);
                    if (symbol == MultiblockPattern.air || symbol == MultiblockPattern.ignore) {
                        continue;
                    }
                    positionsBySymbol.computeIfAbsent(symbol, ignored -> new ArrayList<>()).add(new PreviewPos(x, y, z));
                }
            }
        }

        Map<PreviewPos, List<ItemStack>> plan = new LinkedHashMap<>();
        for (Map.Entry<Character, List<PreviewPos>> symbolEntry : positionsBySymbol.entrySet()) {
            char symbol = symbolEntry.getKey();
            List<PreviewPos> positions = symbolEntry.getValue();

            if (symbol == definition.controllerSymbol()) {
                List<ItemStack> controllerStacks = definition.visualization().validStacks(symbol, tier(), definition.controller(), definition.controllerSymbol());
                for (PreviewPos position : positions) {
                    plan.put(position, controllerStacks);
                }
                continue;
            }

            List<MultiblockPredicate.DisplayOption> options = definition.displayOptions(symbol, tier());
            if (options.isEmpty()) {
                List<ItemStack> fallback = uniqueStacks(definition.visualization().validStacks(symbol, tier(), definition.controller(), definition.controllerSymbol()));
                for (PreviewPos position : positions) {
                    plan.put(position, fallback);
                }
                continue;
            }

            List<PreviewChoice> choices = new ArrayList<>();
            for (MultiblockPredicate.DisplayOption option : options) {
                List<ItemStack> stacks = validStacksForOption(option);
                if (!stacks.isEmpty()) {
                    choices.add(new PreviewChoice(option, stacks, isAbilityOption(option)));
                }
            }
            if (choices.isEmpty()) {
                continue;
            }

            int[] counts = previewCounts(choices, positions.size());
            int positionIndex = 0;
            for (int choiceIndex = 0; choiceIndex < choices.size(); choiceIndex++) {
                PreviewChoice choice = choices.get(choiceIndex);
                for (int count = 0; count < counts[choiceIndex] && positionIndex < positions.size(); count++) {
                    plan.put(positions.get(positionIndex++), choice.stacks());
                }
            }

            List<ItemStack> fallback = choices.stream()
                    .filter(choice -> !choice.ability())
                    .map(PreviewChoice::stacks)
                    .findFirst()
                    .orElse(choices.getFirst().stacks());
            while (positionIndex < positions.size()) {
                plan.put(positions.get(positionIndex++), fallback);
            }
        }
        return Map.copyOf(plan);
    }

    private static int[] previewCounts(List<PreviewChoice> choices, int positionCount) {
        int[] counts = new int[choices.size()];
        int used = 0;

        for (int index = 0; index < choices.size() && used < positionCount; index++) {
            MultiblockPredicate.DisplayOption option = choices.get(index).option();
            int minimum = option.hasMinimum() ? option.min() : 0;
            if (option.hasMaximum()) {
                minimum = Math.min(minimum, option.max());
            }
            int amount = Math.min(minimum, positionCount - used);
            counts[index] = amount;
            used += amount;
        }

        for (int index = 0; index < choices.size() && used < positionCount; index++) {
            PreviewChoice choice = choices.get(index);
            MultiblockPredicate.DisplayOption option = choice.option();
            if (!choice.ability() || counts[index] > 0 || (option.hasMaximum() && option.max() == 0)) {
                continue;
            }
            counts[index] = 1;
            used++;
        }

        while (used < positionCount) {
            int fillIndex = -1;
            for (int index = 0; index < choices.size(); index++) {
                MultiblockPredicate.DisplayOption option = choices.get(index).option();
                if (!choices.get(index).ability() && (!option.hasMaximum() || counts[index] < option.max())) {
                    fillIndex = index;
                    break;
                }
            }
            if (fillIndex < 0) {
                for (int index = 0; index < choices.size(); index++) {
                    MultiblockPredicate.DisplayOption option = choices.get(index).option();
                    if (!option.hasMaximum() || counts[index] < option.max()) {
                        fillIndex = index;
                        break;
                    }
                }
            }
            if (fillIndex < 0) {
                break;
            }
            counts[fillIndex]++;
            used++;
        }

        return counts;
    }

    private List<ItemStack> validStacksForOption(MultiblockPredicate.DisplayOption option) {
        MultiblockVisualization.SymbolInfo info = option.info();
        if (!isAbilityOption(option)) {
            return uniqueStacks(info.validStacks(tier()));
        }

        List<ItemStack> stacks = new ArrayList<>();
        BlockRegistry.getAllMachinePorts().forEach(holder -> {
            MachinePortBlock port = holder.get();
            if (matchesAbilityInfo(port, info) && matchesOptionTier(port.effectiveTier(), option)) {
                stacks.add(new ItemStack(port));
            }
        });
        BlockRegistry.getAllStaticMachinePorts().forEach(holder -> {
            MachinePortBlock port = holder.get();
            if (matchesAbilityInfo(port, info) && matchesOptionTier(port.effectiveTier(), option)) {
                stacks.add(new ItemStack(port));
            }
        });

        List<ItemStack> sorted = new ArrayList<>(uniqueStacks(stacks));
        sorted.sort(Comparator
                .comparingInt(MultiblockJeiRecipe::abilityPortKind)
                .thenComparingInt(MultiblockJeiRecipe::abilityTierOrder)
                .thenComparing(stack -> stack.getHoverName().getString()));
        return List.copyOf(sorted);
    }

    private static boolean matchesAbilityInfo(MachinePortBlock port, MultiblockVisualization.SymbolInfo info) {
        if (!port.abilities().containsAll(info.requiredAbilities())) {
            return false;
        }
        return info.anyAbilities().isEmpty() || info.anyAbilities().stream().anyMatch(port.abilities()::contains);
    }

    private static boolean matchesOptionTier(MachineTier actualTier, MultiblockPredicate.DisplayOption option) {
        if (!option.hasTierRestriction()) {
            return true;
        }
        MachineTier limit = option.tier();
        if (option.exactTier()) {
            return actualTier == limit;
        }
        if (actualTier.family() != limit.family()) {
            return false;
        }
        List<MachineTier> family = limit.isSteam() ? MachineTier.STEAM_SINGLEBLOCK_TIERS : MachineTier.ELECTRIC_TIERS;
        int actualIndex = family.indexOf(actualTier);
        int limitIndex = family.indexOf(limit);
        return actualIndex >= 0 && limitIndex >= 0 && actualIndex <= limitIndex;
    }

    private static int abilityPortKind(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof MachinePortBlock port)) {
            return 2;
        }
        return port.staticPortType() == null ? 0 : 1;
    }

    private static int abilityTierOrder(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof MachinePortBlock port)) {
            return Integer.MAX_VALUE;
        }
        MachineTier tier = port.effectiveTier();
        int electricIndex = MachineTier.ELECTRIC_TIERS.indexOf(tier);
        if (electricIndex >= 0) {
            return electricIndex;
        }
        int steamIndex = MachineTier.STEAM_SINGLEBLOCK_TIERS.indexOf(tier);
        return steamIndex >= 0 ? 100 + steamIndex : 1000;
    }

    private static boolean isAbilityOption(MultiblockPredicate.DisplayOption option) {
        MultiblockVisualization.SymbolInfo info = option.info();
        return !info.requiredAbilities().isEmpty() || !info.anyAbilities().isEmpty();
    }

    private static List<ItemStack> uniqueStacks(List<ItemStack> candidates) {
        Set<String> seen = new LinkedHashSet<>();
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : candidates) {
            if (!stack.isEmpty() && seen.add(itemKey(stack))) {
                result.add(stack);
            }
        }
        return List.copyOf(result);
    }

    private static ItemStack cyclingStack(List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = Math.floorMod((int) (System.currentTimeMillis() / 1000L), stacks.size());
        return stacks.get(index);
    }

    public List<ItemStack> allValidStacks() {
        Set<String> seen = new LinkedHashSet<>();
        List<ItemStack> stacks = new ArrayList<>();
        PatternVariant variant = variant();
        for (int x = 0; x < variant.width(); x++) {
            for (int y = 0; y < variant.height(); y++) {
                for (int z = 0; z < variant.length(); z++) {
                    addStacks(stacks, seen, validStacks(variant.symbolAt(x, y, z)));
                }
            }
        }
        return stacks;
    }

    public List<MaterialEntry> materialEntries() {
        PatternVariant variant = variant();
        Map<Character, Integer> symbolCounts = new LinkedHashMap<>();
        for (int x = 0; x < variant.width(); x++) {
            for (int y = 0; y < variant.height(); y++) {
                for (int z = 0; z < variant.length(); z++) {
                    char symbol = variant.symbolAt(x, y, z);
                    if (symbol != MultiblockPattern.air && symbol != MultiblockPattern.ignore) {
                        symbolCounts.merge(symbol, 1, Integer::sum);
                    }
                }
            }
        }

        Map<String, MaterialEntry> entries = new LinkedHashMap<>();
        for (Map.Entry<Character, Integer> symbolCount : symbolCounts.entrySet()) {
            char symbol = symbolCount.getKey();
            int count = symbolCount.getValue();
            List<ItemStack> validStacks = validStacks(symbol);
            MultiblockVisualization.SymbolInfo info = definition.visualization().symbols().get(symbol);
            List<MultiblockPredicate.CountRequirement> countRequirements = definition.countRequirements(symbol);
            for (MultiblockPredicate.CountRequirement requirement : countRequirements) {
                if (requirement.blockId() == null) {
                    continue;
                }
                ItemStack stack = stackFor(requirement.blockId());
                if (!stack.isEmpty()) {
                    if (requirement.hasMinimum()) {
                        addMaterial(entries, List.of(stack), requirement.min(), false, countTooltip(requirement, requirement.min()), "min:" + requirement.key());
                    }
                    if (requirement.hasMaximum()) {
                        int total = Math.min(count, requirement.max());
                        addMaterial(entries, List.of(stack), total, false, countTooltip(requirement, total), "max:" + requirement.key());
                    }
                }
            }

            if (validStacks.isEmpty()) {
                continue;
            }

            if (isAbilitySymbol(info)) {
                addAbilityEntries(entries, info, validStacks);
            }

            int blockCount = count - abilityCount(info);
            if (blockCount > 0 && hasConcreteBlocks(info) && countRequirements.isEmpty()) {
                addMaterial(entries, List.of(firstConcreteStack(info, validStacks)), blockCount, false, List.of("Total: " + blockCount), "block:" + itemKey(firstConcreteStack(info, validStacks)));
            }
        }
        return List.copyOf(entries.values());
    }

    public int materialBlockCount() {
        PatternVariant variant = variant();
        int count = 0;
        for (int x = 0; x < variant.width(); x++) {
            for (int y = 0; y < variant.height(); y++) {
                for (int z = 0; z < variant.length(); z++) {
                    char symbol = variant.symbolAt(x, y, z);
                    if (symbol != MultiblockPattern.air && symbol != MultiblockPattern.ignore) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void addStacks(List<ItemStack> stacks, Set<String> seen, List<ItemStack> candidates) {
        for (ItemStack stack : candidates) {
            String key = itemKey(stack);
            if (seen.add(key)) {
                stacks.add(stack);
            }
        }
    }

    private static boolean isAbilitySymbol(MultiblockVisualization.SymbolInfo info) {
        return info != null && (!info.requiredAbilities().isEmpty() || !info.anyAbilities().isEmpty());
    }

    private static int abilityCount(MultiblockVisualization.SymbolInfo info) {
        if (info == null) {
            return 0;
        }
        Set<MultiblockAbility> abilities = new LinkedHashSet<>();
        abilities.addAll(info.requiredAbilities());
        abilities.addAll(info.anyAbilities());
        return abilities.size();
    }

    private static boolean hasConcreteBlocks(MultiblockVisualization.SymbolInfo info) {
        return info == null || !info.blockIds().isEmpty() || !info.tieredBlocks().isEmpty() || info.tieredMachineCasing();
    }

    private static ItemStack firstConcreteStack(MultiblockVisualization.SymbolInfo info, List<ItemStack> validStacks) {
        if (info == null) {
            return validStacks.getFirst();
        }
        for (ItemStack stack : validStacks) {
            if (!(stack.getItem() instanceof BlockItem blockItem)
                    || !(blockItem.getBlock() instanceof net.mads.industron.machine.MachinePortBlock)) {
                return stack;
            }
        }
        return validStacks.getFirst();
    }

    private static void addAbilityEntries(Map<String, MaterialEntry> entries, MultiblockVisualization.SymbolInfo info, List<ItemStack> validStacks) {
        Set<MultiblockAbility> abilities = new LinkedHashSet<>();
        abilities.addAll(info.requiredAbilities());
        abilities.addAll(info.anyAbilities());
        for (MultiblockAbility ability : abilities) {
            List<ItemStack> stacks = validStacks.stream()
                    .filter(stack -> stack.getItem() instanceof BlockItem blockItem
                            && blockItem.getBlock() instanceof net.mads.industron.machine.MachinePortBlock port
                            && port.abilities().contains(ability))
                    .toList();
            if (!stacks.isEmpty()) {
                addMaterial(entries, stacks, 1, true, List.of("Ability: " + abilityLabel(ability)), "ability:" + ability.name());
            }
        }
    }

    private static String abilityLabel(MultiblockAbility ability) {
        return switch (ability) {
            case ITEM_INPUT -> "Item In";
            case ITEM_OUTPUT -> "Item Out";
            case FLUID_INPUT -> "Fluid In";
            case CB_INPUT -> "CB In";
            case FLUID_OUTPUT -> "Fluid Out";
            case ENERGY_INPUT -> "Energy In";
            case ENERGY_OUTPUT -> "Energy Out";
            case KINETIC_INPUT -> "Kinetic In";
            case KINETIC_OUTPUT -> "Kinetic Out";
            case IO_INTERFACE -> "I/O";
            case MUFFLER -> "Muffler";
            case REDSTONE -> "Redstone";
        };
    }

    private List<String> displayOptionTooltip(MultiblockPredicate.DisplayOption option) {
        List<String> lines = new ArrayList<>();
        Set<MultiblockAbility> abilities = displayAbilities(option.info());
        if (!abilities.isEmpty()) {
            lines.add("Ability: " + abilityLabels(abilities));
        }
        if (option.hasMinimum()) {
            lines.add("Minimum: " + option.min());
        }
        if (option.hasMaximum()) {
            lines.add("Maximum: " + option.max());
        }
        if (option.hasTierRestriction()) {
            lines.add(option.exactTier() ? "Tier: " + option.tier().displayName() + " only" : "Tier: up to " + option.tier().displayName());
        }
        if (option.sequentialInput() > 0) {
            lines.add("Sequential input: " + option.sequentialInput());
        }
        if (option.sequentialOutput() > 0) {
            lines.add("Sequential output: " + option.sequentialOutput());
        }
        return List.copyOf(lines);
    }

    private String displayOptionLabel(MultiblockPredicate.DisplayOption option) {
        MultiblockVisualization.SymbolInfo info = option.info();
        Set<MultiblockAbility> abilities = displayAbilities(info);
        if (!abilities.isEmpty()) {
            return "Ability: " + abilityLabels(abilities);
        }

        List<ItemStack> stacks = info.validStacks(tier());
        return stacks.isEmpty() ? "Valid block" : stacks.getFirst().getHoverName().getString();
    }

    private static Set<MultiblockAbility> displayAbilities(MultiblockVisualization.SymbolInfo info) {
        Set<MultiblockAbility> abilities = new LinkedHashSet<>();
        abilities.addAll(info.requiredAbilities());
        abilities.addAll(info.anyAbilities());
        return abilities;
    }

    private static String displayOptionConstraintSuffix(MultiblockPredicate.DisplayOption option) {
        List<String> constraints = new ArrayList<>();
        if (option.hasMinimum()) {
            constraints.add("min " + option.min());
        }
        if (option.hasMaximum()) {
            constraints.add("max " + option.max());
        }
        if (option.hasTierRestriction()) {
            constraints.add(option.exactTier() ? option.tier().displayName() + " only" : "<= " + option.tier().displayName());
        }
        if (option.sequentialInput() > 0) {
            constraints.add("seq in " + option.sequentialInput());
        }
        if (option.sequentialOutput() > 0) {
            constraints.add("seq out " + option.sequentialOutput());
        }
        return constraints.isEmpty() ? "" : " [" + String.join(", ", constraints) + "]";
    }

    private static String abilityLabels(Set<MultiblockAbility> abilities) {
        return abilities.stream()
                .map(MultiblockJeiRecipe::abilityLabel)
                .sorted()
                .reduce((first, second) -> first + ", " + second)
                .orElse("Unknown");
    }

    private static List<String> countTooltip(MultiblockPredicate.CountRequirement requirement, int total) {
        List<String> lines = new ArrayList<>();
        lines.add("Total: " + total);
        if (requirement.hasMinimum()) {
            lines.add("Minimum: " + requirement.min());
        }
        if (requirement.hasMaximum()) {
            lines.add("Maximum: " + requirement.max());
        }
        return lines;
    }

    private static void addMaterial(Map<String, MaterialEntry> entries, List<ItemStack> stacks, int count, boolean ability, List<String> tooltip, String key) {
        if (stacks.isEmpty()) {
            return;
        }
        entries.putIfAbsent(key, new MaterialEntry(stacks.stream().map(stack -> withCount(stack, count)).toList(), ability, List.copyOf(tooltip)));
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(Math.max(1, count));
        return copy;
    }

    private static ItemStack stackFor(net.minecraft.resources.ResourceLocation blockId) {
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        ItemStack stack = new ItemStack(block);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    private static String itemKey(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().key().location().toString();
    }

    private List<PatternVariant> variantsForJei() {
        return definition.variants().stream()
                .sorted(Comparator.comparingInt(MultiblockJeiRecipe::displayVariantLevel))
                .toList();
    }

    private static int displayVariantLevel(PatternVariant variant) {
        int level = variant.variantLevel();
        return level > 0 ? level : Integer.MAX_VALUE;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public record SelectedBlock(int x, int y, int z, char symbol) {
    }

    public record ValidBlockEntry(List<ItemStack> stacks, List<String> tooltip) {
        public ItemStack stack() {
            return cyclingStack(stacks);
        }
    }

    private record PreviewPos(int x, int y, int z) {
    }

    private record PreviewChoice(MultiblockPredicate.DisplayOption option, List<ItemStack> stacks, boolean ability) {
    }

    public record MaterialEntry(List<ItemStack> stacks, boolean ability, List<String> tooltip) {
        public ItemStack stack() {
            return stacks.isEmpty() ? ItemStack.EMPTY : stacks.getFirst();
        }
    }
}
