package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.plant.PlantPartItemCatalog;
import net.mads.industron.tool.ToolMaterialLookup;
import net.mads.industron.recipe.recipes.assembly.MaterialType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Code-first assembly recipe executed on a placed base block or an assembly workbench. */
public final class AssemblyRecipeDefinition {
    /** Chance scale used by Assembly. 10000 = 100.00%. */
    public static final int MAX_CHANCE = 10_000;

    public enum BaseKind { BLOCK, ITEM, ENTITY }
    public enum InputKind { MATERIAL, COMPONENT, PLANT_PART, ITEM, TOOL, WAIT }

    /** Extra item result produced only when the complete Assembly recipe finishes. */
    public record Byproduct(ResourceLocation itemId, int count, int chance) {
        public Byproduct {
            Objects.requireNonNull(itemId, "itemId");
            if (count < 1) throw new IllegalArgumentException("Byproduct count must be >= 1");
            validateChance(chance);
        }

        Byproduct withChance(int value) {
            return new Byproduct(itemId, count, value);
        }
    }

    public record BaseValue(
            BaseKind kind,
            ResourceLocation id,
            MaterialPart material,
            PlantPart plantPart,
            AssemblyMaterialSelector materialSelector,
            List<AssemblyRequirement> requirements,
            String captureRole
    ) {
        public BaseValue {
            Objects.requireNonNull(kind, "kind");
            requirements = requirements == null ? List.of() : List.copyOf(requirements);
            if (captureRole != null && captureRole.isBlank()) throw new IllegalArgumentException("Base capture role cannot be blank");

            boolean exact = id != null;
            boolean materialBased = material != null || materialSelector != null;
            boolean plantBased = plantPart != null;
            int modes = (exact ? 1 : 0) + (materialBased ? 1 : 0) + (plantBased ? 1 : 0);
            if (modes != 1) {
                throw new IllegalArgumentException(
                        "Assembly base must be one exact registry id, one material selector, or one PlantPart selector"
                );
            }
            if (materialBased) {
                Objects.requireNonNull(material, "material");
                Objects.requireNonNull(materialSelector, "materialSelector");
                if (material.isFluid()) {
                    throw new IllegalArgumentException("Assembly base cannot be a fluid material part: " + material);
                }
                if (kind == BaseKind.BLOCK && !material.isBlock() && material != MaterialPart.PEBBLE) {
                    throw new IllegalArgumentException("Assembly block base requires a block material part (or the placeable Pebble form), got Material." + material.name());
                }
            }
            if (plantBased) {
                if (kind != BaseKind.ITEM) {
                    throw new IllegalArgumentException("PlantPart Assembly bases are item bases only");
                }
                if (!requirements.isEmpty() || captureRole != null) {
                    throw new IllegalArgumentException("PlantPart Assembly bases do not support material stats or captures");
                }
            }
        }

        public BaseValue(BaseKind kind, ResourceLocation id) {
            this(kind, id, null, null, null, List.of(), null);
        }

        public static BaseValue material(BaseKind kind, MaterialPart part, AssemblyMaterialSelector selector) {
            return new BaseValue(kind, null, part, null, selector, List.of(), null);
        }

        public static BaseValue plantPart(PlantPart part) {
            return new BaseValue(BaseKind.ITEM, null, null, Objects.requireNonNull(part), null, List.of(), null);
        }

        public boolean isMaterialSelection() {
            return material != null;
        }

        public boolean isPlantPartSelection() {
            return plantPart != null;
        }

        BaseValue withRequirement(AssemblyRequirement requirement) {
            if (!isMaterialSelection()) {
                throw new IllegalStateException("Assembly base stats require a material-selected base");
            }
            List<AssemblyRequirement> next = new ArrayList<>(requirements);
            next.add(Objects.requireNonNull(requirement));
            return new BaseValue(kind, null, material, null, materialSelector, next, captureRole);
        }

        BaseValue withCapture(String role) {
            if (!isMaterialSelection()) {
                throw new IllegalStateException("Assembly base capture requires a material-selected base");
            }
            if (role == null || role.isBlank()) throw new IllegalArgumentException("Base capture role cannot be blank");
            return new BaseValue(kind, null, material, null, materialSelector, requirements, role);
        }
    }

