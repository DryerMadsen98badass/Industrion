package net.mads.industron.material;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.atomic.AtomicModel;
import net.mads.industron.material.atomic.AtomicState;
import net.mads.industron.material.atomic.IonState;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Identity definition for a fictional element.
 *
 * Elements are also IndustrialSubstances so composition definitions can refer to
 * the element constant directly, e.g. component(IndustrialMaterials.VERNIUM, 2).
 */
public record ElementDefinition(
        String id,
        String displayName,
        String symbol,
        int atomicNumber,
        MachineTier tier,
        Map<MaterialPart, ResourceLocation> existingParts,
        Optional<GemBlockStyle> gemBlockStyle,
        Optional<Integer> colorOverride
) implements IndustrialSubstance {
    public ElementDefinition(
            String id,
            String displayName,
            String symbol,
            int atomicNumber,
            MachineTier tier
    ) {
        this(id, displayName, symbol, atomicNumber, tier, Map.of(), Optional.empty(), Optional.empty());
    }

    public ElementDefinition(
            String id,
            String displayName,
            String symbol,
            int atomicNumber,
            MachineTier tier,
            Map<MaterialPart, ResourceLocation> existingParts
    ) {
        this(id, displayName, symbol, atomicNumber, tier, existingParts, Optional.empty(), Optional.empty());
    }

    public ElementDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Element id cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Element display name cannot be blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Element symbol cannot be blank");
        }
        if (atomicNumber <= 0) {
            throw new IllegalArgumentException("Element atomic number must be positive: " + id);
        }
        if (tier == null || !MachineTier.ALL.contains(tier)) {
            throw new IllegalArgumentException(
                    "Element tier must be present in MachineTier.ALL: " + id
            );
        }
        existingParts = existingParts == null ? Map.of() : Map.copyOf(existingParts);
        gemBlockStyle = gemBlockStyle == null ? Optional.empty() : gemBlockStyle;
        colorOverride = colorOverride == null ? Optional.empty() : colorOverride;
        colorOverride.ifPresent(color -> {
            if (color < 0 || color > 0x00FFFFFF) {
                throw new IllegalArgumentException(
                        "Element color override must be a 24-bit RGB value: " + id
                );
            }
        });
    }

    /**
     * Reuses an already registered Minecraft/mod form for this element.
     * Existing forms are part of the element just like generated forms; they are
     * not duplicated in Industron's registries.
     */
    public ElementDefinition existing(MaterialPart part, String resourceLocation) {
        Objects.requireNonNull(resourceLocation, "resourceLocation");
        return existing(part, ResourceLocation.parse(resourceLocation));
    }

    /**
     * Reuses an already registered Minecraft/mod form for this element.
     */
    public ElementDefinition existing(MaterialPart part, ResourceLocation resourceLocation) {
        Objects.requireNonNull(part, "part");
        Objects.requireNonNull(resourceLocation, "resourceLocation");

        EnumMap<MaterialPart, ResourceLocation> updated = new EnumMap<>(MaterialPart.class);
        updated.putAll(existingParts);
        ResourceLocation previous = updated.put(part, resourceLocation);
        if (previous != null && !previous.equals(resourceLocation)) {
            throw new IllegalArgumentException(
                    "Element " + id + " already maps " + part + " to " + previous
            );
        }

        return new ElementDefinition(
                id, displayName, symbol, atomicNumber, tier, updated, gemBlockStyle, colorOverride
        );
    }

    /**
     * Overrides the deterministic gem base-block style for this element.
     * This has no effect unless the calculated material is a gem candidate.
     */
    public ElementDefinition gemBlockStyle(GemBlockStyle style) {
        return new ElementDefinition(
                id, displayName, symbol, atomicNumber, tier, existingParts,
                Optional.of(Objects.requireNonNull(style, "style")), colorOverride
        );
    }

    /**
     * Overrides only the generated visual RGB color. Atomic/material properties still
     * come from atomic number + tier; every visual consumer reads the overridden
     * baseColor from MaterialProperties.
     */
    public ElementDefinition color(int rgb) {
        if (rgb < 0 || rgb > 0x00FFFFFF) {
            throw new IllegalArgumentException(
                    "Element color override must be a 24-bit RGB value: " + id
            );
        }
        return new ElementDefinition(
                id, displayName, symbol, atomicNumber, tier, existingParts, gemBlockStyle,
                Optional.of(rgb)
        );
    }


    /** Neutral atom data. Tier and material forms do not affect this identity model. */
    public AtomicState atomicState() {
        return AtomicModel.neutral(atomicNumber);
    }

    /** Builds one charged chemical state. No material item/block is registered for it. */
    public IonState ionState(int charge) {
        return AtomicModel.ion(atomicNumber, charge);
    }

    /** Chemically plausible non-zero ion states, best candidate first. */
    public List<IonState> allowedIonStates() {
        return AtomicModel.allowedIonStates(atomicNumber);
    }

    /** Preferred charged state when the neutral atom has one; empty for neutral-preferring atoms. */
    public Optional<IonState> preferredIonState() {
        return AtomicModel.preferredIonState(atomicNumber);
    }

    @Override
    public int color() {
        return properties().baseColor();
    }

    @Override
    public String formula() {
        return symbol;
    }

    @Override
    public String formula(boolean nested) {
        return symbol;
    }

    @Override
    public int componentTemperature() {
        return properties().meltingPoint();
    }

    private MaterialProperties properties() {
        return MaterialPropertyCalculator.calculate(this);
    }
}
