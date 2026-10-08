package net.mads.industron.material.structure;

import net.mads.industron.block.BlockStrength;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialFormulaFormatter;
import net.mads.industron.material.MaterialOrePolicy;

import net.mads.industron.material.MaterialComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

public final class StoneMaterial implements StructureMaterial {
    private static final Set<MaterialPart> DEFAULT_FORMS = Set.of(
            MaterialPart.PEBBLE,
            MaterialPart.TINY_DUST,
            MaterialPart.SMALL_DUST,
            MaterialPart.DUST,
            MaterialPart.TOOL_HEAD_PICKAXE,
            MaterialPart.TOOL_HEAD_AXE,
            MaterialPart.TOOL_HEAD_SHOVEL,
            MaterialPart.TOOL_HEAD_HOE,
            MaterialPart.TOOL_HEAD_HAMMER,
            MaterialPart.TOOL_HEAD_CHISEL,
            MaterialPart.TOOL_HEAD_PESTLE,
            MaterialPart.KNIFE_BLADE
    );

    private final String id;
    private final String displayName;
    private final int color;
    private final StoneModel model;
    private final MachineTier tier;
    private final List<MaterialComponent> components;
    private final Map<MaterialPart, ResourceLocation> existingParts;
    private final Set<MaterialPart> withoutParts;
    private final BlockStrength defaultStrength;
    private final Map<MaterialPart, BlockStrength> partStrengths;
    private final MaterialOrePolicy.DimensionBand dimension;
    private final boolean baseRock;

    public StoneMaterial(String id, String displayName, int color, StoneModel model, MachineTier tier) {
        this(id, displayName, color, model, tier, List.of(), Map.of(), Set.of(), null, Map.of(), MaterialOrePolicy.DimensionBand.OVERWORLD, false);
    }