    public record RootInput(
            InputKind kind,
            MaterialPart material,
            PlantPart plantPart,
            AssemblyMaterialSelector materialSelector,
            AssemblyComponent component,
            ResourceLocation itemId,
            AssemblyToolType tool,
            int count,
            List<AssemblyRequirement> requirements,
            List<AssemblyCapturedRequirement> capturedRequirements,
            String captureRole,
            int waitTicks,
            SoundEvent sound,
            int consumeChance
    ) {
        public RootInput {
            Objects.requireNonNull(kind, "kind");
            if (count < 1) throw new IllegalArgumentException("Input count must be >= 1");
            requirements = List.copyOf(requirements);
            capturedRequirements = capturedRequirements == null ? List.of() : List.copyOf(capturedRequirements);
            if (captureRole != null && captureRole.isBlank()) throw new IllegalArgumentException("Input capture role cannot be blank");
            if (captureRole != null && kind != InputKind.MATERIAL) {
                throw new IllegalArgumentException("Only direct material inputs can be captured");
            }
            validateChance(consumeChance);
            if ((kind == InputKind.TOOL || kind == InputKind.WAIT) && consumeChance != MAX_CHANCE) {
                throw new IllegalArgumentException("Chance only applies to consumed Assembly inputs");
            }
        }

        /** Compatibility constructor: ordinary inputs are consumed with 100% chance. */
        public RootInput(
                InputKind kind,
                MaterialPart material,
                PlantPart plantPart,
                AssemblyMaterialSelector materialSelector,
                AssemblyComponent component,
                ResourceLocation itemId,
                AssemblyToolType tool,
                int count,
                List<AssemblyRequirement> requirements,
                List<AssemblyCapturedRequirement> capturedRequirements,
                String captureRole,
                int waitTicks,
                SoundEvent sound
        ) {
            this(kind, material, plantPart, materialSelector, component, itemId, tool, count, requirements, capturedRequirements, captureRole, waitTicks, sound, MAX_CHANCE);
        }

        RootInput withSound(SoundEvent value) {
            return new RootInput(kind, material, plantPart, materialSelector, component, itemId, tool, count, requirements, capturedRequirements, captureRole, waitTicks, value, consumeChance);
        }

        RootInput withRequirement(AssemblyRequirement requirement) {
            List<AssemblyRequirement> next = new ArrayList<>(requirements);
            next.add(Objects.requireNonNull(requirement));
            return new RootInput(kind, material, plantPart, materialSelector, component, itemId, tool, count, next, capturedRequirements, captureRole, waitTicks, sound, consumeChance);
        }

        RootInput withCapturedRequirement(AssemblyCapturedRequirement requirement) {
            List<AssemblyCapturedRequirement> next = new ArrayList<>(capturedRequirements);
            next.add(Objects.requireNonNull(requirement));
            return new RootInput(kind, material, plantPart, materialSelector, component, itemId, tool, count, requirements, next, captureRole, waitTicks, sound, consumeChance);
        }

        RootInput withCapture(String role) {
            if (kind != InputKind.MATERIAL) throw new IllegalStateException("Only direct Material inputs can be captured");
            if (count != 1) throw new IllegalStateException("Captured material inputs must have count 1");
            if (role == null || role.isBlank()) throw new IllegalArgumentException("Input capture role cannot be blank");
            return new RootInput(kind, material, plantPart, materialSelector, component, itemId, tool, count, requirements, capturedRequirements, role, waitTicks, sound, consumeChance);
        }

        RootInput withChance(int value) {
            if (kind == InputKind.TOOL || kind == InputKind.WAIT) {
                throw new IllegalStateException("chance() only applies to consumed Assembly inputs");
            }
            return new RootInput(kind, material, plantPart, materialSelector, component, itemId, tool, count, requirements, capturedRequirements, captureRole, waitTicks, sound, value);
        }
    }

    private final String id;
    private final BaseValue baseInput;
    private final BaseValue baseOutput;
    private final int baseOutputCount;
    private final int baseOutputChance;
    private final int level;
    private final List<Byproduct> byproducts;
    private final List<RootInput> inputs;
    private final ToolDefinition toolOutput;

    private AssemblyRecipeDefinition(
            String id,
            BaseValue baseInput,
            BaseValue baseOutput,
            int baseOutputCount,
            int baseOutputChance,
            int level,
            List<Byproduct> byproducts,
            List<RootInput> inputs,
            ToolDefinition toolOutput
    ) {
        this.id = id;
        this.baseInput = baseInput;
        this.baseOutput = baseOutput;
        this.baseOutputCount = baseOutputCount;
        this.baseOutputChance = baseOutputChance;
        this.level = level;
        this.byproducts = List.copyOf(byproducts);
        this.inputs = List.copyOf(inputs);
        this.toolOutput = toolOutput;
    }

    public String id() { return id; }
    public BaseValue baseInput() { return baseInput; }
    public BaseValue baseOutput() { return baseOutput; }
    public int baseOutputCount() { return baseOutputCount; }
    public int baseOutputChance() { return baseOutputChance; }
    /** Minimum Assembly Workbench level for item-base recipes. */
    public int level() { return level; }
    public List<Byproduct> byproducts() { return byproducts; }
    public List<RootInput> inputs() { return inputs; }
    public ToolDefinition toolOutput() { return toolOutput; }
    public boolean hasDynamicToolOutput() { return toolOutput != null; }

