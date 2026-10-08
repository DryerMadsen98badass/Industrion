package net.mads.industron.material.structure;

import net.mads.industron.block.BlockStrength;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPart;

import net.mads.industron.material.MaterialComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

public final class WoodMaterial implements StructureMaterial {
    private static final Set<MaterialPart> DEFAULT_FORMS = Set.of(
            // Item-only forms that every wood can generate when no .existing(...) mapping replaces them.
            // Block forms are discovered from StructureMaterialGenerator.blockDefinitions(...); keeping them
            // out of this set prevents systems such as FuelRecipes from treating texture-dependent blocks
            // (for example bamboo_wood or crimson_leaves) as registered when no such block was generated.
            MaterialPart.BOWL,
            MaterialPart.BOAT,
            MaterialPart.CHEST_BOAT,
            MaterialPart.TINY_WOOD_PULP,
            MaterialPart.SMALL_WOOD_PULP,
            MaterialPart.WOOD_PULP,
            MaterialPart.BARK,
            MaterialPart.STICK,
            MaterialPart.PLATE,
            MaterialPart.SMALL_GEAR,
            MaterialPart.GEAR,
            MaterialPart.RING,
            MaterialPart.WOOD_PEG,
            MaterialPart.TOOL_HANDLE,
            MaterialPart.TOOL_HEAD_MALLET,
            MaterialPart.SAW_HANDLE,
            MaterialPart.SIFTER_FRAME,
            MaterialPart.SHEARS_HANDLE,
            MaterialPart.CROSSBOW_LIMBS,
            MaterialPart.SHIELD_BODY,
            MaterialPart.SHIELD_HANDLE,
            MaterialPart.SNAP_RING_PLIERS_HANDLE,
            MaterialPart.BEARING_PRESS_HANDLE,
            MaterialPart.CLAMP_HANDLE,
            MaterialPart.CRIMPING_TOOL_HANDLE,
            MaterialPart.GEAR_CUTTER_HANDLE,
            MaterialPart.BOW_BODY,
            MaterialPart.CROSSBOW_STOCK,
            MaterialPart.FISHING_ROD_BODY
    );

    private final String id;
    private final String displayName;
    private final int color;
    private final WoodModel model;
    private final MachineTier tier;
    private final List<MaterialComponent> components;
    private final Map<MaterialPart, ResourceLocation> existingParts;
    private final BlockStrength defaultStrength;
    private final Map<MaterialPart, BlockStrength> partStrengths;

    public WoodMaterial(String id, String displayName, int color, WoodModel model, MachineTier tier) {
        this(id, displayName, color, model, tier, List.of(), Map.of(), null, Map.of());
    }

    private WoodMaterial(
            String id,
            String displayName,
            int color,
            WoodModel model,
            MachineTier tier,
            List<MaterialComponent> components,
            Map<MaterialPart, ResourceLocation> existingParts,
            BlockStrength defaultStrength,
            Map<MaterialPart, BlockStrength> partStrengths
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Wood material id cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Wood material display name cannot be blank: " + id);
        }
        if (model == null) {
            throw new IllegalArgumentException("Wood model cannot be null: " + id);
        }
        if (tier == null || tier == MachineTier.NONE) {
            throw new IllegalArgumentException("Wood material tier must be a real tier: " + id);
        }
        this.id = id;
        this.displayName = displayName;
        this.color = color & 0x00FFFFFF;
        this.model = model;
        this.tier = tier;
        this.components = List.copyOf(components);
        this.existingParts = Map.copyOf(existingParts);
        this.defaultStrength = defaultStrength;
        this.partStrengths = Map.copyOf(partStrengths);
    }

    public WoodMaterial contains(MaterialComponent... additions) {
        List<MaterialComponent> updated = new ArrayList<>(components);
        if (additions != null) {
            for (MaterialComponent component : additions) {
                if (component == null) {
                    throw new IllegalArgumentException("Wood material component cannot be null: " + id);
                }
                updated.add(component);
            }
        }
        return new WoodMaterial(id, displayName, color, model, tier, updated, existingParts, defaultStrength, partStrengths);
    }

    public WoodMaterial existing(MaterialPart part, String resourceLocation) {
        ResourceLocation parsed = ResourceLocation.tryParse(resourceLocation);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid existing resource location '" + resourceLocation + "' for " + id);
        }
        return existing(part, parsed);
    }

    public WoodMaterial existing(MaterialPart part, ResourceLocation resourceLocation) {
        if (part == null || resourceLocation == null) {
            throw new IllegalArgumentException("Existing wood part and resource cannot be null: " + id);
        }
        Map<MaterialPart, ResourceLocation> updated = new EnumMap<>(MaterialPart.class);
        updated.putAll(existingParts);
        ResourceLocation previous = updated.put(part, resourceLocation);
        if (previous != null && !previous.equals(resourceLocation)) {
            throw new IllegalArgumentException(
                    "Wood material " + id + " already maps " + part + " to " + previous
            );
        }
        return new WoodMaterial(id, displayName, color, model, tier, components, updated, defaultStrength, partStrengths);
    }

    public WoodMaterial strength(float hardness) {
        return strength(hardness, hardness);
    }

    public WoodMaterial strength(float hardness, float resistance) {
        return new WoodMaterial(
                id, displayName, color, model, tier, components, existingParts,
                BlockStrength.of(hardness, resistance), partStrengths
        );
    }

    public WoodMaterial strength(MaterialPart part, float hardness) {
        return strength(part, hardness, hardness);
    }

    public WoodMaterial strength(MaterialPart part, float hardness, float resistance) {
        if (part == null || !part.isBlock()) {
            throw new IllegalArgumentException("Wood strength override requires a block part: " + id + " " + part);
        }
        Map<MaterialPart, BlockStrength> updated = new EnumMap<>(MaterialPart.class);
        updated.putAll(partStrengths);
        updated.put(part, BlockStrength.of(hardness, resistance));
        return new WoodMaterial(
                id, displayName, color, model, tier, components, existingParts,
                defaultStrength, updated
        );
    }

    public Optional<BlockStrength> strengthFor(MaterialPart part) {
        BlockStrength specific = part == null ? null : partStrengths.get(part);
        return Optional.ofNullable(specific != null ? specific : defaultStrength);
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
    public WoodModel model() {
        return model;
    }

    @Override
    public List<MaterialComponent> components() {
        return components;
    }

    @Override
    public Map<MaterialPart, ResourceLocation> existingParts() {
        return existingParts;
    }

    @Override
    public Set<MaterialPart> generatedForms() {
        return DEFAULT_FORMS;
    }
}