    private StoneMaterial(
            String id,
            String displayName,
            int color,
            StoneModel model,
            MachineTier tier,
            List<MaterialComponent> components,
            Map<MaterialPart, ResourceLocation> existingParts,
            Set<MaterialPart> withoutParts,
            BlockStrength defaultStrength,
            Map<MaterialPart, BlockStrength> partStrengths,
            MaterialOrePolicy.DimensionBand dimension,
            boolean baseRock
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Stone material id cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Stone material display name cannot be blank: " + id);
        }
        if (model == null) {
            throw new IllegalArgumentException("Stone model cannot be null: " + id);
        }
        if (tier == null || tier == MachineTier.NONE) {
            throw new IllegalArgumentException("Stone material tier must be a real tier: " + id);
        }
        this.id = id;
        this.displayName = displayName;
        this.color = color & 0x00FFFFFF;
        this.model = model;
        this.tier = tier;
        this.components = List.copyOf(components);
        this.existingParts = Map.copyOf(existingParts);
        this.withoutParts = Set.copyOf(withoutParts);
        this.defaultStrength = defaultStrength;
        this.partStrengths = Map.copyOf(partStrengths);
        this.dimension = dimension == null ? MaterialOrePolicy.DimensionBand.OVERWORLD : dimension;
        this.baseRock = baseRock;
    }

    public StoneMaterial contains(MaterialComponent... additions) {
        List<MaterialComponent> updated = new ArrayList<>(components);
        if (additions != null) {
            for (MaterialComponent component : additions) {
                if (component == null) {
                    throw new IllegalArgumentException("Stone material component cannot be null: " + id);
                }
                updated.add(component);
            }
        }
        return new StoneMaterial(id, displayName, color, model, tier, updated, existingParts, withoutParts, defaultStrength, partStrengths, dimension, baseRock);
    }

    public StoneMaterial existing(MaterialPart part, String resourceLocation) {
        ResourceLocation parsed = ResourceLocation.tryParse(resourceLocation);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid existing resource location '" + resourceLocation + "' for " + id);
        }
        return existing(part, parsed);
    }

    public StoneMaterial existing(MaterialPart part, ResourceLocation resourceLocation) {
        if (part == null || resourceLocation == null) {
            throw new IllegalArgumentException("Existing stone part and resource cannot be null: " + id);
        }
        if (withoutParts.contains(part)) {
            throw new IllegalArgumentException("Stone material " + id + " excludes " + part + " with .without(...)");
        }
        Map<MaterialPart, ResourceLocation> updated = new EnumMap<>(MaterialPart.class);
        updated.putAll(existingParts);
        ResourceLocation previous = updated.put(part, resourceLocation);
        if (previous != null && !previous.equals(resourceLocation)) {
            throw new IllegalArgumentException(
                    "Stone material " + id + " already maps " + part + " to " + previous
            );
        }
        return new StoneMaterial(id, displayName, color, model, tier, components, updated, withoutParts, defaultStrength, partStrengths, dimension, baseRock);
    }

    public StoneMaterial strength(float hardness) {
        return strength(hardness, hardness);
    }

    public StoneMaterial strength(float hardness, float resistance) {
        return new StoneMaterial(
                id, displayName, color, model, tier, components, existingParts, withoutParts,
                BlockStrength.of(hardness, resistance), partStrengths, dimension, baseRock
        );
    }

    public StoneMaterial strength(MaterialPart part, float hardness) {
        return strength(part, hardness, hardness);
    }

    public StoneMaterial strength(MaterialPart part, float hardness, float resistance) {
        if (part == null || !part.isBlock()) {
            throw new IllegalArgumentException("Stone strength override requires a block part: " + id + " " + part);
        }
        Map<MaterialPart, BlockStrength> updated = new EnumMap<>(MaterialPart.class);
        updated.putAll(partStrengths);
        updated.put(part, BlockStrength.of(hardness, resistance));
        return new StoneMaterial(
                id, displayName, color, model, tier, components, existingParts, withoutParts,
                defaultStrength, updated, dimension, baseRock
        );
    }

    public Optional<BlockStrength> strengthFor(MaterialPart part) {
        BlockStrength specific = part == null ? null : partStrengths.get(part);
        return Optional.ofNullable(specific != null ? specific : defaultStrength);
    }

    public StoneMaterial without(MaterialPart part) {
        if (part == null) {
            throw new IllegalArgumentException("Stone material part cannot be null: " + id);
        }
        if (existingParts.containsKey(part)) {
            throw new IllegalArgumentException(
                    "Stone material " + id + " already maps " + part + " to " + existingParts.get(part)
            );
        }
        Set<MaterialPart> updated = EnumSet.noneOf(MaterialPart.class);
        updated.addAll(withoutParts);
        updated.add(part);
        return new StoneMaterial(id, displayName, color, model, tier, components, existingParts, updated, defaultStrength, partStrengths, dimension, baseRock);
    }

    public boolean isWithout(MaterialPart part) {
        return withoutParts.contains(part);
    }

    public Set<MaterialPart> withoutParts() {
        return withoutParts;
    }

    /**
     * Declares the only dimension where this stone may be used by geology/worldgen.
     * The value is independent of StoneModel; model families never decide placement.
     */
    public StoneMaterial dimension(MaterialOrePolicy.DimensionBand dimension) {
        if (dimension == null) {
            throw new IllegalArgumentException("Stone dimension cannot be null: " + id);
        }
        return new StoneMaterial(id, displayName, color, model, tier, components, existingParts, withoutParts, defaultStrength, partStrengths, dimension, baseRock);
    }

    public MaterialOrePolicy.DimensionBand dimension() {
        return dimension;
    }

    /**
     * Marks this stone as the dimension's background rock. Base rocks are valid terrain that
     * geology may replace, but they are never emitted as the regional prospecting signal around
     * a deposit. This keeps Stone/Deepslate/Netherrack/End Stone as background geology.
     */
    public StoneMaterial baseRock() {
        return new StoneMaterial(id, displayName, color, model, tier, components, existingParts, withoutParts, defaultStrength, partStrengths, dimension, true);
    }

    public boolean isBaseRock() {
        return baseRock;
    }

    /** Compatibility alias for older definitions. Prefer {@link #dimension(MaterialOrePolicy.DimensionBand)}. */
    @Deprecated(forRemoval = false)
    public StoneMaterial oreHostDimension(MaterialOrePolicy.DimensionBand dimension) {
        return dimension(dimension);
    }

    /** Compatibility alias for older callers. Prefer {@link #dimension()}. */
    @Deprecated(forRemoval = false)
    public MaterialOrePolicy.DimensionBand oreHostDimension() {
        return dimension();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public int color() {
        return color;
    }

    @Override
    public MachineTier tier() {
        return tier;
    }

    @Override
    public StoneModel model() {
        return model;
    }

    @Override
    public List<MaterialComponent> components() {
        return components;
    }

    /**
     * Stone .contains(...) amounts are relative selection weights, not stoichiometric counts.
     * The formula therefore shows each contained trace material once while preserving each
     * trace material's own internal formula.
     */
    @Override
    public String formula(boolean nested) {
        return MaterialFormulaFormatter.compound(components, nested, false);
    }

    @Override
    public Map<MaterialPart, ResourceLocation> existingParts() {
        return existingParts;
    }

    @Override
    public Set<MaterialPart> generatedForms() {
        if (withoutParts.isEmpty()) {
            return DEFAULT_FORMS;
        }
        Set<MaterialPart> forms = EnumSet.copyOf(DEFAULT_FORMS);
        forms.removeAll(withoutParts);
        return Set.copyOf(forms);
    }
}