    /** Every block-base recipe is assembled directly on the placed base block. */
    public boolean usesWorldBlockRuntime() {
        return baseInput.kind() == BaseKind.BLOCK;
    }

    /** Every entity-base recipe is assembled directly on the clicked world entity. */
    public boolean usesWorldEntityRuntime() {
        return baseInput.kind() == BaseKind.ENTITY;
    }

    public boolean hasBlockBaseInput() { return baseInput.kind() == BaseKind.BLOCK; }
    public boolean hasItemBaseInput() { return baseInput.kind() == BaseKind.ITEM; }
    public boolean hasEntityBaseInput() { return baseInput.kind() == BaseKind.ENTITY; }
    public boolean hasBlockBaseOutput() { return baseOutput.kind() == BaseKind.BLOCK; }
    public boolean hasItemBaseOutput() { return baseOutput.kind() == BaseKind.ITEM; }
    public boolean hasEntityBaseOutput() { return baseOutput.kind() == BaseKind.ENTITY; }

    public Block baseBlock() {
        if (baseInput.kind() != BaseKind.BLOCK || baseInput.isMaterialSelection()) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use one exact block base input");
        }
        return BuiltInRegistries.BLOCK.getOptional(baseInput.id())
                .orElseThrow(() -> new IllegalStateException("Unknown base block: " + baseInput.id()));
    }

    public Block outputBlock() {
        if (baseOutput.kind() != BaseKind.BLOCK) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use a block base output");
        }
        return BuiltInRegistries.BLOCK.getOptional(baseOutput.id())
                .orElseThrow(() -> new IllegalStateException("Unknown output block: " + baseOutput.id()));
    }

    public Item baseItem() {
        if (baseInput.kind() != BaseKind.ITEM || baseInput.isMaterialSelection() || baseInput.isPlantPartSelection()) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use one exact item base input");
        }
        return BuiltInRegistries.ITEM.getOptional(baseInput.id())
                .orElseThrow(() -> new IllegalStateException("Unknown base item: " + baseInput.id()));
    }

    public Item outputItem() {
        if (baseOutput.kind() != BaseKind.ITEM) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use an item base output");
        }
        return BuiltInRegistries.ITEM.getOptional(baseOutput.id())
                .orElseThrow(() -> new IllegalStateException("Unknown output item: " + baseOutput.id()));
    }

    public AssemblyEntityDefinition baseEntityDefinition() {
        if (baseInput.kind() != BaseKind.ENTITY || baseInput.isMaterialSelection()) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use an entity base input");
        }
        return AssemblyEntityDefinition.require(baseInput.id());
    }

    public AssemblyEntityDefinition outputEntityDefinition() {
        if (baseOutput.kind() != BaseKind.ENTITY) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use an entity base output");
        }
        return AssemblyEntityDefinition.require(baseOutput.id());
    }

    public boolean matchesBaseEntity(Entity entity) {
        return baseInput.kind() == BaseKind.ENTITY && baseEntityDefinition().matches(entity);
    }

    /** Matches either an exact placed base block or a material-selected block base. */
    public boolean matchesBaseBlock(BlockState state) {
        if (baseInput.kind() != BaseKind.BLOCK || state == null) return false;
        if (!baseInput.isMaterialSelection()) return state.is(baseBlock());
        AssemblyMaterialCatalog.Target target = AssemblyMaterialCatalog.find(state.getBlock());
        return target != null && matchesMaterialBase(target);
    }

    /** Matches either an exact workbench base item or a material-selected item base. */
    public boolean matchesBaseItem(ItemStack stack) {
        if (baseInput.kind() != BaseKind.ITEM || stack == null || stack.isEmpty()) return false;
        if (baseInput.isPlantPartSelection()) {
            return PlantPartItemCatalog.contains(
                    baseInput.plantPart(),
                    BuiltInRegistries.ITEM.getKey(stack.getItem())
            );
        }
        if (!baseInput.isMaterialSelection()) return stack.is(baseItem());
        return matchesMaterialBase(stack);
    }

    private boolean matchesMaterialBase(ItemStack stack) {
        if (baseInput.materialSelector().isToolSelector()) {
            ToolMaterialLookup.Target target = ToolMaterialLookup.find(stack);
            if (target == null || target.part() != baseInput.material()) return false;
            if (!baseInput.materialSelector().matchesTool(target.material(), target.part())) return false;
            return baseInput.requirements().isEmpty();
        }
        AssemblyMaterialCatalog.Target target = AssemblyMaterialCatalog.find(stack);
        return target != null && matchesMaterialBase(target);
    }

    private boolean matchesMaterialBase(AssemblyMaterialCatalog.Target target) {
        if (target.part() != baseInput.material()) return false;
        if (!baseInput.materialSelector().matchesSubstance(target.material())) return false;
        if (baseInput.requirements().isEmpty()) return true;
        if (!(target.material() instanceof IndustrialMaterial industrial)) return false;
        return baseInput.requirements().stream()
                .allMatch(requirement -> requirement.matches(industrial, target.part()));
    }

    private static void validateChance(int chance) {
        if (chance < 0 || chance > MAX_CHANCE) {
            throw new IllegalArgumentException("Assembly chance must be between 0 and " + MAX_CHANCE);
        }
    }

    public static Builder recipe(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private enum ChanceTarget { NONE, BASE_OUTPUT, INPUT, BYPRODUCT }

        private final String id;
        private BaseValue baseInput;
        private BaseValue baseOutput;
        private int baseOutputCount = 1;
        private int baseOutputChance = MAX_CHANCE;
        private int level = 1;
        private final List<Byproduct> byproducts = new ArrayList<>();
        private final List<RootInput> inputs = new ArrayList<>();
        private int lastStatInput = -1;
        private int lastTierInput = -1;
        private boolean lastStatBase;
        private ChanceTarget lastChanceTarget = ChanceTarget.NONE;
        private int lastChanceIndex = -1;
        private ToolDefinition toolOutput;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
            if (id.isBlank()) throw new IllegalArgumentException("Assembly recipe id cannot be blank");
        }

        /** Minimum Assembly Workbench level. Level 1 recipes also work on every higher-level bench. */
        public Builder level(int value) {
            if (value < 1) throw new IllegalArgumentException("Assembly workbench level must be >= 1");
            level = value;
            return this;
        }

        public Builder baseBlockInput(String blockId) {
            baseInput = setBase(baseInput, BaseKind.BLOCK, ResourceLocation.parse(blockId), "input");
            return this;
        }

        public Builder baseBlockInput(Block block) {
            baseInput = setBase(baseInput, BaseKind.BLOCK, BuiltInRegistries.BLOCK.getKey(Objects.requireNonNull(block)), "input");
            return this;
        }

        /** Physical material block used as the placed assembly base. */
        public Builder baseBlockInput(MaterialPart material, IndustrialSubstance boundMaterial) {
            baseInput = setBase(
                    baseInput,
                    BaseKind.BLOCK,
                    resolveMaterialPartId(material, boundMaterial, BaseKind.BLOCK),
                    "input"
            );
            return this;
        }

        /** Material-selected placed block base. Stats added immediately afterwards apply to the base material itself. */
        public Builder baseBlockInput(MaterialPart material, AssemblyMaterialSelector selector) {
            baseInput = setMaterialBase(baseInput, BaseKind.BLOCK, material, selector, "input");
            lastStatInput = -1;
            lastStatBase = true;
            return this;
        }

        public Builder baseItemInput(String itemId) {
            baseInput = setBase(baseInput, BaseKind.ITEM, ResourceLocation.parse(itemId), "input");
            return this;
        }

        public Builder baseItemInput(Item item) {
            baseInput = setBase(baseInput, BaseKind.ITEM, BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(item)), "input");
            return this;
        }

        /** Any physical item registered for the requested plant-material role. */
        public Builder baseItemInput(PlantPart part) {
            if (baseInput != null) throw new IllegalStateException("Assembly recipe already has a base input");
            baseInput = BaseValue.plantPart(Objects.requireNonNull(part, "part"));
            lastStatInput = -1;
            lastStatBase = false;
            return this;
        }

        /** Physical material item used as the non-expanding workbench base. */
        public Builder baseItemInput(MaterialPart material, IndustrialSubstance boundMaterial) {
            baseInput = setBase(
                    baseInput,
                    BaseKind.ITEM,
                    resolveMaterialPartId(material, boundMaterial, BaseKind.ITEM),
                    "input"
            );
            return this;
        }

        /** Material-selected workbench base item. Stats added immediately afterwards apply to the base material itself. */
        public Builder baseItemInput(MaterialPart material, AssemblyMaterialSelector selector) {
            baseInput = setMaterialBase(baseInput, BaseKind.ITEM, material, selector, "input");
            lastStatInput = -1;
            lastStatBase = true;
            return this;
        }

        /** World entity used as the physical Assembly base. */
        public Builder baseEntityInput(AssemblyEntityDefinition entity) {
            AssemblyEntityDefinition definition = Objects.requireNonNull(entity, "entity");
            baseInput = setBase(baseInput, BaseKind.ENTITY, definition.id(), "input");
            return this;
        }

        public Builder baseBlockOutput(String blockId) { return baseBlockOutput(blockId, 1); }
        public Builder baseBlockOutput(String blockId, int count) {
            baseOutput = setBase(baseOutput, BaseKind.BLOCK, ResourceLocation.parse(blockId), "output");
            setOutputCount(count);
            markChanceTarget(ChanceTarget.BASE_OUTPUT, -1);
            return this;
        }

        public Builder baseBlockOutput(Block block) { return baseBlockOutput(block, 1); }
        public Builder baseBlockOutput(Block block, int count) {
            baseOutput = setBase(baseOutput, BaseKind.BLOCK, BuiltInRegistries.BLOCK.getKey(Objects.requireNonNull(block)), "output");
            setOutputCount(count);
            markChanceTarget(ChanceTarget.BASE_OUTPUT, -1);
            return this;
        }

        /** Physical material block used as the assembly result. */
        public Builder baseBlockOutput(MaterialPart material, IndustrialSubstance boundMaterial) {
            baseOutput = setBase(
                    baseOutput,
                    BaseKind.BLOCK,
                    resolveMaterialPartId(material, boundMaterial, BaseKind.BLOCK),
                    "output"
            );
            setOutputCount(1);
            markChanceTarget(ChanceTarget.BASE_OUTPUT, -1);
            return this;
        }

        public Builder baseItemOutput(String itemId) { return baseItemOutput(itemId, 1); }
        public Builder baseItemOutput(String itemId, int count) {
            baseOutput = setBase(baseOutput, BaseKind.ITEM, ResourceLocation.parse(itemId), "output");
            setOutputCount(count);
            markChanceTarget(ChanceTarget.BASE_OUTPUT, -1);
            return this;
        }

        public Builder baseItemOutput(Item item) { return baseItemOutput(item, 1); }
        public Builder baseItemOutput(Item item, int count) {
            baseOutput = setBase(baseOutput, BaseKind.ITEM, BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(item)), "output");
            setOutputCount(count);
            markChanceTarget(ChanceTarget.BASE_OUTPUT, -1);
            return this;
        }

        /** Physical material item/block-item used as the assembly result. */
        public Builder baseItemOutput(MaterialPart material, IndustrialSubstance boundMaterial) {
            baseOutput = setBase(
                    baseOutput,
                    BaseKind.ITEM,
                    resolveMaterialPartId(material, boundMaterial, BaseKind.ITEM),
                    "output"
            );
            setOutputCount(1);
            markChanceTarget(ChanceTarget.BASE_OUTPUT, -1);
            return this;
        }

        /** Entity spawned in the world when this Assembly recipe completes. */
        public Builder baseEntityOutput(AssemblyEntityDefinition entity) {
            AssemblyEntityDefinition definition = Objects.requireNonNull(entity, "entity");
            baseOutput = setBase(baseOutput, BaseKind.ENTITY, definition.id(), "output");
            setOutputCount(1);
            markChanceTarget(ChanceTarget.BASE_OUTPUT, -1);
            return this;
        }

        /** Adds an item byproduct produced when the complete recipe finishes. May be called repeatedly. */
        public Builder byproduct(String itemId, int count) {
            return byproduct(ResourceLocation.parse(itemId), count);
        }

        public Builder byproduct(Item item, int count) {
            return byproduct(BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(item)), count);
        }

        private Builder byproduct(ResourceLocation itemId, int count) {
            byproducts.add(new Byproduct(Objects.requireNonNull(itemId), count, MAX_CHANCE));
            markChanceTarget(ChanceTarget.BYPRODUCT, byproducts.size() - 1);
            return this;
        }

        /**
         * Applies a percentage chance to the immediately preceding consumed input, main output, or byproduct.
         * For inputs this is the chance that the item is consumed; it must still be present to perform the step.
         */
        public Builder chance(double percent) {
            int value = chanceFromPercent(percent);
            switch (lastChanceTarget) {
                case BASE_OUTPUT -> baseOutputChance = value;
                case INPUT -> {
                    if (lastChanceIndex < 0 || lastChanceIndex >= inputs.size()) {
                        throw new IllegalStateException("chance() has no previous Assembly input");
                    }
                    inputs.set(lastChanceIndex, inputs.get(lastChanceIndex).withChance(value));
                }
                case BYPRODUCT -> {
                    if (lastChanceIndex < 0 || lastChanceIndex >= byproducts.size()) {
                        throw new IllegalStateException("chance() has no previous Assembly byproduct");
                    }
                    byproducts.set(lastChanceIndex, byproducts.get(lastChanceIndex).withChance(value));
                }
                case NONE -> throw new IllegalStateException("chance() needs a previous consumed input, main output, or byproduct");
            }
            return this;
        }

        /** Material leaf with implicit free MaterialType.ANY selection in a normal recipe. */
        public Builder input(MaterialPart material) { return input(material, MaterialType.ANY, 1); }
        public Builder input(MaterialPart material, int count) { return input(material, MaterialType.ANY, count); }
        public Builder input(MaterialPart material, AssemblyMaterialSelector selector) { return input(material, selector, 1); }
        public Builder input(MaterialPart material, AssemblyMaterialSelector selector, int count) {
            addInput(new RootInput(InputKind.MATERIAL, requireItemMaterial(material), null, Objects.requireNonNull(selector), null, null, null, count, List.of(), List.of(), null, 0, null));
            return this;
        }

        /** Binds directly to the canonical IndustrialMaterial; no duplicate recipe-side material constant is needed. */
        public Builder input(MaterialPart material, IndustrialSubstance boundMaterial) {
            return input(material, boundMaterial, 1);
        }
        public Builder input(MaterialPart material, IndustrialSubstance boundMaterial, int count) {
            return input(material, AssemblyMaterialSelector.fixed(Objects.requireNonNull(boundMaterial)), count);
        }

        /** Component tree with implicit free MaterialType.ANY selection; each material leaf may differ. */
        public Builder input(AssemblyComponent component) { return input(component, MaterialType.ANY, 1); }
        public Builder input(AssemblyComponent component, int count) { return input(component, MaterialType.ANY, count); }
        public Builder input(AssemblyComponent component, AssemblyMaterialSelector selector) { return input(component, selector, 1); }
        public Builder input(AssemblyComponent component, AssemblyMaterialSelector selector, int count) {
            addInput(new RootInput(InputKind.COMPONENT, null, null, Objects.requireNonNull(selector), Objects.requireNonNull(component), null, null, count, List.of(), List.of(), null, 0, null));
            return this;
        }

        /** Binds the complete component tree directly to the canonical IndustrialMaterial. */
        public Builder input(AssemblyComponent component, IndustrialSubstance boundMaterial) {
            return input(component, boundMaterial, 1);
        }
        public Builder input(AssemblyComponent component, IndustrialSubstance boundMaterial, int count) {
            return input(component, AssemblyMaterialSelector.fixed(Objects.requireNonNull(boundMaterial)), count);
        }

        /** Plant-definition leaf such as any registered plant-derived string. */
        public Builder input(PlantPart part) { return input(part, 1); }
        public Builder input(PlantPart part, int count) {
            addInput(new RootInput(InputKind.PLANT_PART, null, Objects.requireNonNull(part), null, null, null, null, count, List.of(), List.of(), null, 0, null));
            return this;
        }

        public Builder input(String itemId) { return input(itemId, 1); }
        public Builder input(String itemId, int count) {
            addInput(new RootInput(InputKind.ITEM, null, null, null, null, ResourceLocation.parse(itemId), null, count, List.of(), List.of(), null, 0, null));
            return this;
        }

        public Builder input(AssemblyToolType tool) {
            return tool(tool, 1);
        }

        public Builder input(ToolDefinition tool) {
            return tool(tool, 1);
        }

        /** Public recipe syntax: .tool(Tool.PICKAXE, amount). */
        public Builder tool(ToolDefinition tool, int amount) {
            return tool(Objects.requireNonNull(tool).type(), amount);
        }

        /** Repeats the same non-consumed tool action the requested number of times. */
        public Builder tool(AssemblyToolType tool, int amount) {
            addInput(new RootInput(InputKind.TOOL, null, null, null, null, null, Objects.requireNonNull(tool), amount, List.of(), List.of(), null, 0, null));
            return this;
        }

        public Builder waitTicks(int ticks) {
            if (ticks < 1) throw new IllegalArgumentException("Wait must be >= 1 tick");
            addInput(new RootInput(InputKind.WAIT, null, null, null, null, null, null, 1, List.of(), List.of(), null, ticks, null));
            return this;
        }

        public Builder waitSeconds(double seconds) {
            if (!(seconds > 0.0D)) throw new IllegalArgumentException("Wait must be > 0 seconds");
            return waitTicks(Math.max(1, (int) Math.round(seconds * 20.0D)));
        }

        /** Adds a numeric material requirement to the previous Material/Component input. */
        public AssemblyNumericStatBuilder<Builder> stat(AssemblyCapability capability) {
            ensureStatTarget();
            return new AssemblyNumericStatBuilder<>(this, capability, this::addRequirementToLastTarget, this::addCapturedRequirementToLastTarget);
        }

        /** Adds a typed material requirement to the previous Material/Component input. */
        public <T> AssemblyPropertyStatBuilder<T, Builder> stat(AssemblyProperty<T> property) {
            ensureStatTarget();
            return new AssemblyPropertyStatBuilder<>(this, property, this::addRequirementToLastTarget);
        }

        /**
         * Requires the previous material-selected base/material input or tool step to use exactly
         * the requested progression tier. Recipe-family tiers are normalized through recipeTier(),
         * so future steam-family aliases can share the same progression band without recipes
         * hardcoding raw tier-multiplier numbers.
         */
        public Builder tier(MachineTier tier) {
            MachineTier normalized = Objects.requireNonNull(tier, "tier").recipeTier();
            if (!MachineTier.ALL.contains(normalized)) {
                throw new IllegalArgumentException("Unsupported Assembly tier: " + tier.id());
            }
            int tierIndex = MachineTierStats.tierIndex(normalized);
            AssemblyRequirement requirement = AssemblyCapability.TIER_MULTIPLIER.exactly(tierIndex + 1);
            if (lastStatBase) {
                baseInput = baseInput.withRequirement(requirement);
                return this;
            }
            if (lastTierInput < 0 || lastTierInput >= inputs.size()) {
                throw new IllegalStateException("tier() needs a previous material-selected base, Material/Component input, or Tool step");
            }
            RootInput current = inputs.get(lastTierInput);
            if (current.kind() != InputKind.MATERIAL
                    && current.kind() != InputKind.COMPONENT
                    && current.kind() != InputKind.TOOL) {
                throw new IllegalStateException("tier() cannot target Assembly input kind " + current.kind());
            }
            inputs.set(lastTierInput, current.withRequirement(requirement));
            return this;
        }

        /** Requires a Material input, or every material leaf of a Component, to match a captured role. */
        public Builder sameMaterialAs(String role) {
            ensureStatTarget();
            if (lastStatBase) {
                throw new IllegalStateException("sameMaterialAs() targets a consumed material input, not the Assembly base");
            }
            RootInput current = inputs.get(lastStatInput);
            if (current.kind() != InputKind.MATERIAL && current.kind() != InputKind.COMPONENT) {
                throw new IllegalStateException("sameMaterialAs() requires a Material or Component input");
            }
            inputs.set(lastStatInput, current.withCapturedRequirement(AssemblyCapturedRequirement.sameMaterial(role)));
            return this;
        }

        /** Captures the material identity of the previous direct Material input. */
        public Builder capture(String role) {
            if (lastStatBase) {
                baseInput = baseInput.withCapture(role);
                return this;
            }
            if (lastStatInput < 0 || lastStatInput >= inputs.size()) {
                throw new IllegalStateException("capture() needs a previous material-selected base or direct Material input");
            }
            RootInput current = inputs.get(lastStatInput);
            inputs.set(lastStatInput, current.withCapture(role));
            return this;
        }

        /** Marks this item result as a dynamic composed tool built from captured permanent parts. */
        public Builder toolOutput(ToolDefinition definition) {
            if (toolOutput != null) throw new IllegalStateException("Assembly recipe already has a dynamic tool output");
            toolOutput = Objects.requireNonNull(definition, "definition");
            return baseItemOutput(Industron.MOD_ID + ":" + definition.id());
        }

        public Builder sound(SoundEvent sound) {
            if (inputs.isEmpty()) throw new IllegalStateException("sound() needs a previous input");
            int index = inputs.size() - 1;
            inputs.set(index, inputs.get(index).withSound(Objects.requireNonNull(sound)));
            return this;
        }

        public AssemblyRecipeDefinition build() {
            if (baseInput == null || baseOutput == null || inputs.isEmpty()) {
                throw new IllegalStateException("Assembly recipe requires one base input, at least one input step, and one base output");
            }
            if (toolOutput != null) validateToolCaptures();
            return new AssemblyRecipeDefinition(
                    id, baseInput, baseOutput, baseOutputCount, baseOutputChance, level, byproducts, inputs, toolOutput
            );
        }

        private void validateToolCaptures() {
            java.util.Set<String> captures = new java.util.HashSet<>();
            if (baseInput.captureRole() != null) captures.add(baseInput.captureRole());
            for (RootInput input : inputs) {
                if (input.captureRole() != null && !captures.add(input.captureRole())) {
                    throw new IllegalStateException("Duplicate Assembly capture role in " + id + ": " + input.captureRole());
                }
            }
            for (ToolDefinition.PartSlot slot : toolOutput.parts()) {
                if (!captures.contains(slot.role())) {
                    throw new IllegalStateException("Tool recipe " + id + " does not capture required role: " + slot.role());
                }
            }
            for (RootInput input : inputs) {
                for (AssemblyCapturedRequirement requirement : input.capturedRequirements()) {
                    if (!captures.contains(requirement.captureRole())) {
                        throw new IllegalStateException("Assembly captured stat references unknown role "
                                + requirement.captureRole() + " in " + id);
                    }
                }
            }
        }

        private static int chanceFromPercent(double percent) {
            if (!Double.isFinite(percent) || percent < 0.0D || percent > 100.0D) {
                throw new IllegalArgumentException("Assembly chance must be between 0 and 100 percent");
            }
            return (int) Math.round(percent * 100.0D);
        }

        private static BaseValue setBase(BaseValue current, BaseKind kind, ResourceLocation id, String side) {
            if (current != null) throw new IllegalStateException("Assembly recipe already has a base " + side);
            return new BaseValue(kind, Objects.requireNonNull(id));
        }

        private static BaseValue setMaterialBase(
                BaseValue current,
                BaseKind kind,
                MaterialPart material,
                AssemblyMaterialSelector selector,
                String side
        ) {
            if (current != null) throw new IllegalStateException("Assembly recipe already has a base " + side);
            return BaseValue.material(kind, Objects.requireNonNull(material), Objects.requireNonNull(selector));
        }

        private void addInput(RootInput input) {
            inputs.add(input);
            lastStatBase = false;
            lastStatInput = switch (input.kind()) {
                case MATERIAL, COMPONENT -> inputs.size() - 1;
                case PLANT_PART, ITEM, TOOL, WAIT -> -1;
            };
            lastTierInput = switch (input.kind()) {
                case MATERIAL, COMPONENT, TOOL -> inputs.size() - 1;
                case PLANT_PART, ITEM, WAIT -> -1;
            };
            if (input.kind() == InputKind.MATERIAL || input.kind() == InputKind.COMPONENT || input.kind() == InputKind.PLANT_PART || input.kind() == InputKind.ITEM) {
                markChanceTarget(ChanceTarget.INPUT, inputs.size() - 1);
            } else {
                markChanceTarget(ChanceTarget.NONE, -1);
            }
        }

        private void setOutputCount(int count) {
            if (count < 1) throw new IllegalArgumentException("Assembly output count must be >= 1");
            baseOutputCount = count;
        }

        private void markChanceTarget(ChanceTarget target, int index) {
            lastChanceTarget = target;
            lastChanceIndex = index;
        }

        private void ensureStatTarget() {
            if (lastStatBase) return;
            if (lastStatInput < 0 || lastStatInput >= inputs.size()) {
                throw new IllegalStateException("stat() needs a previous material-selected base, Material, or Component input");
            }
        }

        private void addRequirementToLastTarget(AssemblyRequirement requirement) {
            ensureStatTarget();
            if (lastStatBase) {
                baseInput = baseInput.withRequirement(requirement);
                return;
            }
            RootInput current = inputs.get(lastStatInput);
            inputs.set(lastStatInput, current.withRequirement(requirement));
        }

        private void addCapturedRequirementToLastTarget(
                AssemblyCapability candidate,
                String captureRole,
                AssemblyCapability source,
                boolean atLeast
        ) {
            ensureStatTarget();
            if (lastStatBase) {
                throw new IllegalStateException("Cross-input stat requirements cannot target the assembly base");
            }
            RootInput current = inputs.get(lastStatInput);
            AssemblyCapturedRequirement requirement = atLeast
                    ? AssemblyCapturedRequirement.atLeast(candidate, captureRole, source)
                    : AssemblyCapturedRequirement.atMost(candidate, captureRole, source);
            inputs.set(lastStatInput, current.withCapturedRequirement(requirement));
        }

        private static MaterialPart requireItemMaterial(MaterialPart material) {
            Objects.requireNonNull(material, "material");
            if (material.isFluid()) throw new IllegalArgumentException("Assembly material input cannot be fluid: " + material);
            return material;
        }

        private static ResourceLocation resolveMaterialPartId(
                MaterialPart part,
                IndustrialSubstance substance,
                BaseKind kind
        ) {
            Objects.requireNonNull(part, "part");
            Objects.requireNonNull(substance, "material");
            Objects.requireNonNull(kind, "kind");
            IndustrialMaterial material = MaterialCatalog.require(substance.id());

            if (!material.has(part)) {
                throw new IllegalArgumentException(
                        "Material " + material.id() + " does not expose Material." + part.name()
                );
            }
            if (part.isFluid()) {
                throw new IllegalArgumentException("Assembly base cannot be a fluid material part: " + part);
            }
            if (kind == BaseKind.BLOCK && !part.isBlock()) {
                throw new IllegalArgumentException(
                        "Assembly block base/output requires a block material part, got Material." + part.name()
                );
            }

            if (material.hasExistingPart(part)) {
                return material.existingPart(part);
            }
            return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(material));
        }
    }
}
